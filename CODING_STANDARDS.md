# Coding standards

The rules every change follows, checked in review. They apply to layer-first, reactive Spring Boot applications using WebFlux and R2DBC. Coding standards specific to this project: [CODING_STANDARDS_PROJECT_SPECIFIC.md](CODING_STANDARDS_PROJECT_SPECIFIC.md).

## Packages

The application is organised layer-first. Place a class by what it does; its name, suffix and annotations then reflect that responsibility. A class that talks to TikTok's API is a `TikTokApiClient` in `client`, not a `TikTokApiService` in `service`. When the architecture test fails, fix the design, not the test result. If the class does something other than its package's responsibility, move it to the package that matches what it does. If it is in the right package but calls a package it may not use, call through the layer in between instead (a controller that needs data calls a service, which calls the repository). Renaming a class or moving it into a more permissive package such as `config` turns the test green while leaving the problem in place.

| Package | Responsibility |
| --- | --- |
| `config` | Wiring: `@Configuration` classes that build beans, `@ConfigurationProperties` records, startup triggers (an `ApplicationRunner` that calls a service) and startup guards (`EnvironmentPostProcessor`, Flyway customizers). |
| `controller` | The HTTP edge: read the request, call a service, map the result to a `dto`. A static server-rendered page, such as a sign-in form, needs no service. |
| `service` | Business behaviour: business rules and repository calls. A business failure is signalled with an application exception from `exception`. |
| `repository` | Data access: Spring Data repositories and other storage implementations. |
| `model` | Domain types: entities, value objects, enums. A model need not be a table; a result an external integration returns is a model too. |
| `dto` | API records the frontend sends and receives, such as `CreateUserRequest` and `UserResponse`. |
| `mapper` | Conversions between `model` and `dto` that more than one controller uses, or that are too long to sit in one. A one-line mapping stays a private method in its controller. |
| `exception` | Application exceptions, and the error handlers that turn failures into `ProblemDetail` responses (see [Exception handling](#exception-handling)), with any helper those handlers share, such as a `ProblemDetail` writer. |
| `client` | Calls to external APIs and the external API's own request and response types. |
| `security` | Security infrastructure beyond wiring: a credential encryptor, a custom authentication manager or entry point. `SecurityConfig` stays in `config` and wires them. |

Create a package when its first class arrives; an empty package is left out. A new kind of package (`scheduler`, `event`) needs a row in both tables here and a layer in the architecture test, so its place in the architecture is decided once, in review.

### Dependencies

Each package may use only the application packages in its row. Arrows mean "may depend on", not the order a request runs in. Framework and library types are not limited here.

| Package | May depend on |
| --- | --- |
| `controller` | `service`, `dto`, `mapper`, `model` (to map it to a `dto`), `exception` |
| `service` | `repository`, `model`, `exception`, `client`, `security`, `config` (`@ConfigurationProperties` records only) |
| `repository` | `model` |
| `mapper` | `model`, `dto` |
| `model` | other `model` types |
| `dto` | other `dto` types and `model` enums |
| `exception` | `dto`, `model` |
| `client` | `model`, `exception`, `config` (`@ConfigurationProperties` records only) |
| `security` | `model`, `exception`, `config` (`@ConfigurationProperties` records only) |
| `config` | any package, to wire it |

Nothing depends on `controller`. `exception` holds two kinds of class: application exceptions, which the packages that list `exception` in their row may use, and error handlers (`GlobalExceptionHandler` and a `WebExceptionHandler`), HTTP boundary components that Spring invokes and no application code references. Services represent business failures with application exceptions. `service`, `repository` and `model` never use HTTP-specific types (`ResponseEntity`, `HttpStatus`, `ResponseStatusException`, `@ResponseStatus`) to represent failures.

An ArchUnit architecture test checks this table, the two `exception` rules above, and that framework types sit in their package: `@RestController` and `@Controller` in `controller`, exceptions, `@RestControllerAdvice` and `WebExceptionHandler` implementations in `exception`, `@Configuration` and `@ConfigurationProperties` in `config`, `@Table` in `model`, Spring Data repositories in `repository`.

Why: a controller that skips the service skips the business rules with it. An entity returned from a controller makes every column part of the public API, so a schema change breaks the frontend; a `dto` built from a model by a controller or mapper keeps the two free to change apart.

## Classes and interfaces

Prefer concrete classes. Introduce an interface when a framework requires one (Spring Data repositories), when there are interchangeable implementations (one per external provider), when it defines a meaningful contract between modules, or when it isolates the application from an external technology (a storage engine the business logic must stay independent of). Interfaces exist for design, not for convention or mocking: Spring proxies concrete classes and Mockito mocks them. Names carry no `I` prefix and no `Impl` suffix; name an implementation by what makes it different (`YouTubeClient`, `InMemoryNoteRepository`).

Prefer polymorphism and dynamic method dispatch (runtime polymorphism or late binding) over repeated conditional logic when behavior varies by type, platform, provider or implementation. Introduce interfaces for interchangeable implementations, abstract base classes for meaningful shared behavior or state, and factories for encapsulating object creation or implementation selection. Prefer composition over inheritance. Introduce these abstractions only when they solve a concrete design problem, never in anticipation of one.

Spring-managed components use constructor injection with `final` dependency fields: their dependencies are visible in one place, and a test builds the class with `new`, without a Spring context. Classes and methods that need a class-based Spring proxy (`@Transactional`, `@Cacheable`, `@Async`) are not `final`, since the proxy is a subclass that overrides them.

## Reactive code

- Reactive types are for asynchronous work: I/O, the database, HTTP calls, timers. A pure calculation returns a plain value (`int add(int a, int b)`), and its caller maps it into a pipeline.
- Blocking work (password hashing, file I/O, a blocking library) is wrapped lazily: `Mono.fromCallable(() -> readFile()).subscribeOn(Schedulers.boundedElastic())`. `Mono.just(readFile())` runs `readFile()` immediately, on the calling thread, before any scheduler applies; it is never a wrapper for blocking work.
- Controllers and services return composed pipelines and leave subscription to the framework. Calling `subscribe()` yourself starts work nobody awaits, cancels or reports. It is allowed only at an application lifecycle boundary (a scheduled job, a startup runner), with error handling and cancellation management.
- Never block on a reactive thread (event loop or `parallel` scheduler). `.block()` is allowed only on a thread you control (startup, tests, a dedicated job), with a comment saying why the code can't stay reactive. Inside request handling, compose with `flatMap`/`zip` instead.
- JDBC (`java.sql`, `javax.sql`, Spring JDBC) is used only for Flyway migrations and startup database checks. Request handling and application data access use R2DBC.
- Time is read from an injected `Clock`, never `Instant.now()`, so tests control it.

### Transactions

- Database operations that must succeed or fail together run in one reactive transaction: a `@Transactional` service method returning `Mono`/`Flux`, or `TransactionalOperator` around only the part of a pipeline that needs it.
- Transactional work executes within the same reactive pipeline and transaction context: it is composed into the returned `Mono`/`Flux`, never started in a separate `subscribe()` or on another pipeline.
- `@Transactional` works through the Spring proxy, so a call from another method in the same class bypasses it. Put the transaction boundary on the service method other beans call, use `TransactionalOperator`, or move the transactional operation to another service when it belongs there.
- A transaction stays open only around database work. Calls to an external API happen before or after it, so a slow external service never holds a database connection and its locks.

## Exception handling

- Application exceptions live in `exception` and are named for the failure: `UserNotFoundException`, `DuplicateUserException`.
- Services detect business failures and signal them as application exceptions (`Mono.error(new UserNotFoundException(id))`).
- `exception.GlobalExceptionHandler` is the one place that turns an application exception into an HTTP status and response. It is declared `@RestControllerAdvice(annotations = RestController.class)`, so it covers the REST API and leaves server-rendered pages alone, and it extends WebFlux's `ResponseEntityExceptionHandler`, so Spring's own web errors (bad request bodies, unsupported media types) come out as `ProblemDetail` too. Controllers let exceptions propagate.
- Every REST API error body is a Spring `ProblemDetail` (RFC 9457), so the frontend reads one format.
- Authentication and authorization failures raised in the security filter chain are answered by Spring Security, as configured in `SecurityConfig`, not by `GlobalExceptionHandler`. A method-security failure (`@PreAuthorize`) is raised inside a service but takes the same route: `GlobalExceptionHandler` leaves `AccessDeniedException` and `AuthenticationException` alone so they reach Spring Security. For API paths the answer is a `ProblemDetail`, written by the authentication entry point (401) and access-denied handler (403). Browser pages keep the sign-in flow and redirect to the sign-in page.
- Some errors happen before or outside any controller: an unknown API path, an unsupported method or media type, an exception no handler caught. Controller advice never sees them, so a `WebExceptionHandler` in `exception`, ordered before Spring Boot's default error handler, answers them on API paths with a `ProblemDetail` and leaves pages to the default handling.
- Client responses never expose stack traces, credentials, SQL statements or internal implementation details.
- Server logs contain enough sanitized information to diagnose failures. Credentials, passwords, tokens and other secrets are never logged.

| Failure | HTTP status |
| --- | --- |
| Invalid request | 400 |
| Authentication required | 401 |
| Permission denied | 403 |
| Resource not found | 404 |
| Conflicting resource | 409 |
| Unexpected application failure | 500 |

Tests prove the `ProblemDetail` contract for API authentication and authorization errors, separately from page redirects, and for a request to an unknown API path.

## Naming

Use the terms in the project glossary and none of its _Avoid_ words.

## Configuration

- YAML only. Shared settings in `application.yml`, one `application-<profile>.yml` per environment in `src/main/resources`, and `application-test.yml` in `src/test/resources`.
- Exactly one environment profile is selected explicitly with `SPRING_PROFILES_ACTIVE`; no file activates a profile. Without one Spring Boot would run in its implicit `default` profile, so a startup guard stops startup unless exactly one environment profile is active and it chooses a database. Tests activate `test` with `@ActiveProfiles`.
- Each profile file names its own database host, port and name.
- Secrets and deployment values come from environment variables. Database credentials come only from environment variables, with no default. A required setting has no default, so a missing value fails startup with a message naming the variable.
- Whether a setting is required and whether a supplied value is valid are separate rules. A setting needed only in some situations carries no `@NotNull`; the service that needs it checks it is present and fails with a message naming the environment variables. A supplied value is still validated on its `@Validated` record (`@Email` on an email).
- Configuration classes use `@Configuration(proxyBeanMethods = false)` unless inter-bean method interception is required. A `@Bean` method takes the beans it needs as parameters instead of calling other `@Bean` methods.
- Configuration property records are registered with `@EnableConfigurationProperties` on a config class and validated with `@Validated` where constraints apply.
- Logging: root `INFO`, the application's own level set by an environment variable defaulting to `INFO`, `DEBUG` in `local`. Log messages never contain credentials, passwords, tokens or other secrets; records holding them override `toString()`.

## Database

- All schema changes are Flyway migrations in `src/main/resources/db/migration`.
- A migration already applied to a persistent environment is never modified, comments included, since Flyway checksums the whole file; the change goes in a new migration.
- Data integrity is enforced by database constraints (`NOT NULL`, `UNIQUE`, `CHECK`, foreign keys), with application validation on top, not instead.
- Repositories read and write data; they never create or alter schema.
- Tests connect only to the test database, never to a development or production database.

## Tests

Test packages mirror main: a test sits in the package of the class it tests. `architecture` holds the architecture test; `support` holds the shared test harness.

| Test type | Verifies |
| --- | --- |
| Controller | Request validation, response bodies and HTTP status codes, including `ProblemDetail` errors |
| Service | Business rules and failure scenarios |
| Repository | Custom queries and persistence against the test database |
| Configuration (`config`) | Profiles, properties, startup guards and security rules |
| Architecture | Package placement and permitted dependencies |
| Integration | Several parts working together through the booted app |

- Reactive results through `StepVerifier`. `.block()` is acceptable in test setup and teardown only.
- Assertions on credentials compare booleans so a failure never prints a value.

### BlockHound

- BlockHound is installed in the test JVM (`blockhound-junit-platform`) before any test runs, and stays enabled; a test is never made to pass by disabling it.
- Investigate every `BlockingOperationError`. It usually means blocking work in main code: move that work to `Schedulers.boundedElastic()`. Framework or library code may require a narrowly scoped allowance after investigation establishes that the call is acceptable on a non-blocking thread (NativePRNG refilling from `/dev/urandom`). Being framework code does not by itself make a blocking call safe.
- Allow-list only verified, narrowly scoped calls (one class and method) in a single allow-list class in `support`, each with a comment saying why it is safe.
- A test proves BlockHound still rejects a deliberate blocking call, so an allow-list entry can never silently switch detection off.
