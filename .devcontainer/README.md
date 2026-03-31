# OpenRemote DevContainer Setup

This devcontainer provides a complete development environment for OpenRemote with Java 21, Node.js, Docker, and all necessary tools pre-configured.

## Quick Start

1. **Open in DevContainer**: VS Code should automatically detect the devcontainer configuration and prompt you to reopen in container.

2. **Wait for Setup**: The devcontainer will automatically:
   - Install Java 21, Node.js, and required tools
   - Start Docker services (PostgreSQL, Keycloak)
   - Build the project with `./gradlew clean installDist`

3. **Launch Configurations Available**:
   - **Manager (Debug Demo Setup, one-click)**
   - **Manager (Debug Test Setup, one-click)**
   - **Manager UI: serve**

## Development Workflow

### Backend Development
1. Run one backend task from Terminal → Run Task (for example `gradle: run manager (demo)` or `gradle: debug manager (demo)`)
2. For one-click debug, start `Manager (Debug Demo Setup, one-click)` or `Manager (Debug Test Setup, one-click)` from Run and Debug (Ctrl+Shift+D)
3. The application will connect to the pre-started Docker services
4. Access the manager at: http://localhost:8080/manager/
5. Default credentials: `admin` / `secret`

### Frontend Development
1. Use the "Manager UI: serve" launch configuration, or
2. Run the "npm: serve manager UI" task from Terminal → Run Task
3. Access the dev server at: http://localhost:9000/manager/

### Available Tasks
- **gradle: start infra**: Start required Docker services
- **gradle: stop infra**: Stop Docker services
- **gradle: run manager (demo)**: Run Manager in demo mode
- **gradle: run manager (test)**: Run Manager with test setup
- **gradle: run manager (empty)**: Run Manager without setup profile
- **gradle: run manager (load1)**: Run Manager with load1 setup
- **gradle: debug manager (demo)**: Run Manager in debug mode (port 5005)
- **gradle: debug manager (test)**: Run Manager in debug mode (port 5006)
- **gradle: build manager UI**: Build manager UI workspace
- **npm: serve manager UI**: Start the manager UI development server

These devcontainer Gradle tasks are loaded from `gradle/devcontainer-run.gradle`.

## Troubleshooting

### If Java launch configurations fail:
1. Ensure Docker services are running: `docker ps`
2. Run the build task: `./gradlew clean installDist`
3. Check that the Java Extension Pack is installed and active

### If Docker services fail to start:
1. Check Docker is running: `docker info`
2. Restart services: Terminal → Run Task → "gradle: start infra"
3. Check logs: `docker compose -f profile/dev-testing.yml logs`

### Port Conflicts:
- Manager: 8080
- Keycloak: 8081  
- PostgreSQL: 5432
- Manager UI Dev Server: 9000

## Environment Variables

The launch configurations set host routing for the devcontainer:
- `OR_DB_HOST=host.docker.internal`
- `OR_KEYCLOAK_HOST=host.docker.internal`

You can override any task environment variable with a Gradle property, e.g.:
- `./gradlew runManagerDemo -POR_DB_HOST=localhost -POR_KEYCLOAK_HOST=localhost`

If you run these tasks outside the devcontainer, also enable them explicitly:
- `./gradlew -Popenremote.devcontainer.tasks=true runManagerDemo`

## Files Structure

- `.devcontainer/`: DevContainer configuration
- `.vscode/launch.json`: Debug/launch configurations
- `.vscode/tasks.json`: Build and utility tasks
- `gradle/devcontainer-run.gradle`: Devcontainer task definitions
- `profile/dev-testing.yml`: Docker services for development
