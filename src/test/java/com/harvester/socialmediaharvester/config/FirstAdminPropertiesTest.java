package com.harvester.socialmediaharvester.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** The first-Admin settings are optional, but a supplied email must be an email. */
class FirstAdminPropertiesTest {

	private final ApplicationContextRunner context = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesOnly.class);

	@Test
	void absentSettingsAreAccepted() {
		context.run(started -> assertThat(started).hasNotFailed());
	}

	@Test
	void aWellFormedEmailIsAccepted() {
		context.withPropertyValues("harvester.first-admin.email=admin@example.com")
				.run(started -> assertThat(started).hasNotFailed());
	}

	@Test
	void aMalformedEmailFailsStartup() {
		context.withPropertyValues("harvester.first-admin.email=not-an-email")
				.run(started -> assertThat(started).getFailure()
						.hasStackTraceContaining("harvester.first-admin.email"));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(FirstAdminProperties.class)
	static class PropertiesOnly {
	}
}
