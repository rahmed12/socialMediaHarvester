# Reactive stack end to end: WebFlux, WebClient and R2DBC

The backend is reactive from the HTTP edge to the database: Spring WebFlux serves the React app and its API, Harvesters call Platforms with `WebClient` and return `Mono<Harvest>`, PostgreSQL is reached through Spring Data R2DBC, and Spring Security runs its reactive variant (form login on `ServerHttpSecurity`). No `.block()` in application code.

We chose this over a blocking Spring MVC app with `RestClient` and JPA, and over reactive Harvesters inside a blocking core. Performance did not decide it: collection is limited by each Platform's Rate Limit, not by threads, and blocking code on Java 25 virtual threads would have been simpler and fast enough. We chose it deliberately for the team's learning and experience with the reactive model, accepting the extra complexity.

Consequences:
- No JPA: Spring Data R2DBC has no lazy loading or mapped relations, so aggregates are loaded with explicit queries. Raw Payloads use the R2DBC PostgreSQL driver's `Json` codec for `jsonb` (ADR 0007 still holds).
- Schema migrations (Flyway or Liquibase) still run over JDBC at startup; that is the only JDBC use.
- Run claiming (`SELECT … FOR UPDATE SKIP LOCKED`) and the one-snapshot-per-run unique key work unchanged inside reactive transactions (`@Transactional` or `TransactionalOperator`).
- Any library added later must be non-blocking, or be isolated on `Schedulers.boundedElastic()`. BlockHound in tests catches blocking calls on event-loop threads.
- Tests use `StepVerifier` and `WebTestClient`.
- Sessions live in the in-memory `WebSession`, which is enough for one instance; Kubernetes with several instances will need Spring Session.

See [the decision](https://github.com/rahmed12/socialMediaHarvester/issues/26).

Amended by [ADR 0012](0012-startup-may-block-once-to-seed-the-first-admin.md): startup may block once to seed the first Admin.
