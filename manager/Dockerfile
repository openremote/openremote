# TODO: Switch to adoptopenjdk 8u232-b9 once it also supports armhf
FROM debian:stretch-slim

# JRE package expects man dir to exist but doesn't in slim
RUN mkdir -p /usr/share/man/man1

RUN apt-get update && apt-get install -y curl openjdk-8-jre-headless --no-install-recommends && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME /etc/alternatives/java

# Add git commit label must be specified at build time using --build-arg GIT_COMMIT=dadadadadad
ARG GIT_COMMIT=unknown

LABEL git-commit=$GIT_COMMIT

ENV JAVA_OPTS=${JAVA_OPTS}
ENV JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS}

ENV DEV_MODE false
ENV APP_DOCROOT /opt/web
ENV CUSTOM_APP_DOCROOT /deployment/manager/app
ENV LOGGING_CONFIG_FILE /deployment/manager/logging.properties
ENV MAP_TILES_PATH /deployment/map/mapdata.mbtiles
ENV MAP_SETTINGS_PATH /deployment/map/mapsettings.json
ENV PROVISION_DOCROOT /deployment/manager/provisioning

EXPOSE 8080

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=120 CMD curl --fail --silent http://localhost:8080 || exit 1

RUN mkdir -p /opt/app

WORKDIR /opt/app

RUN mkdir -p /deployment/manager/extensions

ADD lib /opt/app/lib
ADD web /opt/web

ENTRYPOINT java $JAVA_OPTS -cp /opt/app/lib/*:/deployment/manager/extensions/* org.openremote.manager.Main
