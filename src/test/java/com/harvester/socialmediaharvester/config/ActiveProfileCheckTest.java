package com.harvester.socialmediaharvester.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.harvester.socialmediaharvester.SocialMediaHarvesterApplication;

/** Starting without exactly one known environment profile that chooses a database stops before any connection. */
class ActiveProfileCheckTest {

	@Test
	void startupWithoutAProfileFailsClearly() {
		assertThatThrownBy(() -> start())
				.hasStackTraceContaining("No Spring profile is active. Set SPRING_PROFILES_ACTIVE");
	}

	@Test
	void startupWithAnUnknownProfileFailsClearly() {
		assertThatThrownBy(() -> start("staging"))
				.hasStackTraceContaining("Unknown Spring profile [staging]. Set SPRING_PROFILES_ACTIVE");
	}

	@Test
	void startupWithAnUnknownProfileBesideAnEnvironmentProfileFailsClearly() {
		assertThatThrownBy(() -> start("local", "foo"))
				.hasStackTraceContaining("Unknown Spring profile [foo]. Set SPRING_PROFILES_ACTIVE");
	}

	@Test
	void startupWithTwoEnvironmentProfilesFailsClearly() {
		assertThatThrownBy(() -> start("dev", "prod"))
				.hasStackTraceContaining("Only one environment profile may be active, but [dev, prod] are.");
	}

	private static void start(String... profiles) {
		Map<String, Object> variables = new HashMap<>(System.getenv());
		variables.remove("SPRING_PROFILES_ACTIVE");
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, variables));
		environment.getPropertySources().replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
				new MapPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, new HashMap<>()));
		SpringApplication app = new SpringApplication(SocialMediaHarvesterApplication.class);
		app.setEnvironment(environment);
		app.setAdditionalProfiles(profiles);
		app.run().close();
	}
}
