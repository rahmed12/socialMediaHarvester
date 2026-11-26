package com.harvester.socialmediaharvester.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the whole app in the {@code test} profile against {@code harvester_test}
 * (cleaned and migrated once per test run), with a {@code WebTestClient},
 * a controllable {@link TestClock} and a {@link PlatformApiStub} ready.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestHarnessConfig.class)
public @interface AppTest {
}
