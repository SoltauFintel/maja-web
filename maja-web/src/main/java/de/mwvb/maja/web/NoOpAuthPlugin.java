package de.mwvb.maja.web;

import spark.Request;
import spark.Response;

public final class NoOpAuthPlugin implements AuthPlugin {

	@Override
	public void deactivate() {
	}

	@Override
	public void addNotProtected(String path) {
	}

	@Override
	public void routes() {
	}

	@Override
	public String login(Request req, Response res, String name, String foreignId, String service, boolean rememberMe) {
		return "";
	}
}
