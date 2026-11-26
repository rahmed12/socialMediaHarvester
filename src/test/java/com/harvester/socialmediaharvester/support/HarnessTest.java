package com.harvester.socialmediaharvester.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;

import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.blockhound.BlockingOperationError;
import reactor.test.StepVerifier;

/** The harness every later test relies on works as advertised. */
@AppTest
class HarnessTest {

	@Autowired
	Clock clock;

	@Autowired
	TestClock testClock;

	@Autowired
	PlatformApiStub platformApi;

	@Test
	void theAppsClockIsTheTestClockAndOnlyMovesWhenTold() {
		testClock.setInstant(Instant.parse("2026-01-01T09:00:00Z"));
		assertThat(clock.instant()).isEqualTo(Instant.parse("2026-01-01T09:00:00Z"));

		testClock.advance(Duration.ofHours(25));

		assertThat(clock.instant()).isEqualTo(Instant.parse("2026-01-02T10:00:00Z"));
	}

	@Test
	void platformApiStubAnswersWithTheQueuedResponse() throws InterruptedException {
		platformApi.enqueue(new MockResponse.Builder()
				.code(200)
				.addHeader("Content-Type", "application/json")
				.body("{\"items\":[]}")
				.build());

		Mono<String> call = WebClient.create(platformApi.baseUrl()).get().uri("/youtube/v3/channels?part=id")
				.retrieve().bodyToMono(String.class);

		StepVerifier.create(call).expectNext("{\"items\":[]}").verifyComplete();
		RecordedRequest request = platformApi.takeRequest();
		assertThat(request.getTarget()).isEqualTo("/youtube/v3/channels?part=id");
	}

	@Test
	void blockHoundRejectsBlockingCallsOnNonBlockingThreads() {
		Mono<Void> blocking = Mono.<Void>fromRunnable(() -> {
			try {
				Thread.sleep(1);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}).subscribeOn(Schedulers.parallel());

		StepVerifier.create(blocking).verifyError(BlockingOperationError.class);
	}

	@Test
	void secureRandomRefillIsAllowedOnNonBlockingThreads() {
		// 64 bytes exceeds NativePRNG's 32-byte buffer, so this always reads /dev/urandom
		Mono<Void> random = Mono.<Void>fromRunnable(() -> new SecureRandom().nextBytes(new byte[64]))
			.subscribeOn(Schedulers.parallel());

		StepVerifier.create(random).verifyComplete();
	}
}
