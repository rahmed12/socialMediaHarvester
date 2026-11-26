package com.harvester.socialmediaharvester.service;

import org.springframework.stereotype.Service;

import com.harvester.socialmediaharvester.exception.SignedInAccountNotFoundException;
import com.harvester.socialmediaharvester.model.UserAccount;
import com.harvester.socialmediaharvester.repository.UserAccountRepository;

import reactor.core.publisher.Mono;

/** Looks up the people on our team who sign in. */
@Service
public class UserAccountService {

	private final UserAccountRepository accounts;

	public UserAccountService(UserAccountRepository accounts) {
		this.accounts = accounts;
	}

	/** The account with this email in any case, or empty. */
	public Mono<UserAccount> findByEmail(String email) {
		return accounts.findByEmailIgnoringCase(email);
	}

	/**
	 * The account of the person signed in with this email. Fails with
	 * {@link SignedInAccountNotFoundException} when it has gone since they signed in.
	 */
	public Mono<UserAccount> findSignedIn(String email) {
		return findByEmail(email).switchIfEmpty(Mono.error(SignedInAccountNotFoundException::new));
	}
}
