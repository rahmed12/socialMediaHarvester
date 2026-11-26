package com.harvester.socialmediaharvester.exception;

/**
 * The session's principal names a user account that no longer exists, for example
 * because it was deleted after signing in. The person has to sign in again.
 */
public class SignedInAccountNotFoundException extends RuntimeException {

	public SignedInAccountNotFoundException() {
		super("The signed-in user account no longer exists");
	}
}
