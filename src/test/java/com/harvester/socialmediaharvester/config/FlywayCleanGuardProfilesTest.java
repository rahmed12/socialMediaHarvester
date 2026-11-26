package com.harvester.socialmediaharvester.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.StringUtils;

/** Clean stays allowed only when {@code test} is the one and only active profile. */
class FlywayCleanGuardProfilesTest {

	@ParameterizedTest
	@ValueSource(strings = { "prod,test", "test,prod", "dev,test", "test,local", "dev", "prod", "local", "" })
	void cleanIsRefusedUnlessTestIsTheOnlyActiveProfile(String activeProfiles) {
		assertThat(cleanDisabledWith(activeProfiles)).isTrue();
	}

	@Test
	void cleanIsLeftAsConfiguredWhenTestIsTheOnlyActiveProfile() {
		assertThat(cleanDisabledWith("test")).isFalse();
	}

	private static boolean cleanDisabledWith(String activeProfiles) {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(StringUtils.commaDelimitedListToStringArray(activeProfiles));
		FluentConfiguration configuration = new FluentConfiguration().cleanDisabled(false);

		new FlywayCleanGuard(environment).customize(configuration);

		return configuration.isCleanDisabled();
	}
}
