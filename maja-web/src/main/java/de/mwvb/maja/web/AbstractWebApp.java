package de.mwvb.maja.web;

import static spark.Spark.exception;
import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.staticFileLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.ConsoleWriter;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;

import spark.Request;
import spark.Response;

/**
 * Maja-web comes with Spark, Actions, templating (Velocity), configuration, banner, favicon,
 * Logging (tinylog) and Bootstrap.
 * 
 * <p>Static web resources must be in src/main/resources/web.</p>
 * <p>Place banner.txt and favicon.ico (16x16 PNG) in src/main/resources.</p>
 */
public abstract class AbstractWebApp {
	private static final LocalDateTime boottime = LocalDateTime.now();
	protected Level level;
	protected List<Plugin> plugins;
	private Injector injector;
	@Inject
	private AppConfig config;
	
	public void start(String version, Plugin ... plugins) {
		start(version, Arrays.asList(plugins));
	}
	
	public void start(String version, List<Plugin> plugins) {
		initLogging();
		
		this.plugins = plugins;
		init();
		
		int port = Integer.parseInt(config.get("port"));
		port(port);
		banner(port, version);
		
    	staticFileLocation("web");
    	if (config.isDevelopment()) {
    		externalStaticFileLocation("src/main/resources/web");
    	}
    	
    	defaultRoutes();
    	this.plugins.forEach(plugin -> plugin.routes());
    	routes();
	}

	public void startForTest(Plugin ... plugins) {
		this.plugins = Arrays.asList(plugins);
		init();
	}
	
	private void init() {
		injector = Guice.createInjector(getAllModules());
		injector.injectMembers(this);
		plugins.forEach(plugin -> injector.injectMembers(plugin));
		plugins.forEach(plugin -> plugin.init());
	}

	private List<Module> getAllModules() {
		List<Module> modules = new ArrayList<>();
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AppConfig.class);
			}
		});
		return modules;
	}
	
	protected void defaultRoutes() {
		setupExceptionHandler();
	
		get("/rest/_ping", (req, res) -> "pong");
		addNotProtected("/rest/_");
		
		get("/favicon.ico", (req, res) -> getFavicon(req, res));
		addNotProtected("/favicon.ico");
	}
	
	private void addNotProtected(String path) {
		plugins.stream()
			.filter(plugin -> plugin instanceof AuthPlugin)
			.forEach(plugin -> ((AuthPlugin) plugin).addNotProtected(path));
	}

	private void setupExceptionHandler() {
		exception(RuntimeException.class, (exception, req, res) -> {
			ActionBase action = getErrorPage();
			action.init(req, res);
			if (action instanceof ErrorPage) {
				((ErrorPage) action).setException(exception);
			}
			if (action instanceof Action) {
				initAction(req, (Action) action);
			}
			String html = action.run();
			res.body(html);
		});
	}
	
	protected ActionBase getErrorPage() {
		return new GuruErrorPage();
	}

	protected String getFavicon(Request req, Response res) {
		try (InputStream is = getClass().getResourceAsStream("/favicon.ico")) {
			try (OutputStream os = res.raw().getOutputStream()) {
				byte[] buf = new byte[1200];
				for (int nChunk = is.read(buf); nChunk != -1; nChunk = is.read(buf)) {
					os.write(buf, 0, nChunk);
				}
			}
		} catch (IOException e) {
			Logger.error(e);
		}
		return "";
	}

	protected abstract void routes();
	
	protected final void _get(String path, Class<? extends ActionBase> actionClass) {
		get(path, (req, res) -> {
			ActionBase action = actionClass.newInstance();
			action.init(req, res);
			if (action instanceof Action) {
				initAction(req, (Action) action);
			}
			return action.run();
		});
	}

	protected final void _get(String path, ActionBase action) {
		get(path, (req, res) -> {
			action.init(req, res);
			if (action instanceof Action) {
				initAction(req, (Action) action);
			}
			return action.run();
		});
	}

	protected void initAction(Request req, Action action) {
	}

	protected void banner(int port, String version) {
		banner();
		System.out.println("v" + version + " ready on port " + port);
		System.out.println("Configuration file: " + config.getFilename()
				+ " | Log level: " + Logger.getLevel()
				+ " | Mode: " + (config.isDevelopment() ? "development" : "production"));
		
		plugins.forEach(plugin -> plugin.printInfo());
		
		String info = getTimeInfo();
		if (info != null) {
			System.out.println(info);
		}
	}
	
	protected void banner() {
		try (InputStream is = getClass().getResourceAsStream("/banner.txt")) {
			try (java.util.Scanner scanner = new java.util.Scanner(is)) {
				java.util.Scanner text = scanner.useDelimiter("\\A");
				if (text.hasNext()) {
					System.out.println(text.next());
				}
			}
		} catch (IOException ignore) {}
	}

	protected String getTimeInfo() {
		return "Date/time: " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").format(LocalDateTime.now())
				+ ", timezone: " + ZoneId.systemDefault();
	}

	public static LocalDateTime getBoottime() {
		return boottime;
	}

	protected void initLogging() {
		level = Level.WARNING;
		String loglevel = System.getenv("LOGLEVEL");
		if ("DEBUG".equalsIgnoreCase(loglevel)) {
			level = Level.DEBUG;
		} else if ("INFO".equalsIgnoreCase(loglevel)) {
			level = Level.INFO;
		} else if ("ERROR".equalsIgnoreCase(loglevel)) {
			level = Level.ERROR;
		} else if ("OFF".equalsIgnoreCase(loglevel)) {
			level = Level.OFF;
		} else if ("TRACE".equalsIgnoreCase(loglevel)) {
			level = Level.TRACE;
		}
		Configurator.currentConfig()
			.writer(new ConsoleWriter())
			.formatPattern("{date}  {message}")
			.level(level)
			.activate();
	}
}
