package com.harvester.socialmediaharvester.support;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

/** Blocking calls BlockHound may ignore, each with the reason it is safe. */
public class BlockHoundAllowList implements BlockHoundIntegration {

	@Override
	public void applyTo(BlockHound.Builder builder) {
		// SecureRandom (NativePRNG) refills its small buffer from /dev/urandom here. That read
		// never waits for entropy and returns in microseconds; any SecureRandom caller on an
		// event loop (CSRF tokens, session ids, R2DBC SCRAM nonces) can trigger it.
		builder.allowBlockingCallsInside("sun.security.provider.NativePRNG$RandomIO", "implNextBytes");
	}
}
