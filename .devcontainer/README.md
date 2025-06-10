# OpenRemote DevContainer Setup

This devcontainer provides a complete development environment for OpenRemote with Java 21, Node.js, Docker, and all necessary tools pre-configured.

## Quick Start

1. **Open in DevContainer**: VS Code should automatically detect the devcontainer configuration and prompt you to reopen in container.

2. **Wait for Setup**: The devcontainer will automatically:
   - Install Java 21, Node.js, and required tools
   - Start Docker services (PostgreSQL, Keycloak)
   - Build the project with `gradle clean installDist`

3. **Launch Configurations Available**:
   - **OpenRemote Manager (Default)**: Standard manager application
   - **Demo Setup**: Manager with demo data (`OR_SETUP_TYPE=demo`)
   - **Test Setup**: Manager for testing/integration
   - **Empty**: Basic manager without setup data
   - **Debug npm run serve (ui/app/manager)**: Frontend development server

## Development Workflow

### Backend Development
1. Use any of the Java launch configurations from the Run and Debug panel (Ctrl+Shift+D)
2. The application will connect to the pre-started Docker services
3. Access the manager at: http://localhost:8080/manager/
4. Default credentials: `admin` / `secret`

### Frontend Development
1. Use the "Debug npm run serve (ui/app/manager)" launch configuration, or
2. Run the "npm: serve manager UI" task from Terminal → Run Task
3. Access the dev server at: http://localhost:9000/manager/

### Available Tasks
- **gradle: build**: Build the entire project
- **gradle: clean**: Clean build artifacts  
- **gradle: installDist**: Clean and install distribution
- **docker: start services**: Start required Docker services
- **docker: stop services**: Stop Docker services
- **npm: serve manager UI**: Start the manager UI development server

## Troubleshooting

### If Java launch configurations fail:
1. Ensure Docker services are running: `docker ps`
2. Run the build task: Terminal → Run Task → "gradle: installDist"
3. Check that the Java Extension Pack is installed and active

### If Docker services fail to start:
1. Check Docker is running: `docker info`
2. Restart services: Terminal → Run Task → "docker: start services"
3. Check logs: `docker compose -f profile/dev-testing.yml logs`

### Port Conflicts:
- Manager: 8080
- Keycloak: 8081  
- PostgreSQL: 5432
- Manager UI Dev Server: 9000

## Environment Variables

The launch configurations include these important environment variables:
- `OR_HOSTNAME=localhost`: OpenRemote hostname
- `WEBSERVER_LISTEN_HOST=0.0.0.0`: Bind to all interfaces
- `OR_SETUP_TYPE=demo`: Enable demo data (Demo Setup only)
- `OR_SETUP_RUN_ON_RESTART=true`: Re-run setup on restart

## Files Structure

- `.devcontainer/`: DevContainer configuration
- `.vscode/launch.json`: Debug/launch configurations
- `.vscode/tasks.json`: Build and utility tasks
- `.vscode/settings.json`: Java and editor settings
- `profile/dev-testing.yml`: Docker services for development
