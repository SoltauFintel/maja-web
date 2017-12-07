package de.mwvb.maja.web;

public interface Plugin {

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
