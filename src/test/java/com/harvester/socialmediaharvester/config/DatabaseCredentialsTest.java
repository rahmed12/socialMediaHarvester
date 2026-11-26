package com.harvester.socialmediaharvester.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.harvester.socialmediaharvester.SocialMediaHarvesterApplication;

/**
 * Startup refuses to go on unless both database connections log in with
 * exactly the DB_USER_HARVEST / DB_PASSWORD_HARVEST OS environment variables.
 * Every failing start below stops before any connection is attempted.
 */
class DatabaseCredentialsTest {

	static final String NOT_THE_LOGIN = "not-the-harvest-login";

	@ParameterizedTest
	@ValueSource(strings = { "DB_USER_HARVEST", "DB_PASSWORD_HARVEST" })
	void startupFailsClearlyWhenAVariableIsMissing(String missing) {
		Map<String, Object> variables = osEnvironment();
		variables.remove(missing);

		assertThatThrownBy(() -> start(variables, Map.of()))
				.hasStackTraceContaining("Environment variable " + missing + " is not set");
	}

	@ParameterizedTest
	@ValueSource(strings = { "DB_USER_HARVEST", "DB_PASSWORD_HARVEST" })
	void startupFailsClearlyWhenAVariableIsBlank(String blank) {
		Map<String, Object> variables = osEnvironment();
		variables.put(blank, " ");

		assertThatThrownBy(() -> start(variables, Map.of()))
				.hasStackTraceContaining("Environment variable " + blank + " is not set");
	}

	@ParameterizedTest
	@ValueSource(strings = { "DB_USER_HARVEST", "DB_PASSWORD_HARVEST" })
	void aSystemPropertyCannotStandInForTheVariable(String variable) {
		Map<String, Object> variables = osEnvironment();
		variables.remove(variable);

		assertThatThrownBy(() -> start(variables, Map.of(variable, NOT_THE_LOGIN)))
				.hasStackTraceContaining("Environment variable " + variable + " is not set");
	}

	@ParameterizedTest
	@ValueSource(strings = { "DB_USER_HARVEST", "DB_PASSWORD_HARVEST" })
	void aSystemPropertyCannotOverrideTheVariable(String variable) {
		assertThatThrownBy(() -> start(osEnvironment(), Map.of(variable, NOT_THE_LOGIN)))
				.hasStackTraceContaining(loginNotFromVariablesMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "SPRING_R2DBC_USERNAME", "SPRING_R2DBC_PASSWORD", "SPRING_FLYWAY_USER",
			"SPRING_FLYWAY_PASSWORD" })
	void anotherEnvironmentVariableCannotOverrideTheLogin(String override) {
		Map<String, Object> variables = osEnvironment();
		variables.put(override, NOT_THE_LOGIN);

		assertThatThrownBy(() -> start(variables, Map.of()))
				.hasStackTraceContaining(loginNotFromVariablesMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = { "spring.r2dbc.username", "spring.r2dbc.password", "spring.flyway.user",
			"spring.flyway.password" })
	void aCommandLineArgumentCannotOverrideTheLogin(String property) {
		assertThatThrownBy(() -> start(osEnvironment(), Map.of(), "--" + property + "=" + NOT_THE_LOGIN))
				.hasStackTraceContaining(loginNotFromVariablesMessage());
	}

	@Test
	void theMessageNamesTheVariablesButNoValues() {
		Map<String, Object> variables = osEnvironment();
		variables.put("SPRING_R2DBC_PASSWORD", NOT_THE_LOGIN);

		assertThatThrownBy(() -> start(variables, Map.of()))
				.hasStackTraceContaining("DB_USER_HARVEST and DB_PASSWORD_HARVEST")
				.satisfies(failure -> org.assertj.core.api.Assertions
						.assertThat(stackTraceOf(failure).contains(NOT_THE_LOGIN))
						.as("the failure mentions a credential value")
						.isFalse());
	}

	private static String loginNotFromVariablesMessage() {
		return "must log in with DB_USER_HARVEST and DB_PASSWORD_HARVEST";
	}

	private static String stackTraceOf(Throwable failure) {
		var out = new java.io.StringWriter();
		failure.printStackTrace(new java.io.PrintWriter(out));
		return out.toString();
	}

	private static Map<String, Object> osEnvironment() {
		return new HashMap<>(System.getenv());
	}

	private static void start(Map<String, Object> osEnvironment, Map<String, Object> systemProperties,
			String... args) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, osEnvironment));
		environment.getPropertySources().replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
				new MapPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
						new HashMap<>(systemProperties)));
		SpringApplication app = new SpringApplication(SocialMediaHarvesterApplication.class);
		app.setEnvironment(environment);
		app.setAdditionalProfiles("test");
		app.run(args).close();
	}
}
