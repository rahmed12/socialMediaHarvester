package com.harvester.socialmediaharvester.controller;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.harvester.socialmediaharvester.support.AppTest;

/** {@code /api/me} for a signed-in principal whose user account no longer exists. */
@AppTest
class CurrentUserControllerTest {

	@Autowired
	WebTestClient client;

	@Test
	void aSignedInAccountThatNoLongerExistsMustSignInAgain() {
		client.mutateWith(mockUser("deleted.account@harvester.test"))
				.get().uri("/api/me").exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(401)
				.jsonPath("$.title").isEqualTo("Unauthorized")
				.jsonPath("$.instance").isEqualTo("/api/me");
	}
}
