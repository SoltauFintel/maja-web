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

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.ConsoleWriter;

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
	protected boolean development;
	protected Level level;
	protected AppConfig config;
	protected AuthPlugin auth;
	
	public void start(String version) {
		initLogging();
		
		initConfig();
		initDatabase();
		
		int port = Integer.parseInt(config.get("port"));
		port(port);
		
    	staticFileLocation("web");
    	if (development) {
    		externalStaticFileLocation("src/main/resources/web");
    	}
    	
    	init();
		if ("false".equals(config.get("auth"))) {
			if (!development) {
				System.err.println("[WARNING] Authentication is deactivated! Web application is not secure.");
			}
			auth.deactivate();
		}
    	
    	defaultRoutes();
    	routes();
    	
		banner(port, version);
	}

	protected void initConfig() {
		String configFile = System.getenv("CONFIG");
		if (configFile != null && !configFile.isEmpty()) {
			AppConfig.filename = configFile;
		}
		config = new AppConfig();
		development = "true".equals(config.get("development"));
	}

	/**
	 * Open database (if database is needed)
	 */
	protected void initDatabase() {
	}
	
	/**
	 * @return database name and user for internal output
	 */
	protected String getDatabaseInfo() {
		return null;
	}

	/**
	 * init auth and app
	 */
	protected void init() {
		auth = new NoOpAuthPlugin();
	}
	
	protected void defaultRoutes() {
		setupExceptionHandler();
	
		get("/rest/_ping", (req, res) -> "pong");
		auth.addNotProtected("/rest/_");
	
		get("/favicon.ico", (req, res) -> getFavicon(req, res));
		auth.addNotProtected("/favicon.ico");
	
		auth.routes();
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
		System.out.println("Configuration file: " + AppConfig.filename
				+ " | Log level: " + Logger.getLevel()
				+ " | Mode: " + (development ? "development" : "production"));
		
		String info = getDatabaseInfo();
		if (info != null) {
			System.out.println(info);
		}
		
		info = getTimeInfo();
		if (info != null) {
			System.out.println(info);
		}
	}
	
	protected String getTimeInfo() {
		return "Date/time: " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").format(LocalDateTime.now())
				+ ", timezone: " + ZoneId.systemDefault();
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
