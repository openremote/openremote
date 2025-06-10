# OpenRemote VS Code DevContainer

## Summary of What's Been Set Up

Your devcontainer now provides a complete OpenRemote development environment with:

### 🐳 **Pre-configured Services**
- **PostgreSQL** (port 5432) - Database
- **Keycloak** (port 8081) - Identity management
- **Docker-in-Docker** - For running containers

### ☕ **Java Development**
- **Java 21** with Gradle support
- **VS Code Java Extension Pack** 
- **4 Launch Configurations** ready to use:
  - `OpenRemote Manager (Default)` - Standard setup
  - `Demo Setup` - With demo data
  - `Test Setup` - For testing/integration
  - `Empty` - Minimal setup

### 🌐 **Frontend Development**
- **Node.js** with npm/yarn
- **Manager UI development server** configuration
- **TypeScript** support

### 🛠️ **Development Tasks**
- `gradle: build` - Build the project
- `gradle: clean` - Clean artifacts
- `gradle: installDist` - Build and install
- `docker: start/stop services` - Manage dependencies
- `npm: serve manager UI` - Start frontend dev server

## 🚀 How to Use

### 1. **Start Development**
The devcontainer automatically:
- Builds the project (`gradle clean installDist`)
- Starts required Docker services
- Sets up VS Code with proper extensions

### 2. **Run Backend**
1. Open Run and Debug panel (`Ctrl+Shift+D`)
2. Select a launch configuration (e.g., "Demo Setup")
3. Press F5 or click the green play button
4. Access at: http://localhost:8080/manager/
5. Login: `admin` / `secret`

### 3. **Run Frontend**
1. Use the "Debug npm run serve" launch config, OR
2. Terminal → Run Task → "npm: serve manager UI"
3. Access at: http://localhost:9000/manager/

## 🔧 Troubleshooting

### If Java Launch Fails:
```bash
# Check Docker services
docker ps

# Rebuild if needed
./gradlew clean installDist

# Check VS Code Java extension is active
```

### If Docker Services Fail:
```bash
# Check Docker
docker info

# Restart services
docker compose -p openremote -f profile/dev-testing.yml up -d

# Check logs
docker compose -f profile/dev-testing.yml logs
```

## 📁 Key Files

- `.devcontainer/devcontainer.json` - Container configuration
- `.vscode/launch.json` - Debug configurations  
- `.vscode/tasks.json` - Build tasks
- `.vscode/settings.json` - Java settings
- `profile/dev-testing.yml` - Docker services

## 🎯 Next Steps

1. **Try the Demo Setup** - Use "Demo Setup" launch config for a full demo environment
2. **Explore the UI** - Use the npm serve task to develop the frontend
3. **Write Tests** - Use "Test Setup" for integration testing
4. **Debug** - Set breakpoints and use F5 to debug Java code

Your devcontainer is now fully configured and ready for OpenRemote development! 🎉
