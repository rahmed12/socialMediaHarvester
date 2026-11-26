package com.harvester.socialmediaharvester.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

/** Each profile points both database connections at its own database. */
class ProfileDatabaseTest {

	@ParameterizedTest
	@CsvSource({
			"local, harvester_dev",
			"dev,   harvester_dev",
			"prod,  harvester_prod",
			"test,  harvester_test" })
	void profileUsesItsDatabaseForR2dbcAndFlyway(String profile, String database) {
		StandardEnvironment environment = environmentFor(profile);

		assertThat(environment.getProperty("spring.r2dbc.url"))
				.isEqualTo("r2dbc:postgresql://host.docker.internal:35432/" + database);
		assertThat(environment.getProperty("spring.flyway.url"))
				.isEqualTo("jdbc:postgresql://host.docker.internal:35432/" + database);
	}

	@ParameterizedTest
	@ValueSource(strings = { "local", "dev", "prod", "test" })
	void bothConnectionsLogInWithTheEnvironmentVariables(String profile) {
		StandardEnvironment environment = environmentFor(profile);

		// Compared as booleans so a failure never prints a credential.
		assertThat(sameAsVariable(environment, "spring.r2dbc.username", "DB_USER_HARVEST")).isTrue();
		assertThat(sameAsVariable(environment, "spring.flyway.user", "DB_USER_HARVEST")).isTrue();
		assertThat(sameAsVariable(environment, "spring.r2dbc.password", "DB_PASSWORD_HARVEST")).isTrue();
		assertThat(sameAsVariable(environment, "spring.flyway.password", "DB_PASSWORD_HARVEST")).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "local", "dev", "prod" })
	void flywayCleanIsDisabledOutsideTest(String profile) {
		assertThat(environmentFor(profile).getProperty("spring.flyway.clean-disabled")).isEqualTo("true");
	}

	private static boolean sameAsVariable(StandardEnvironment environment, String property, String variable) {
		String value = environment.getProperty(property);
		return value != null && value.equals(System.getenv(variable));
	}

	private static StandardEnvironment environmentFor(String profile) {
		StandardEnvironment environment = new StandardEnvironment();
		ConfigDataEnvironmentPostProcessor.applyTo(environment, new DefaultResourceLoader(),
				new DefaultBootstrapContext(), profile);
		return environment;
	}
}
