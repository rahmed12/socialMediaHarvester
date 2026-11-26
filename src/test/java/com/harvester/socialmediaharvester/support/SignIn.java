package com.harvester.socialmediaharvester.support;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/** Signs in through the real sign-in form and hands back a client carrying the session. */
public final class SignIn {

	private SignIn() {
	}

	public static WebTestClient.ResponseSpec submit(WebTestClient client, String email, String password) {
		return client.mutateWith(csrf()).post().uri("/login")
				.body(BodyInserters.fromFormData("username", email).with("password", password))
				.exchange();
	}

	public static WebTestClient as(WebTestClient client, String email, String password) {
		ResponseCookie session = submit(client, email, password)
				.expectStatus().isFound()
				.expectHeader().location("/")
				.returnResult(Void.class)
				.getResponseCookies().getFirst("SESSION");
		if (session == null) {
			throw new AssertionError("sign-in as " + email + " did not start a session");
		}
		return client.mutate().defaultCookie(session.getName(), session.getValue()).build();
	}
}
