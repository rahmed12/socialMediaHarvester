package com.harvester.socialmediaharvester.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.harvester.socialmediaharvester.support.AppTest;

import reactor.test.StepVerifier;

/** Errors that never reach a controller still answer API requests with a ProblemDetail. */
@AppTest
class ApiErrorWebExceptionHandlerTest {

	@Autowired
	WebTestClient client;

	private final ApiErrorWebExceptionHandler handler = new ApiErrorWebExceptionHandler(new ProblemDetailWriter(ServerCodecConfigurer.create()));

	@Test
	void anUnknownApiPathIsAProblemDetail() {
		client.mutateWith(mockUser()).get().uri("/api/does-not-exist").accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(404)
				.jsonPath("$.title").isEqualTo("Not Found")
				.jsonPath("$.instance").isEqualTo("/api/does-not-exist");
	}

	@Test
	void anUnsupportedMethodIsAProblemDetailNamingTheAllowedMethods() {
		client.mutateWith(mockUser()).mutateWith(csrf()).delete().uri("/api/me").exchange()
				.expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectHeader().exists("Allow")
				.expectBody().jsonPath("$.status").isEqualTo(405);
	}

	@Test
	void anUnexpectedFailureIsA500ThatRevealsNothing() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/me"));

		StepVerifier.create(handler.handle(exchange, new IllegalStateException("SELECT password FROM user_account")))
				.verifyComplete();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		StepVerifier.create(exchange.getResponse().getBodyAsString())
				.assertNext(body -> assertThat(body).contains("\"status\":500").doesNotContain("SELECT")
						.doesNotContain("password"))
				.verifyComplete();
	}

	@Test
	void pageErrorsAreLeftToTheDefaultHandling() {
		var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/no-such-page"));
		var failure = new IllegalStateException("page failure");

		StepVerifier.create(handler.handle(exchange, failure))
				.expectErrorMatches(error -> error == failure)
				.verify();
	}
}
