package com.harvester.socialmediaharvester.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** A {@link Clock} that stands still until a test sets or advances it. Always UTC. */
public final class TestClock extends Clock {

	public static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

	private final AtomicReference<Instant> now = new AtomicReference<>(START);

	public void setInstant(Instant instant) {
		now.set(instant);
	}

	public void advance(Duration duration) {
		now.updateAndGet(current -> current.plus(duration));
	}

	@Override
	public Instant instant() {
		return now.get();
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		throw new UnsupportedOperationException("The harvester runs on UTC");
	}
}
