# Coding standards: Social Media Harvester

How [CODING_STANDARDS.md](CODING_STANDARDS.md) applies to this project, plus the rules only this project has. Alongside them:

- Domain terms to use in code: [GLOSSARY.md](GLOSSARY.md)
- Architecture decisions a change must respect: [docs/adr/](docs/adr/)
- Running the app, profiles and required environment variables: [README.md](README.md#run)

## Packages

- Every class sits under `com.harvester.socialmediaharvester`.
- The architecture test is `architecture.LayerBoundariesTest`.
- A per-Platform `Harvester` is an interface with one implementation per Platform. A `Harvest` it returns is a `model`, and a Platform's API calls live in `client`.
- `SignInController` serves the sign-in form as static HTML, with no service call.

## Naming

- A class named `Harvester*` is a per-Platform Harvester and nothing else.
- Say "sign in" in code and UI text; "Login" appears only as part of **Platform Login** and in the `/login` URL.

## Errors and security

- API paths are `/api/**`. Without signing in they answer 401 with a `ProblemDetail` from `security.ProblemDetailAuthenticationEntryPoint`; pages redirect to `/login`. No endpoint is role-restricted yet, so the 403 `ProblemDetail` handler arrives with the first one. Application exceptions from controllers become a `ProblemDetail` in `exception.GlobalExceptionHandler`; a signed-in account that no longer exists answers 401, so the React app returns to `/login`. Other API errors outside a controller are answered by `exception.ApiErrorWebExceptionHandler`. Handlers that write the response themselves share the `exception.ProblemDetailWriter` bean.
- A slow Platform API never runs inside a database transaction.

## Configuration

- Environment profiles: `local`, `dev`, `prod`, and `test` for tests only, named once in `config.EnvironmentProfile`. `config.ActiveProfileCheck` refuses no profile, an unknown one, or more than one.
- Each profile file names its own database host, port and name (issue #28).
- Database credentials come only from `DB_USER_HARVEST` / `DB_PASSWORD_HARVEST`; `config.DatabaseCredentialsCheck` refuses any other source.
- The first-Admin settings (`HARVESTER_FIRST_ADMIN_EMAIL`, `HARVESTER_FIRST_ADMIN_PASSWORD`) are needed only on an empty database; `FirstAdminService` checks they are present, and `FirstAdminProperties` validates a supplied email.
- The application's log level is `APP_LOG_LEVEL`.
- JDBC appears only in `config.Flyway*` and `config.DatabaseCredentialsCheck`.

## Tests

- Tests use only `harvester_test`, cleaned and migrated once per test run.
- `@AppTest` boots the whole app for integration tests. `support` also holds `TestClock`, `PlatformApiStub` and `BlockHoundAllowList`.
- `HarnessTest` holds the check that BlockHound rejects a deliberate blocking call.
