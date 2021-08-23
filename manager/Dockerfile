FROM adoptopenjdk/openjdk8:ubi-minimal-jre-nightly

### S6 installation
ENV S6_OVERLAY_VERSION=v2.2.0.3

RUN apkArch="$(uname -m)"; \
	case "${apkArch}" in \
        x86_64) s6Arch='amd64'; ;; \
        armv7) s6Arch='arm'; ;; \
        armv7l) s6Arch='arm'; ;; \
        armhf) s6Arch='armhf'; ;; \
        aarch64) s6Arch='aarch64'; ;; \
        ppc64le) s6Arch='ppc64le'; ;; \
        *) echo >&2 "Error: unsupported architecture ${apkArch}"; exit 1 ;; \
	esac; \
  curl -LfsSo /tmp/s6-overlay.tar.gz https://github.com/just-containers/s6-overlay/releases/download/${S6_OVERLAY_VERSION}/s6-overlay-${s6Arch}.tar.gz;

RUN tar xzf /tmp/s6-overlay.tar.gz -C / --exclude="./bin" && \
    tar xzf /tmp/s6-overlay.tar.gz -C /usr ./bin
### End S6 installation


# Add git commit label must be specified at build time using --build-arg GIT_COMMIT=dadadadadad
ARG GIT_COMMIT=unknown

LABEL git-commit=$GIT_COMMIT

ENV JAVA_TOOL_OPTIONS ${JAVA_TOOL_OPTIONS}

ENV TZ ${TZ:-Europe/Amsterdam}
ENV SETUP_ADMIN_PASSWORD ${SETUP_ADMIN_PASSWORD:-secret}
ENV EXTERNAL_URL ${EXTERNAL_URL:-https://localhost}
ENV SETUP_EMAIL_HOST ${SETUP_EMAIL_HOST}
ENV SETUP_EMAIL_USER ${SETUP_EMAIL_USER}
ENV SETUP_EMAIL_PASSWORD ${SETUP_EMAIL_PASSWORD}
ENV SETUP_EMAIL_PORT ${SETUP_EMAIL_PORT:-25}
ENV SETUP_EMAIL_TLS ${SETUP_EMAIL_TLS:-true}
ENV SETUP_EMAIL_FROM_KEYCLOAK ${SETUP_EMAIL_FROM_KEYCLOAK:-no-reply@localhost}
ENV SETUP_EMAIL_FROM_DEFAULT ${SETUP_EMAIL_FROM_DEFAULT:-no-reply@localhost}
ENV NOTIFICATION_FIREBASE_URL ${NOTIFICATION_FIREBASE_URL:-https://fcm.googleapis.com/fcm/send}
ENV NOTIFICATION_FIREBASE_API_KEY ${NOTIFICATION_FIREBASE_API_KEY}
ENV FIREBASE_CONFIG_FILE ${FIREBASE_CONFIG_FILE:-/deployment/manager/fcm.json}
ENV DEV_MODE ${DEV_MODE:-false}
ENV SETUP_WIPE_CLEAN_INSTALL ${SETUP_WIPE_CLEAN_INSTALL:-false}
ENV WEBSERVER_LISTEN_HOST ${WEBSERVER_LISTEN_HOST:-0.0.0.0}
ENV DB_VENDOR ${DB_VENDOR:-postgres}
ENV DB_HOST ${DB_HOST:-postgresql}
ENV DB_PORT ${DB_PORT:-5432}
ENV DB_NAME ${DB_NAME:-openremote}
ENV DB_SCHEMA ${DB_SCHEMA:-openremote}
ENV DB_USERNAME ${DB_USERNAME:-postgres}
ENV DB_PASSWORD ${DB_PASSWORD:-postgres}
ENV DB_MIN_POOL_SIZE ${DB_MIN_POOL_SIZE:-5}
ENV DB_MAX_POOL_SIZE ${DB_MAX_POOL_SIZE:-20}
ENV DB_CONNECTION_TIMEOUT_SECONDS ${DB_CONNECTION_TIMEOUT_SECONDS:-300}
ENV KEYCLOAK_HOST ${KEYCLOAK_HOST:-keycloak}
ENV KEYCLOAK_PORT ${KEYCLOAK_PORT:-8080}
ENV KEYCLOAK_GRANT_FILE ${KEYCLOAK_GRANT_FILE:-/deployment/manager/keycloak.json}
ENV APP_DOCROOT ${APP_DOCROOT:-/opt/web}
ENV CUSTOM_APP_DOCROOT ${CUSTOM_APP_DOCROOT:-/deployment/manager/app}
ENV PROVISIONING_DOCROOT ${PROVISIONING_DOCROOT:-/deployment/manager/provisioning}
ENV ROOT_REDIRECT_PATH ${ROOT_REDIRECT_PATH:-/manager}
ENV MAP_TILES_PATH ${MAP_TILES_PATH:-/deployment/map/mapdata.mbtiles}
ENV MAP_SETTINGS_PATH ${MAP_SETTINGS_PATH:-/deployment/map/mapsettings.json}
ENV DATA_POINTS_EXPORT_DIR ${DATA_POINTS_EXPORT_DIR:-/tmp}
ENV LOGGING_CONFIG_FILE ${LOGGING_CONFIG_FILE}
ENV MAP_TILESERVER_HOST ${MAP_TILESERVER_HOST}
ENV MAP_TILESERVER_PORT ${MAP_TILESERVER_PORT:-8082}
ENV MAP_TILESERVER_REQUEST_TIMEOUT ${MAP_TILESERVER_REQUEST_TIMEOUT:-10000}
ENV SCHEDULED_TASKS_THREADS_MAX ${SCHEDULED_TASKS_THREADS_MAX:-4}
ENV RULE_EVENT_EXPIRES ${RULE_EVENT_EXPIRES:-1h}
ENV IDENTITY_PROVIDER ${IDENTITY_PROVIDER:-keycloak}
ENV IDENTITY_SESSION_MAX_MINUTES ${IDENTITY_SESSION_MAX_MINUTES:-1440}
ENV IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES ${IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES:-2628000}
ENV MANAGER_JAVA_OPTS ${MANAGER_JAVA_OPTS:--Xms400m -Xmx400m \
    -XX:CompressedClassSpaceSize=25m -XX:MaxMetaspaceSize=150m \
    -XX:InitialCodeCacheSize=50m -XX:ReservedCodeCacheSize=50m \
    -XX:MaxDirectMemorySize=25m -XX:NativeMemoryTracking=summary \
    -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/javadump.core.hprof}

EXPOSE 8080

HEALTHCHECK --interval=3s --timeout=3s --start-period=30s --retries=120 CMD curl --fail --silent http://localhost:8080 || exit 1

RUN mkdir -p /opt/app
RUN mkdir -p /deployment/manager/extensions
RUN mkdir -p /zwave

WORKDIR /opt/app

ADD lib /opt/app/lib
ADD web /opt/web
ADD map /opt/map

ENTRYPOINT ["/init"]
CMD java $MANAGER_JAVA_OPTS -cp /opt/app/lib/*:/deployment/manager/extensions/* org.openremote.manager.Main
