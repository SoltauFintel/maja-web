package de.mwvb.maja.web;

import com.google.inject.Module;

public interface Plugin {

	Module getModule();
	
	void init();
	
	/**
	 * Setup routes.
	 */
	void routes();

	/**
	 * This method may print a info during startup to the console.
	 */
	void printInfo();
}
