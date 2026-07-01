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

## UI

### Writing tests

- **Test Naming:** `test.describe` blocks describe a feature. Tests should be named starting with "should ...".
- **Test Structure:** Keep tests flat by default. Omit top-level `test.describe` blocks. If grouping is needed, target a specific feature (e.g., filtering notifications) rather than a parent concept like the whole page. Example: `test.describe("Filter Notifications", ...)` instead of `test.describe("Notifications", ...)`.

### Fixtures

- **Avoid Redundant Actions:** Do not create methods in fixtures that simply wrap native Playwright actions (e.g., `click()`, `getByRole()`, `expect()`). Use native Playwright actions directly in the tests where possible.
- **Provide Locators:** Provide fixture methods for locators with non-standard or complex paths (e.g., reliant on specific DOM structures) so others can reuse the correct locators across tests.

### App tests

- **Compilation:** Run `./gradlew clean installDist` after making changes to the UI source code to ensure they are applied before testing.
- **Location:** Define tests in `ui/app/<app-name>/test/`. Define fixtures in `ui/app/<app-name>/test/fixtures/`.
- **Comments:** Add scenario comments above tests (`@given`, `@when`, `@then`, `@and`) based on the acceptance criteria.
- **Auth State:** Select correct `storageState` for the task to be tested. Use `adminStatePath` for master/admin tasks. Use `userStatePath` for regular realm user tasks.

### Component tests

- **Location:** Define tests in `ui/component/<component-name>/test/`. Define fixtures in `ui/component/<component-name>/test/fixtures/`.
