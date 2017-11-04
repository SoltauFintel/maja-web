package de.mwvb.maja.web;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
	public static String filename = "AppConfig.properties";
	private final Properties properties = new Properties();
	
	public AppConfig() {
		try {
			properties.load(new FileReader(filename));
		} catch (IOException e1) {
			if (filename.startsWith("/")) {
				throw new RuntimeException(e1);
			}
			try {
				properties.load(new FileReader("/" + filename));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * @param key
	 * @return null if property does not exist
	 */
	public String get(String key) {
		String ret = properties.getProperty(key);
		return ret == null ? null : ret.trim();
	}
	
	/**
	 * @param key
	 * @param pDefault
	 * @return pDefault if property does not exist
	 */
	public String get(String key, String pDefault) {
		String ret = properties.getProperty(key);
		return ret == null ? pDefault : ret.trim();
	}
}
