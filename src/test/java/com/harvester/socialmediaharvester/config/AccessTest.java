package com.harvester.socialmediaharvester.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.harvester.socialmediaharvester.support.AppTest;

@AppTest
class AccessTest {

	@Autowired
	WebTestClient client;

	@Test
	void healthIsReachableWithoutSigningIn() {
		client.get().uri("/actuator/health").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.status").isEqualTo("UP");
	}

	@Test
	void healthProbePathsAreReachableWithoutSigningIn() {
		client.get().uri("/actuator/health/liveness").accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().value(status -> assertThat(status).as("not redirected to sign in").isNotEqualTo(302));
	}

	@Test
	void anonymousHealthCheckStartsNoSession() {
		var response = client.get().uri("/actuator/health").exchange()
				.expectStatus().isOk()
				.returnResult(Void.class);

		assertThat(response.getResponseCookies()).doesNotContainKey("SESSION");
	}

	@Test
	void signInPageIsReachableWithoutSigningIn() {
		client.get().uri("/login").exchange()
				.expectStatus().isOk();
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE })
	void signInPageIsHtmlWhateverTheRequestAccepts(String accept) {
		client.get().uri("/login").header("Accept", accept).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
	}

	@ParameterizedTest
	@ValueSource(strings = { "/", "/index.html", "/actuator/info", "/anything" })
	void pagesRedirectToSignInWithoutSigningIn(String path) {
		client.get().uri(path).accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().isFound()
				.expectHeader().location("/login");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/me", "/api/anything" })
	void apiIsUnauthorizedWithoutSigningIn(String path) {
		client.get().uri(path).accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().isUnauthorized();
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE, MediaType.ALL_VALUE })
	void apiRefusalIsAProblemDetailWhateverTheRequestAccepts(String accept) {
		client.get().uri("/api/me").header("Accept", accept).exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(401)
				.jsonPath("$.title").isEqualTo("Unauthorized")
				.jsonPath("$.instance").isEqualTo("/api/me");
	}
}
