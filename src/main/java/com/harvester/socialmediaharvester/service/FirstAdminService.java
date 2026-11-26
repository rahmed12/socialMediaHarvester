package com.harvester.socialmediaharvester.service;

import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.harvester.socialmediaharvester.config.FirstAdminProperties;
import com.harvester.socialmediaharvester.model.Role;
import com.harvester.socialmediaharvester.model.UserAccount;
import com.harvester.socialmediaharvester.repository.UserAccountRepository;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Creates the first Admin from {@code harvester.first-admin.*} when the database
 * has no user accounts yet; with any account present it does nothing and needs
 * no config. Fails with a clear message when it has to seed but the config is
 * missing.
 */
@Service
public class FirstAdminService {

	private static final Logger log = LoggerFactory.getLogger(FirstAdminService.class);

	private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(1);

	private final FirstAdminProperties firstAdmin;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final UserAccountRepository accounts;

	public FirstAdminService(FirstAdminProperties firstAdmin, PasswordEncoder passwordEncoder, Clock clock,
			UserAccountRepository accounts) {
		this.firstAdmin = firstAdmin;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.accounts = accounts;
	}

	/**
	 * The one wait in the app (ADR 0012): called once at startup on the main
	 * thread, which is not a reactive thread, so startup cannot finish before
	 * the first Admin exists.
	 */
	public void seedIfEmptyAndWait() {
		seedIfEmpty().block(STARTUP_TIMEOUT);
	}

	/**
	 * Another instance starting at the same time may save the first Admin between
	 * this count and save; the unique email index then refuses the second save,
	 * which counts as already seeded.
	 */
	public Mono<Void> seedIfEmpty() {
		return accounts.count()
				.filter(count -> count == 0)
				.flatMap(none -> newAdmin())
				.flatMap(accounts::save)
				.then()
				.onErrorResume(DuplicateKeyException.class, duplicate -> {
					log.info("The first Admin was already created by another instance; nothing to seed");
					return Mono.empty();
				});
	}

	private Mono<UserAccount> newAdmin() {
		if (!StringUtils.hasText(firstAdmin.email()) || !StringUtils.hasText(firstAdmin.password())) {
			return Mono.error(new IllegalStateException("The database has no user accounts, so the first Admin "
					+ "must be created, but harvester.first-admin.email or harvester.first-admin.password is missing. "
					+ "Set HARVESTER_FIRST_ADMIN_EMAIL and HARVESTER_FIRST_ADMIN_PASSWORD."));
		}
		// Hashing is slow on purpose, so it stays off the event loop.
		return Mono.fromCallable(() -> new UserAccount(null, firstAdmin.email().trim(),
				passwordEncoder.encode(firstAdmin.password()), Role.ADMIN, clock.instant()))
				.subscribeOn(Schedulers.boundedElastic());
	}
}
