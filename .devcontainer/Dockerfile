# Base image for Java 17
FROM mcr.microsoft.com/devcontainers/java:1-17

# Install Node.js and Yarn
RUN curl -fsSL https://deb.nodesource.com/setup_21.x | bash - && \
    apt-get update && \
    apt-get install -y nodejs

# Verify installations
RUN java -version && \
    node --version && \
    yarn --version

# Set up the working directory
WORKDIR /workspace