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

import de.mwvb.maja.timer.BaseTimer;
import de.mwvb.maja.timer.JuicyJobFactory;
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
	@Inject
	private Routes routes;
	@Inject
	private ActionInitializer actionInitializer;
	
	public void start(String version, Plugin ... plugins) {
		start(version, Arrays.asList(plugins));
	}
	
	public void start(String version, List<Plugin> plugins) {
		initLogging();
		
		this.plugins = plugins;
		injector = Guice.createInjector(getAllModules());
		injector.injectMembers(this);
		plugins.forEach(plugin -> injector.injectMembers(plugin));
		plugins.forEach(plugin -> plugin.prepare());
		plugins.forEach(plugin -> plugin.install());
		
		int port = Integer.parseInt(config.get("port"));
		port(port);
		banner(port, version);
		
    	staticFileLocation("web");
    	if (config.isDevelopment()) {
    		externalStaticFileLocation("src/main/resources/web");
    	}
    	
    	init();
    	
    	defaultRoutes();
    	this.plugins.forEach(plugin -> plugin.routes());
    	routes();
	}

	private List<Module> getAllModules() {
		List<Module> modules = new ArrayList<>();
		
		addModules(modules);
		
		plugins.forEach(plugin -> {
			Module module = plugin.getModule();
			if (module != null) {
				modules.add(module);
			}
		});
		return modules;
	}
	
	/**
	 * Extend this method to add a own module for depedency injection.
	 * @param modules
	 */
	protected void addModules(List<Module> modules) {
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AppConfig.class);
				bind(Broadcaster.class);
				bind(JuicyJobFactory.class);
				bind(ActionInitializer.class);
				bind(Routes.class);
			}
		});
	}
	
	protected void defaultRoutes() {
		setupExceptionHandler();
		get("/rest/_ping", (req, res) -> "pong");
		get("/favicon.ico", (req, res) -> getFavicon(req, res));
	}
	
	private void setupExceptionHandler() {
		exception(RuntimeException.class, (exception, req, res) -> {
			ActionBase action = getErrorPage();
			action.init(req, res);
			if (action instanceof ErrorPage) {
				((ErrorPage) action).setException(exception);
			}
			if (action instanceof Action) {
				actionInitializer.initAction(req, (Action) action);
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

	protected void init() {
	}
	
	protected final void startTimer(Class<? extends BaseTimer> timerClass) {
		try {
			BaseTimer timer = timerClass.newInstance();
			timer.start(injector);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected abstract void routes();
	
	protected final void _get(String path, Class<? extends ActionBase> actionClass) {
		routes._get(path, actionClass);
	}
	
	protected final void _post(String path, Class<? extends ActionBase> actionClass) {
		routes._post(path, actionClass);
	}

	protected final void _put(String path, Class<? extends ActionBase> actionClass) {
		routes._put(path, actionClass);
	}

	protected final void _delete(String path, Class<? extends ActionBase> actionClass) {
		routes._delete(path, actionClass);
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
