package com.harvester.socialmediaharvester.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_account")
public record UserAccount(@Id Long id, String email, String passwordHash, Role role, Instant createdAt) {

	@Override
	public String toString() {
		return "UserAccount[id=" + id + ", email=" + email + ", role=" + role + "]";
	}
}
