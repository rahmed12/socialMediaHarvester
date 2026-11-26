# Social Media Harvester

## Verification checks

Run these checks in this order before you report a change as complete:

1. `./mvnw test -Dtest=LayerBoundariesTest -Dskip.npm`: layer boundaries, the Java `lint:boundaries`. Fast, needs no database.
2. `./mvnw clean verify`: compiles, builds the React app, and runs every Java and frontend test with BlockHound.


### Seeing the app run

`SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run`, then `curl localhost:8080/actuator/health`. Sign in as the first Admin to check a sign-in or UI change yourself instead of asking the person you're working for.

## When something breaks

| Symptom | Cause |
| --- | --- |
| `BlockingOperationError` in a test | A blocking call on a reactive thread, usually in main code: move that work to `Schedulers.boundedElastic()`. If the stack shows safe framework or library code, allow-list that one method in `support.BlockHoundAllowList` with a comment saying why. Keep BlockHound enabled. |
| `LayerBoundariesTest` fails | A class sits outside the package of its responsibility, or uses a package its row in the CODING_STANDARDS.md dependency table forbids. Move the class to the package that matches what it does, or call through the layer in between (controller → service → repository). Renaming it or moving it into `config` only hides the problem. |
| Flyway "checksum mismatch" at startup | An applied `V*__*.sql` was edited, comments included. Restore it and put the change in a new migration. |
| Startup: "... is set from somewhere else" | Database credentials come only from `DB_USER_HARVEST` / `DB_PASSWORD_HARVEST`. Drop the `-D`, argument or `SPRING_R2DBC_*` override. |
| React change missing from the running app | Maven builds `frontend/` into the jar at `process-resources`; restart through `./mvnw`. |
