package de.mwvb.maja.web;

import spark.Request;
import spark.Response;

/**
 * AbstractWebApp auth plugin
 */
public interface AuthPlugin {
	
	/**
	 * Deactivate auth feature. That means that everybody can access every resource. The deactivate-feature is
	 * only for development and installation - not for production. This method() must be called before routes() is called.
	 */
	void deactivate();
	
	/**
	 * Adds a path that is not protected. That means everybody can access resources on that path.
	 * 
	 * @param path or begin of path e.g. "/rest/_"
	 */
	void addNotProtected(String path);

	/**
	 * Setup routes.
	 */
	void routes();
	
	/**
	 * Called by the callback action to login the user to the Maja system.
	 * 
	 * @param req Request
	 * @param res Response
	 * @param name user name from the foreign auth service
	 * @param foreignId user id from the foreign auth service
	 * @param service id of the auth service
	 * @param rememberMe true if the remember service shall store the login, false if the remember service shall delete the login
	 * @return usually "" because a redirect to another page will be executed
	 */
	String login(Request req, Response res, String name, String foreignId, String service, boolean rememberMe);
}
