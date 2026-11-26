package com.harvester.socialmediaharvester.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * A local HTTP server standing in for Platform APIs. Point a Platform's base URL
 * at {@link #baseUrl()}, queue recorded responses (or errors and rate-limit
 * answers) and inspect what the app sent.
 */
public final class PlatformApiStub implements AutoCloseable {

	private final MockWebServer server = new MockWebServer();

	public PlatformApiStub() {
		try {
			server.start();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not start the Platform API stub", ex);
		}
	}

	public String baseUrl() {
		String url = server.url("/").toString();
		return url.substring(0, url.length() - 1);
	}

	public void enqueue(MockResponse response) {
		server.enqueue(response);
	}

	public RecordedRequest takeRequest() throws InterruptedException {
		RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
		if (request == null) {
			throw new AssertionError("The Platform API stub received no request");
		}
		return request;
	}

	@Override
	public void close() {
		server.close();
	}
}
