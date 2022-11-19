package com.app;

import static org.apache.logging.log4j.LogManager.getLogger;

import org.apache.logging.log4j.Logger;

import com.app.environment.AppModule;
import com.app.environment.ApplicationEnvironment;
import com.app.qraphql.GraphQlController;
import com.google.inject.Guice;
import com.google.inject.Injector;

import spark.Spark;

public class App {
	
	private static final Logger logger = getLogger(App.class);

	public static void main(String[] args) {
		logger.debug("started");
		Spark.port(8080);
		Spark.get("/", (req, res) -> ":)");
		ApplicationEnvironment env = ApplicationEnvironment.parse(args, App.class);
		Injector injector = Guice.createInjector(new AppModule(env));
		GraphQlController graphQLController = injector.getInstance(GraphQlController.class);
		Spark.post("/graphql", graphQLController::postQuery);

		logger.debug("finished");
	}

}
