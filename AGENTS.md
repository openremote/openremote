# OpenRemote AGENTS.md file

## DB update scripts

Flyway is used to automate execution of DB update scripts.  
When naming scripts, use \<date>_\<time> as version number, date being YYYYMMDD and time being HHmm in UTC e.g. `v20260611_0755__Changes.sql`

## Dev environment
The backend is written in Java, we target JDK 21, use modern language features up to that version during implementation.  
Gradle is used as the build system; run `./gradlew clean installDist` for a full clean build.  
In sandboxed environments, add `--offline` (requires dependencies to already be cached).

## Running tests

When running integration tests, part of the stack (PostgreSQL and Keycloak) must be running. Start it with `mkdir -pm 777 tmp && docker compose -f profile/dev-testing.yml -p openremote up -d --no-build`.  
Running `./gradlew clean` deletes the root `tmp/` directory that is mounted into PostgreSQL (see `profile/dev-testing.yml`), so recreate it and restart the stack before running tests again.
