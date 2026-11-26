package com.harvester.socialmediaharvester.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.harvester.socialmediaharvester.SocialMediaHarvesterApplication;

/**
 * Flyway {@code clean} is refused outside the {@code test} profile even if someone
 * turns {@code spring.flyway.clean-disabled} off. Runs a dev-profile app pointed
 * at harvester_test so a broken guard can only ever wipe the disposable database.
 */
class FlywayCleanGuardTest {

	@Test
	void cleanIsRefusedInTheDevProfileEvenWhenConfiguredOn() {
		try (ConfigurableApplicationContext app = new SpringApplicationBuilder(SocialMediaHarvesterApplication.class)
				.profiles("dev")
				// Command-line arguments outrank the dev profile's own settings.
				.run("--harvester.db.name=harvester_test",
						"--spring.flyway.clean-disabled=false",
						"--server.port=0",
						"--harvester.first-admin.email=first.admin@harvester.test",
						"--harvester.first-admin.password=first-admin-test-password")) {
			Flyway flyway = app.getBean(Flyway.class);

			assertThatThrownBy(flyway::clean)
					.isInstanceOf(FlywayException.class)
					.hasMessageContaining("clean");
		}
	}
}
