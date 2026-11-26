package com.harvester.socialmediaharvester.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.harvester.socialmediaharvester.config.FirstAdminProperties;
import com.harvester.socialmediaharvester.model.Role;
import com.harvester.socialmediaharvester.model.UserAccount;
import com.harvester.socialmediaharvester.repository.UserAccountRepository;
import com.harvester.socialmediaharvester.support.AppTest;
import com.harvester.socialmediaharvester.support.FirstAdmin;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** The first Admin is created from config on an empty database only, with the password hashed. */
@AppTest
class FirstAdminServiceTest {

	static final FirstAdminProperties CONFIGURED = new FirstAdminProperties(FirstAdmin.EMAIL, FirstAdmin.PASSWORD);

	@Autowired
	UserAccountRepository accounts;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	Clock clock;

	@AfterEach
	void restoreTheFirstAdmin() {
		if (Boolean.FALSE.equals(accounts.findByEmailIgnoringCase(FirstAdmin.EMAIL).hasElement().block())) {
			accounts.deleteAll().then(seeder(CONFIGURED).seedIfEmpty()).block();
		}
	}

	@Test
	void theStoredPasswordIsAHashOfTheConfiguredOne() {
		StepVerifier.create(accounts.findByEmailIgnoringCase(FirstAdmin.EMAIL))
				.assertNext(account -> {
					assertThat(account.passwordHash()).isNotEqualTo(FirstAdmin.PASSWORD)
							.doesNotContain(FirstAdmin.PASSWORD);
					assertThat(passwordEncoder.matches(FirstAdmin.PASSWORD, account.passwordHash())).isTrue();
				})
				.verifyComplete();
	}

	@Test
	void anExistingUserMeansNoSeeding() {
		var someoneElse = new FirstAdminProperties("someone.else@harvester.test", "someone-else-password");

		StepVerifier.create(seeder(someoneElse).seedIfEmpty()).verifyComplete();

		StepVerifier.create(accounts.findByEmailIgnoringCase("someone.else@harvester.test")).verifyComplete();
		StepVerifier.create(accounts.count()).expectNext(1L).verifyComplete();
	}

	@Test
	void anEmptyDatabaseGetsTheConfiguredAdmin() {
		Instant now = clock.instant();

		StepVerifier.create(accounts.deleteAll().then(seeder(CONFIGURED).seedIfEmpty())).verifyComplete();

		StepVerifier.create(accounts.findAll())
				.assertNext(account -> {
					assertThat(account.email()).isEqualTo(FirstAdmin.EMAIL);
					assertThat(account.role()).isEqualTo(Role.ADMIN);
					assertThat(account.createdAt()).isEqualTo(now);
				})
				.verifyComplete();
	}

	@Test
	void anEmptyDatabaseWithoutFirstAdminConfigIsReportedClearly() {
		var missing = new FirstAdminProperties(null, null);

		StepVerifier.create(accounts.deleteAll().then(seeder(missing).seedIfEmpty()))
				.expectErrorSatisfies(error -> assertThat(error)
						.isInstanceOf(IllegalStateException.class)
						.hasMessageContaining("HARVESTER_FIRST_ADMIN_EMAIL")
						.hasMessageContaining("HARVESTER_FIRST_ADMIN_PASSWORD"))
				.verify();
	}

	@Test
	void anotherInstanceSeedingFirstMeansAlreadySeeded() {
		// Another instance saved the first Admin between this instance's count and save:
		// the count saw an empty database, the save hits the unique email index.
		UserAccountRepository racing = mock(UserAccountRepository.class);
		when(racing.count()).thenReturn(Mono.just(0L));
		when(racing.save(any(UserAccount.class))).thenAnswer(call -> accounts.save(call.getArgument(0)));

		StepVerifier.create(new FirstAdminService(CONFIGURED, passwordEncoder, clock, racing).seedIfEmpty())
				.verifyComplete();

		StepVerifier.create(accounts.count()).expectNext(1L).verifyComplete();
	}

	private FirstAdminService seeder(FirstAdminProperties properties) {
		return new FirstAdminService(properties, passwordEncoder, clock, accounts);
	}
}
