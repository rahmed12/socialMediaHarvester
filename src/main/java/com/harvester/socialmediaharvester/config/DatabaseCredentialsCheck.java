package com.harvester.socialmediaharvester.config;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

/**
 * Stops startup before any connection is attempted unless both database
 * connections log in with exactly the {@link #VARIABLES} from the OS
 * environment. System properties, command-line arguments or other variables
 * such as {@code SPRING_R2DBC_USERNAME} cannot stand in for them or override
 * them. Messages only ever name the variables, never values.
 */
public class DatabaseCredentialsCheck implements EnvironmentPostProcessor {

	private record Credential(String variable, List<String> properties) {
	}

	private static final List<Credential> CREDENTIALS = List.of(
			new Credential("DB_USER_HARVEST", List.of("spring.r2dbc.username", "spring.flyway.user")),
			new Credential("DB_PASSWORD_HARVEST", List.of("spring.r2dbc.password", "spring.flyway.password")));

	static final List<String> VARIABLES = CREDENTIALS.stream().map(Credential::variable).toList();

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		PropertySource<?> osEnvironment = environment.getPropertySources()
				.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		Binder binder = Binder.get(environment);
		for (Credential credential : CREDENTIALS) {
			Object expected = osEnvironment == null ? null : osEnvironment.getProperty(credential.variable());
			if (!(expected instanceof String value) || !StringUtils.hasText(value)) {
				throw new IllegalStateException("Environment variable " + credential.variable() + " is not set. "
						+ "The database login is read only from the " + variableList()
						+ " environment variables; set them before starting the app.");
			}
			for (String property : credential.properties()) {
				String resolved = binder.bind(property, String.class).orElse(null);
				if (!Objects.equals(resolved, value)) {
					throw new IllegalStateException("Both database connections must log in with " + variableList()
							+ " only, but " + property + " is set from somewhere else (a system property, "
							+ "command-line argument or another environment variable). Remove that override.");
				}
			}
		}
	}

	private static String variableList() {
		return VARIABLES.stream().collect(Collectors.joining(" and "));
	}
}
