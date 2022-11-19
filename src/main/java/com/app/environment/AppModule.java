package com.app.environment;

import com.google.inject.AbstractModule;

public class AppModule extends AbstractModule {

	private final ApplicationEnvironment env;

	public AppModule(ApplicationEnvironment env) {
		this.env = env;
	}

	@Override
	protected void configure() {
		bind(ApplicationEnvironment.class).toProvider(() -> env);

	}

}
