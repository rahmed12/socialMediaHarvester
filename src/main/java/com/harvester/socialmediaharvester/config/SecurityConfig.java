package com.harvester.socialmediaharvester.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint.DelegateEntry;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import com.harvester.socialmediaharvester.exception.ProblemDetailWriter;
import com.harvester.socialmediaharvester.security.ProblemDetailAuthenticationEntryPoint;

/**
 * Form sign-in on {@link ServerHttpSecurity} (ADR 0010). Only the sign-in page, the
 * built React assets and Actuator {@code health} (with its probe groups) are reachable without signing in.
 * Pages redirect to the sign-in form; the API answers 401 with a {@code ProblemDetail}.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ProblemDetailWriter problems) {
		var apiRefused = new DelegateEntry(ServerWebExchangeMatchers.pathMatchers("/api/**"),
				new ProblemDetailAuthenticationEntryPoint(problems));
		var entryPoint = new DelegatingServerAuthenticationEntryPoint(List.of(apiRefused));
		entryPoint.setDefaultEntryPoint(new RedirectServerAuthenticationEntryPoint("/login"));

		return http
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers("/login", "/assets/**", "/favicon.ico", "/actuator/health", "/actuator/health/**").permitAll()
						.anyExchange().authenticated())
				.formLogin(form -> form.loginPage("/login"))
				.exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint))
				.build();
	}
}
