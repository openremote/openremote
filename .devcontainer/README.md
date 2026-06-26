# OpenRemote DevContainer Setup (Beta)

This DevContainer provides a complete development environment for OpenRemote with Java, Node.js, Docker, and all necessary tools pre-configured.

Please note that this feature is still in beta and may not be supported or work correctly in all IDEs.
Tested and working with IntelliJ.

## Quick Start

1. **Open in DevContainer**: IntelliJ should automatically detect the DevContainer configuration and prompt you to reopen in container.

2. **Wait for Setup**: The DevContainer will automatically:
    - Install Java 21, Node.js, and required tools
    - Start Docker services (PostgreSQL, Keycloak)
    - Build the project with `./gradlew clean installDist`

3. **Launch Configurations Available**:
    - **Manager (Debug Demo Setup, one-click)**
    - **Manager (Debug Test Setup, one-click)**
    - **Manager (Debug Empty Setup, one-click)**

