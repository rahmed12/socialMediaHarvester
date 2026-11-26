package com.harvester.socialmediaharvester.exception;

import java.net.URI;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Writes a {@link ProblemDetail} as the response, for error handlers that run outside a
 * controller and so cannot return one for Spring to render. One bean, so the codec is
 * looked up once and shared by every such handler.
 */
@Component
public class ProblemDetailWriter {

	private static final ResolvableType PROBLEM = ResolvableType.forClass(ProblemDetail.class);

	private final HttpMessageWriter<ProblemDetail> writer;

	@SuppressWarnings("unchecked")
	public ProblemDetailWriter(ServerCodecConfigurer codecs) {
		this.writer = codecs.getWriters().stream()
				.filter(candidate -> candidate.canWrite(PROBLEM, MediaType.APPLICATION_PROBLEM_JSON))
				.map(candidate -> (HttpMessageWriter<ProblemDetail>) candidate)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No codec writes application/problem+json"));
	}

	/** Sets the status and instance from the problem and request, then writes the body. */
	public Mono<Void> write(ServerWebExchange exchange, ProblemDetail problem) {
		problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
		var response = exchange.getResponse();
		response.setStatusCode(HttpStatusCode.valueOf(problem.getStatus()));
		return writer.write(Mono.just(problem), PROBLEM, MediaType.APPLICATION_PROBLEM_JSON, response, Map.of());
	}
}
