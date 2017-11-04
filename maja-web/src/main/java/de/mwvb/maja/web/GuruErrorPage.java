package de.mwvb.maja.web;

import org.pmw.tinylog.Logger;

public class GuruErrorPage extends Action implements ErrorPage {
	private Exception exception;
	
	@Override
	public void setException(Exception exception) {
		this.exception = exception;
	}
	
	@Override
	protected void execute() {
		Logger.error(exception);
		res.status(500);
		put("msg", exception.getClass().getName() + ": " + exception.getMessage());
	}
}
