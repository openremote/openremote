# Start from Ubuntu devcontainer base
FROM mcr.microsoft.com/devcontainers/base:ubuntu

# Set a faster mirror and apt timeouts
# RUN sed -i 's|http://ports.ubuntu.com/ubuntu-ports|http://mirror.kumi.systems/ubuntu-ports|g' /etc/apt/sources.list && \
#     echo 'Acquire::Retries "3"; Acquire::Timeout "10"; Acquire::http::Timeout "10"; Acquire::ftp::Timeout "10";' > /etc/apt/apt.conf.d/99timeout

# Install basic packages
RUN apt-get update && apt-get install -y \
    openjdk-21-jdk \
    git \
    curl \
    unzip \
    ca-certificates \
    gnupg \
    iputils-ping \
    && rm -rf /var/lib/apt/lists/*

# Install SDKMAN and Gradle
RUN curl -s "https://get.sdkman.io" | bash && \
    bash -c "source /root/.sdkman/bin/sdkman-init.sh && sdk install gradle 8.5"

# Install Node.js 20
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs

# Install Yarn globally
RUN npm install -g yarn

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
ENV PATH=$JAVA_HOME/bin:$PATH

# Set working directory
WORKDIR /workspace