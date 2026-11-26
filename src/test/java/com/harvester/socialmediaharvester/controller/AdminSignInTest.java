package com.harvester.socialmediaharvester.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.harvester.socialmediaharvester.support.AppTest;
import com.harvester.socialmediaharvester.support.FirstAdmin;
import com.harvester.socialmediaharvester.support.SignIn;

@AppTest
class AdminSignInTest {

	@Autowired
	WebTestClient client;

	@Test
	void firstAdminSignsInWithEmailAndPassword() {
		SignIn.as(client, FirstAdmin.EMAIL, FirstAdmin.PASSWORD)
				.get().uri("/api/me").exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.email").isEqualTo(FirstAdmin.EMAIL)
				.jsonPath("$.role").isEqualTo("ADMIN");
	}

	@Test
	void signingInLeadsToTheReactApp() {
		ResponseCookie session = SignIn.submit(client, FirstAdmin.EMAIL, FirstAdmin.PASSWORD)
				.expectStatus().isFound()
				.expectHeader().location("/")
				.returnResult(Void.class)
				.getResponseCookies().getFirst("SESSION");
		assertThat(session).as("sign-in starts a session").isNotNull();

		client.get().uri("/").cookie(session.getName(), session.getValue()).accept(MediaType.TEXT_HTML).exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
				.expectBody(String.class).value(page -> assertThat(page).contains("<div id=\"root\"></div>"));
	}

	@Test
	void emailIsMatchedIgnoringCase() {
		SignIn.as(client, FirstAdmin.EMAIL.toUpperCase(), FirstAdmin.PASSWORD)
				.get().uri("/api/me").exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.email").isEqualTo(FirstAdmin.EMAIL);
	}

	@Test
	void wrongPasswordIsRefused() {
		SignIn.submit(client, FirstAdmin.EMAIL, "not-the-password")
				.expectStatus().isFound()
				.expectHeader().location("/login?error");
	}

	@Test
	void unknownEmailIsRefused() {
		SignIn.submit(client, "nobody@harvester.test", FirstAdmin.PASSWORD)
				.expectStatus().isFound()
				.expectHeader().location("/login?error");
	}
}
