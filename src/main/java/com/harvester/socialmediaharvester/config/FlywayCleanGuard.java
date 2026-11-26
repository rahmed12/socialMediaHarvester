package com.harvester.socialmediaharvester.config;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Flyway {@code clean} wipes a database. Only the disposable {@code harvester_test}
 * may be wiped, so clean is forced off unless {@code test} is the only active
 * profile (not with {@code prod,test} either), whatever
 * {@code spring.flyway.clean-disabled} says.
 */
@Component
class FlywayCleanGuard implements FlywayConfigurationCustomizer {

	private final Environment environment;

	FlywayCleanGuard(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void customize(FluentConfiguration configuration) {
		if (!EnvironmentProfile.TEST.isTheOnlyActiveProfile(environment)) {
			configuration.cleanDisabled(true);
		}
	}
}
