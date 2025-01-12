# Use a base image that already has a JDK installed (for Gradle)
FROM gradle:8-jdk17

# Install Node.js (and optionally yarn if you prefer)
USER root
RUN apt-get update && \
    apt-get install -y curl gnupg && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install nodejs -y && \
    npm install --global yarn && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Optionally, set a default workdir
WORKDIR /workspace

CMD ["tail", "-f", "/dev/null"]