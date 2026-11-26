package com.harvester.socialmediaharvester.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Stops startup unless exactly one profile is active, it is a known environment
 * profile, and it chooses a database. Spring's implicit {@code default} profile is never used: the profile
 * picks the database, so it is always chosen from outside with
 * {@code SPRING_PROFILES_ACTIVE}. Runs after the config files are
 * loaded and before {@link DatabaseCredentialsCheck}.
 */
public class ActiveProfileCheck implements EnvironmentPostProcessor, Ordered {

	private static final String PROFILE_CHOICES = EnvironmentProfile.choices();

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String[] active = environment.getActiveProfiles();
		if (active.length == 0) {
			throw new IllegalStateException("No Spring profile is active. Set SPRING_PROFILES_ACTIVE to "
					+ PROFILE_CHOICES + " before starting the app.");
		}
		List<String> unknown = Arrays.stream(active).filter(profile -> !EnvironmentProfile.isEnvironmentProfile(profile)).toList();
		if (!unknown.isEmpty()) {
			throw new IllegalStateException("Unknown Spring profile " + unknown + ". Set SPRING_PROFILES_ACTIVE to "
					+ PROFILE_CHOICES + ".");
		}
		if (active.length > 1) {
			throw new IllegalStateException("Only one environment profile may be active, but "
					+ Arrays.toString(active) + " are. Set SPRING_PROFILES_ACTIVE to one of " + PROFILE_CHOICES + ".");
		}
		if (!environment.containsProperty("harvester.db.name")) {
			throw new IllegalStateException("The active profile " + Arrays.toString(active)
					+ " chooses no database. Set SPRING_PROFILES_ACTIVE to " + PROFILE_CHOICES + ".");
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}
}
