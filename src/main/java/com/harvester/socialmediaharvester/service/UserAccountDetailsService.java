package com.harvester.socialmediaharvester.service;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/** Finds who is signing in by email (any case); the principal's name is the stored email. */
@Service
public class UserAccountDetailsService implements ReactiveUserDetailsService {

	private final UserAccountService accounts;

	public UserAccountDetailsService(UserAccountService accounts) {
		this.accounts = accounts;
	}

	@Override
	public Mono<UserDetails> findByUsername(String email) {
		return accounts.findByEmail(email)
				.map(account -> User.withUsername(account.email())
						.password(account.passwordHash())
						.roles(account.role().name())
						.build());
	}
}
