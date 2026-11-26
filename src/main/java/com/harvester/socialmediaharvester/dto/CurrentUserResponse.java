package com.harvester.socialmediaharvester.dto;

import com.harvester.socialmediaharvester.model.Role;

/** Who is signed in, so the React app can shape itself to the role. */
public record CurrentUserResponse(String email, Role role) {
}
