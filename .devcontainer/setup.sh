#!/bin/bash
set -e

echo "Starting setup..."

# Ensure gradlew is executable
if [ -f "gradlew" ]; then
    chmod +x gradlew
fi

# Disable Gradle parallel execution for VS Code / devcontainer
mkdir -p ~/.gradle
cat <<EOF >> ~/.gradle/gradle.properties
org.gradle.parallel=false
EOF

# Install dependencies
echo "Running gradle clean installDist..."
./gradlew clean installDist

# Start Docker services
echo "Starting Docker services..."
export OR_HOSTNAME=host.docker.internal
docker compose -p openremote -f profile/dev-testing.yml up -d

echo "Setup complete!"
