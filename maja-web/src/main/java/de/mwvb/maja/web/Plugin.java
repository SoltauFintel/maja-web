package de.mwvb.maja.web;

import com.google.inject.Module;

public interface Plugin {

	/**
	 * Phase 1
	 * @return Guice Module that defines the classes that can be injected
	 */
	Module getModule();
	
	/**
	 * Phase 2
	 */
	void prepare();
	
	/**
	 * Phase 3
	 */
	void install();
	
	/**
	 * Phase 4: Setup routes.
	 */
	void routes();

	/**
	 * This method may print a info during startup to the console.
	 */
	void printInfo();
}
