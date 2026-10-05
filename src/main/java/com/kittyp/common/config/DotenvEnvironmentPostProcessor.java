/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads key/value pairs from a local .env file into the Spring environment.
 *
 * <p>
 * The file path comes from the ENV_FILE environment variable and falls back to
 * .env in the working directory. The property source is added last, so real
 * environment variables and command line arguments keep precedence.
 * </p>
 *
 * @author rrohan419@gmail.com
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String PROPERTY_SOURCE_NAME = "dotenvFile";

	private static final String ENV_FILE_VARIABLE = "ENV_FILE";

	private static final String DEFAULT_ENV_FILE = ".env";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Path envFile = resolveEnvFile();
		if (envFile == null || !Files.isReadable(envFile)) {
			return;
		}

		Map<String, Object> values = read(envFile);
		if (values.isEmpty()) {
			return;
		}

		environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, values));
	}

	private Path resolveEnvFile() {
		String configured = System.getenv(ENV_FILE_VARIABLE);
		if (configured != null && !configured.isBlank()) {
			return Paths.get(configured.trim());
		}
		return Paths.get(DEFAULT_ENV_FILE).toAbsolutePath();
	}

	private Map<String, Object> read(Path envFile) {
		Map<String, Object> values = new LinkedHashMap<>();
		List<String> lines;
		try {
			lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
		} catch (IOException exception) {
			return values;
		}

		for (String line : lines) {
			String entry = line.trim();
			if (entry.isEmpty() || entry.startsWith("#")) {
				continue;
			}
			if (entry.startsWith("export ")) {
				entry = entry.substring("export ".length()).trim();
			}

			int separator = entry.indexOf('=');
			if (separator <= 0) {
				continue;
			}

			String key = entry.substring(0, separator).trim();
			String value = unquote(entry.substring(separator + 1).trim());
			if (!key.isEmpty()) {
				values.put(key, value);
			}
		}
		return values;
	}

	private String unquote(String value) {
		if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
				|| value.startsWith("'") && value.endsWith("'"))) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}
}
