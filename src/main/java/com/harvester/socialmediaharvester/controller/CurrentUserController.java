package com.harvester.socialmediaharvester.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harvester.socialmediaharvester.dto.CurrentUserResponse;
import com.harvester.socialmediaharvester.model.UserAccount;
import com.harvester.socialmediaharvester.service.UserAccountService;

import reactor.core.publisher.Mono;

/** {@code /api/me}: the signed-in user's email and Role. */
@RestController
public class CurrentUserController {

	private final UserAccountService accounts;

	public CurrentUserController(UserAccountService accounts) {
		this.accounts = accounts;
	}

	@GetMapping("/api/me")
	Mono<CurrentUserResponse> currentUser(@AuthenticationPrincipal UserDetails principal) {
		return accounts.findSignedIn(principal.getUsername()).map(CurrentUserController::toResponse);
	}

	private static CurrentUserResponse toResponse(UserAccount account) {
		return new CurrentUserResponse(account.email(), account.role());
	}
}
