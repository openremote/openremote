# hadolint ignore=DL3007
FROM openremote/manager:latest AS manager
# hadolint ignore=DL3007
FROM openremote/deployment:latest AS deployment
FROM openjdk:8-jre

# Add git commit label must be specified at build time using --build-arg GIT_COMMIT=dadadadadad
ARG GIT_COMMIT=unknown
LABEL git-commit=$GIT_COMMIT

ENV JAVA_OPTS -Xmx1g

ENV APP_DOCROOT /deployment/manager/app
ENV SHARED_DOCROOT /deployment/manager/shared
ENV LOGGING_CONFIG_FILE /deployment/manager/logging.properties
ENV MAP_TILES_PATH /deployment/map/mapdata.mbtiles
ENV MAP_SETTINGS_PATH /deployment/map/mapsettings.json

EXPOSE 8080

HEALTHCHECK --interval=3s --timeout=3s --start-period=300s --retries=120 CMD curl --fail --silent http://localhost:8080 || exit 1

WORKDIR /opt/app

RUN mkdir -p /deployment/extensions

COPY --from=manager /opt/app/lib /opt/app/lib
COPY --from=deployment /deployment/ /deployment/
# hadolint ignore=DL3025
ENTRYPOINT java $JAVA_OPTS -cp /opt/app/lib/*:/deployment/manager/extensions/* org.openremote.manager.Main
