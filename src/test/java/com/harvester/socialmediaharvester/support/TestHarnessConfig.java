package com.harvester.socialmediaharvester.support;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestHarnessConfig {

	/** Static so it outlives each test context: the database is wiped once per test run, not once per context. */
	private static final AtomicBoolean CLEANED = new AtomicBoolean();

	/**
	 * Wipes harvester_test and rebuilds it from the migrations when the first test context starts.
	 * Later contexts only migrate, so they never wipe data under a context that is still cached.
	 */
	@Bean
	FlywayMigrationStrategy cleanOnceThenMigrate() {
		return flyway -> {
			if (CLEANED.compareAndSet(false, true)) {
				flyway.clean();
			}
			flyway.migrate();
		};
	}

	@Bean
	@Primary
	TestClock testClock() {
		return new TestClock();
	}

	@Bean(destroyMethod = "close")
	PlatformApiStub platformApiStub() {
		return new PlatformApiStub();
	}
}
