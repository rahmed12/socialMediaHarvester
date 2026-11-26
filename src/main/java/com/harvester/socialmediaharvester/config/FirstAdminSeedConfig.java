package com.harvester.socialmediaharvester.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.harvester.socialmediaharvester.service.FirstAdminService;

/** Makes sure the first Admin exists once Flyway has migrated, before the app is reported ready. */
@Configuration(proxyBeanMethods = false)
public class FirstAdminSeedConfig {

	@Bean
	ApplicationRunner seedFirstAdmin(FirstAdminService firstAdmin) {
		return args -> firstAdmin.seedIfEmptyAndWait();
	}
}
