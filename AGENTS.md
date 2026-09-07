# OpenRemote AGENTS.md file

## DB update scripts

Flyway is used to automate execution of DB update scripts.  
When naming scripts, use \<date>_\<time> as version number, date being YYYYMMDD and time being HHmm in UTC e.g. `V20260611_0755__Changes.sql` (the `V` prefix is required by Flyway and must be uppercase)

## Dev environment

The backend is written in Java, we target JDK 21, use modern language features up to that version during implementation.  
Gradle is used as the build system; run `./gradlew clean installDist` for a full clean build.  
In sandboxed environments, add `--offline` (requires dependencies to already be cached).

## Running tests

When running integration tests, part of the stack (PostgreSQL and Keycloak) must be running. Start it with `mkdir -pm 777 tmp && docker compose -f profile/dev-testing.yml -p openremote up -d --no-build`.  
Running `./gradlew clean` deletes the root `tmp/` directory that is mounted into PostgreSQL (see `profile/dev-testing.yml`), so recreate it and restart the stack before running tests again.

## REST resources

### Endpoint roles

Annotate every endpoint with the resource role of its domain, e.g. `read:alarms` or `write:notifications`. Use `read:admin` or `write:admin` only where the domain has no resource role, or where admin-only access is a deliberate decision. The admin roles are standalone, so holding `write:admin` does not grant `write:notifications`.

### Access control

Access control belongs in the resource implementation, not in the service. Extend `ManagerWebResource` and use its checks rather than reaching for the identity provider: `throwIfNotRealmActiveAndAccessible` and `throwIfRestrictedUser` reject with a 403, and `isRealmActiveAndAccessible` and `isRestrictedUser` are there for conditions that need more than a rejection. Resolve the entity first and return 404 when it is missing, then require access to its realm, and apply restricted user rules in the same place. Service methods take already authorised input and state that in their javadoc with "Callers are responsible for enforcing realm authorization." Alarms and notifications are the reference for this shape; equivalent endpoints across the two must enforce the same way. Flows that never pass through a resource, such as notifications published to the message broker, authorise inside the service instead.

## UI

Frontend conventions live in `ui/AGENTS.md`. Read it before changing anything under `ui/`.
