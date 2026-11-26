package com.harvester.socialmediaharvester.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;

/**
 * The environment profiles, one of which is chosen with {@code SPRING_PROFILES_ACTIVE}.
 * {@link #TEST} is for tests only, against the disposable {@code harvester_test}.
 */
enum EnvironmentProfile {

	LOCAL("local"), DEV("dev"), PROD("prod"), TEST("test");

	private final String profileName;

	EnvironmentProfile(String profileName) {
		this.profileName = profileName;
	}

	String profileName() {
		return profileName;
	}

	static boolean isEnvironmentProfile(String profileName) {
		return Arrays.stream(values()).anyMatch(profile -> profile.profileName.equals(profileName));
	}

	/** The profiles to choose from, for startup messages: "local, dev or prod (tests use the test profile)". */
	static String choices() {
		String[] names = Arrays.stream(values()).filter(profile -> profile != TEST)
				.map(EnvironmentProfile::profileName).toArray(String[]::new);
		String allButLast = Arrays.stream(names, 0, names.length - 1).collect(Collectors.joining(", "));
		return allButLast + " or " + names[names.length - 1] + " (tests use the " + TEST.profileName + " profile)";
	}

	/** Whether this is the one and only active profile, not merely one of several. */
	boolean isTheOnlyActiveProfile(Environment environment) {
		return Arrays.equals(environment.getActiveProfiles(), new String[] { profileName });
	}
}
