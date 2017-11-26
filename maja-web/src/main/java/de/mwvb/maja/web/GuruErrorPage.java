package de.mwvb.maja.web;

import org.pmw.tinylog.Logger;

public class GuruErrorPage extends Action implements ErrorPage {
	private Exception exception;
	
	@Override
	public void setException(Exception exception) {
		this.exception = exception;
	}
	
	// TODO HTML in die Klasse einbetten, damit es jo immer dabei ist. (‹berschreibbar machen, wenn null 
	// wird standardm‰ﬂig doch die Datei gezogen)
	
	@Override
	protected void execute() {
		Logger.error(exception);
		res.status(500);
		put("msg", exception.getClass().getName() + ": " + exception.getMessage());
	}
}
