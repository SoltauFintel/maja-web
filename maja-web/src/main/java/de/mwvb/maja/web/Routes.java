package de.mwvb.maja.web;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import spark.Route;

@Singleton
public class Routes {
	@Inject
	private Injector injector;
	@Inject
	private ActionInitializer actionInitializer;
	
	public void _get(String path, Class<? extends ActionBase> actionClass) {
		get(path, createRoute(actionClass));
	}

	public void _post(String path, Class<? extends ActionBase> actionClass) {
		post(path, createRoute(actionClass));
	}

	public void _put(String path, Class<? extends ActionBase> actionClass) {
		put(path, createRoute(actionClass));
	}

	public void _delete(String path, Class<? extends ActionBase> actionClass) {
		delete(path, createRoute(actionClass));
	}

	private Route createRoute(Class<? extends ActionBase> actionClass) {
		return (req, res) -> {
			ActionBase action = actionClass.newInstance();
			injector.injectMembers(action);
			action.init(req, res);
			if (action instanceof Action) {
				actionInitializer.initAction(req, (Action) action);
			}
			return action.run();
		};
	}
}
