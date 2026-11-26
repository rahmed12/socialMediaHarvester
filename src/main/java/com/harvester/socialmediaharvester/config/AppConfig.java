package com.harvester.socialmediaharvester.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirstAdminProperties.class)
public class AppConfig {

	/** The one source of "now"; tests replace it to move time. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	/** Stores passwords hashed (bcrypt by default, prefixed with the algorithm id). */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
