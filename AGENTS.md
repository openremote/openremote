# OpenRemote AGENTS.md file

## DB update scripts

Flyway is used to automate execution of DB update scripts.
When naming scripts, use <date>_<time> as version number, date being YYYYMMDD and time being HHmm in UTC e.g. 20260611_0755

## Dev environment
The backend is written in Java, we target JDK 21, use modern language features up to that version during implementation.
Gradle is used as the build system, use ./gradlew --offline clean installDist for a full clean build.
When running gradle, use --offline option to avoid sandboxing issues.

## Running tests

When running integration tests, part of the stack (PostgreSQL and Keycloak) must be running. This can be done by running `docker-compose -f profile/dev-testing.yml -p openremote up`.
Running `./gradlew clean` will remove some files required by the tests and thus the stack must be stopped and re-started before running the tests again.
