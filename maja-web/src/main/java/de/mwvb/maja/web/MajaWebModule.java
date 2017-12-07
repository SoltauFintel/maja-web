package de.mwvb.maja.web;

import com.google.inject.AbstractModule;

public class MajaWebModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(AppConfig.class);
	}
}
