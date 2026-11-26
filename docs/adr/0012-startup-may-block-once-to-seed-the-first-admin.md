# Startup may block once to seed the first Admin

This amends ADR 0010, which says "No `.block()` in application code". Startup creates the first Admin when the database has no user accounts (issue #28), and the app must not report itself ready, or accept a sign-in, before that Admin exists. If seeding fails, for example because `HARVESTER_FIRST_ADMIN_*` is missing, startup must fail with that message. Spring Boot's `ApplicationRunner` is synchronous: it can only hold startup open, or fail it, if the reactive seed is waited on. So `FirstAdminService.seedIfEmptyAndWait()` calls `.block()` on the main thread, with a timeout.

We chose this over subscribing without waiting. That way the app would report ready with no Admin, and a missing config would show up only as a log line. We also chose it over seeding in a Flyway migration, which cannot read runtime config or hash a password.

Consequences:
- `.block()` is allowed in application code only on a thread the app controls, never on a reactive thread (event loop or `parallel`). Each use carries a comment saying why the code can't stay reactive (CODING_STANDARDS.md, Reactive code).
- Everything else in ADR 0010 still holds.
