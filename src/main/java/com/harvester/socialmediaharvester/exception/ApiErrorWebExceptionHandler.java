package com.harvester.socialmediaharvester.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import reactor.core.publisher.Mono;

/**
 * Answers API errors that never reach a controller, or that no controller advice handled,
 * with a {@link ProblemDetail}: an unknown path, an unsupported method or media type, an
 * unexpected failure. Ordered before Spring Boot's default error handler, which would
 * otherwise answer with its own JSON. Page errors are left to that default handling.
 */
@Component
@Order(-2)
public class ApiErrorWebExceptionHandler implements WebExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiErrorWebExceptionHandler.class);

	private static final PathPattern API_PATHS = PathPatternParser.defaultInstance.parse("/api/**");

	private final ProblemDetailWriter problems;

	public ApiErrorWebExceptionHandler(ProblemDetailWriter problems) {
		this.problems = problems;
	}

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable failure) {
		if (exchange.getResponse().isCommitted()
				|| !API_PATHS.matches(exchange.getRequest().getPath().pathWithinApplication())) {
			return Mono.error(failure);
		}
		if (failure instanceof ErrorResponse error) {
			exchange.getResponse().getHeaders().addAll(error.getHeaders());
			return problems.write(exchange, ProblemDetail.forStatus(error.getStatusCode()));
		}
		log.error("Unexpected failure handling {} {}", exchange.getRequest().getMethod(),
				exchange.getRequest().getPath(), failure);
		return problems.write(exchange, ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR));
	}
}
