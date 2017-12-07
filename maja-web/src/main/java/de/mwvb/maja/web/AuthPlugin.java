package de.mwvb.maja.web;

/**
 * Auth plugin
 */
public interface AuthPlugin extends Plugin {
	
	/**
	 * Adds a path that is not protected. That means everybody can access resources on that path.
	 * 
	 * @param path or begin of path e.g. "/rest/_"
	 */
	void addNotProtected(String path);
}
