package com.harvester.socialmediaharvester.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * The one place an application exception becomes an HTTP status and a {@link ProblemDetail}.
 * Covers the REST API only; server-rendered pages keep their own handling. Spring's own web
 * errors come out as {@code ProblemDetail} through {@link ResponseEntityExceptionHandler}.
 * {@code AccessDeniedException} and {@code AuthenticationException} are deliberately not
 * handled here, so they reach Spring Security as configured in {@code SecurityConfig}.
 */
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/** Authentication required: the React app sends the person back to the sign-in page. */
	@ExceptionHandler(SignedInAccountNotFoundException.class)
	Mono<ResponseEntity<Object>> signedInAccountNotFound(SignedInAccountNotFoundException failure,
			ServerWebExchange exchange) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Sign in again.");
		problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
		return handleExceptionInternal(failure, problem, null, HttpStatus.UNAUTHORIZED, exchange);
	}
}
