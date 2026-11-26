package com.harvester.socialmediaharvester.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HtmlUtils;

import reactor.core.publisher.Mono;

/** The email and password form posted to {@code /login}. */
@RestController
public class SignInController {

	/** Always HTML, whatever the request accepts. */
	@GetMapping("/login")
	Mono<ResponseEntity<String>> signInPage(ServerWebExchange exchange) {
		Mono<CsrfToken> token = exchange.getAttributeOrDefault(CsrfToken.class.getName(), Mono.empty());
		Outcome outcome = Outcome.of(exchange);
		return token.map(csrf -> html(csrf, outcome))
				.defaultIfEmpty(html(null, outcome))
				.map(body -> ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body));
	}

	/** What brought the person to the form, and the message it shows them. */
	private enum Outcome {
		NONE(""),
		FAILED("<p role=\"alert\">Wrong email or password.</p>"),
		SIGNED_OUT("<p role=\"status\">You have been signed out.</p>");

		private final String message;

		Outcome(String message) {
			this.message = message;
		}

		static Outcome of(ServerWebExchange exchange) {
			var query = exchange.getRequest().getQueryParams();
			return query.containsKey("error") ? FAILED : query.containsKey("logout") ? SIGNED_OUT : NONE;
		}
	}

	private static String html(CsrfToken csrf, Outcome outcome) {
		String csrfField = csrf == null ? ""
				: "<input type=\"hidden\" name=\"%s\" value=\"%s\">".formatted(
						HtmlUtils.htmlEscape(csrf.getParameterName()), HtmlUtils.htmlEscape(csrf.getToken()));
		return """
				<!doctype html>
				<html lang="en">
				<head><meta charset="utf-8"><title>Sign in - Social Media Harvester</title></head>
				<body>
				<main>
				<h1>Sign in</h1>
				%s
				<form method="post" action="/login">
				<label>Email <input type="email" name="username" autocomplete="username" required autofocus></label>
				<label>Password <input type="password" name="password" autocomplete="current-password" required></label>
				%s
				<button type="submit">Sign in</button>
				</form>
				</main>
				</body>
				</html>
				""".formatted(outcome.message, csrfField);
	}
}
