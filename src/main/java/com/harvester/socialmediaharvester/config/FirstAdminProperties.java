package com.harvester.socialmediaharvester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;

/**
 * The Admin created when the database has no user accounts yet. Both values may
 * be absent: they are required only when seeding is needed, which the service checks.
 * A value that is supplied is always validated.
 */
@Validated
@ConfigurationProperties("harvester.first-admin")
public record FirstAdminProperties(@Email String email, String password) {

	@Override
	public String toString() {
		return "FirstAdminProperties[email=" + email + ", password=****]";
	}
}
