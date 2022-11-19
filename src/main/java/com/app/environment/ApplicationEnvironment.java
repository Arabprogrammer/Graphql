package com.app.environment;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.app.utils.Fallback;
import com.app.utils.Fallback.Result;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;

public class ApplicationEnvironment {

	private static final Logger logger = getLogger(ApplicationEnvironment.class);

	private final String name;
	private final Properties props;

	public ApplicationEnvironment(String name, Properties props) {
		this.name = name;
		this.props = props;
	}

	private String get(String key) {
		if (props.getProperty(key) != null) {
			return props.getProperty(key);
		}
		Any pro = JsonIterator.deserialize(props.toString());
		throw new NoSuchElementException(
				String.format("Couldn't find key %s for the %s environment. All properties: %s", key, name, pro));
	}

	public boolean getBoolean(String key) {
		return get(key, Boolean::parseBoolean);
	}

	public String getString(String key) {
		return get(key, x -> x);
	}

	public <T> T get(String key, Function<String, T> parser) {
		return parser.apply(get(key));
	}

	public static <T> ApplicationEnvironment parse(String[] args, Class<T> applicationMainClass) {
		if (args.length == 0) {
			logger.debug("Expected a present environment as first parameter but was empty");
			throw new IllegalArgumentException("Expected a present environment as first parameter but was empty");
		}

		String potentialEnvironment = args[0];
		String fileName = "/" + potentialEnvironment + "-application.properties";

		Fallback<Path> getPath = new Fallback<Path>().from(() -> {
			URL url = applicationMainClass.getResource(fileName);

			if (url == null) {
				return Fallback.notFound("No properties file " + fileName + " found on the classpath");
			}

			try {
				Path path = Paths.get(url.toURI());
				return Fallback.found(path);
			} catch (URISyntaxException e) {
				return Fallback.notFound(e.getMessage());
			}
		}).from(() -> {
			String userHome = System.getProperty("user.home");
			Path path = Paths.get(userHome, fileName);

			if (Files.exists(path)) {
				return Fallback.found(path);
			}

			return Fallback.notFound("No properties file " + path + " found");
		});

		Result<Path> result = getPath.execute();
		if (result.isEmpty()) {
			throw new IllegalStateException(result.getErrors().stream().collect(Collectors.joining(",")));
		}

		Path path = getPath.getValue();
		try (InputStream in = Files.newInputStream(path)) {
			Properties props = new Properties();
			props.load(in);
			return new ApplicationEnvironment(potentialEnvironment, props);
		} catch (IOException e) {
			e.printStackTrace();
		}
		throw new IllegalArgumentException("Expected a present environment as first parameter but was empty");
	}

	public String getEnvironmentName() {
		return name;
	}

}
