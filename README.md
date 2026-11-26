# Social Media Harvester

Collects posts and engagement data from social media Platforms for tracked Handles. Terms: [GLOSSARY.md](GLOSSARY.md). Decisions: [docs/adr/](docs/adr/).

## Run

Choose a profile with `SPRING_PROFILES_ACTIVE`. There is no default: without one, startup stops and says so.

| Profile | Database |
| --- | --- |
| `local` | `harvester_dev`, application logging at DEBUG |
| `dev` | `harvester_dev` |
| `prod` | `harvester_prod` |
| `test` | `harvester_test`, used by the tests only (`@ActiveProfiles("test")`) |

Each profile file (`src/main/resources/application-<profile>.yml`) holds its database host, port and name.

Environment variables:

| Variable | Required | Purpose |
| --- | --- | --- |
| `DB_USER_HARVEST`, `DB_PASSWORD_HARVEST` | always | Database login for both R2DBC and Flyway; nothing else can supply or override it |
| `HARVESTER_FIRST_ADMIN_EMAIL`, `HARVESTER_FIRST_ADMIN_PASSWORD` | when the database has no user accounts | The first Admin, created at startup |
| `PORT` | no (8080) | HTTP port |
| `APP_LOG_LEVEL` | no (INFO; DEBUG in `local`) | Log level of the application's own code |

```sh
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Tests: `./mvnw clean test` (runs the frontend tests too; needs `DB_USER_HARVEST` / `DB_PASSWORD_HARVEST` and touches only `harvester_test`).
