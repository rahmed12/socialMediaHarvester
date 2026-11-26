package com.harvester.socialmediaharvester.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;

import com.harvester.socialmediaharvester.exception.ProblemDetailWriter;

import reactor.core.publisher.Mono;

/**
 * Answers an API request made without signing in with 401 and a {@link ProblemDetail}
 * body, the API's one error format. The security filter chain runs before any
 * controller, so the REST exception handler never sees this failure.
 */
public class ProblemDetailAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

	private final ProblemDetailWriter problems;

	public ProblemDetailAuthenticationEntryPoint(ProblemDetailWriter problems) {
		this.problems = problems;
	}

	@Override
	public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
		return problems.write(exchange,
				ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Sign in to use the API."));
	}
}
