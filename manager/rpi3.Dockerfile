FROM openremote/rpi-raspbian:latest

ENV LANG C.UTF-8

RUN [ "cross-build-start" ]
RUN apt-get update && apt-get install -y openjdk-8-jdk-headless --no-install-recommends && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-armhf
RUN [ "cross-build-end" ]

ENV MANAGER_DOCROOT webapp
ENV CONSOLE_DOCROOT deployment/resources_console
ENV LOGGING_CONFIG_FILE deployment/logging.properties
ENV MAP_TILES_PATH deployment/mapdata.mbtiles
ENV MAP_SETTINGS_PATH deployment/mapsettings.json

ADD server /opt/app
ADD client /opt/app

EXPOSE 8080

WORKDIR /opt/app

ENTRYPOINT ["java", "-cp", "/opt/app/lib/*", "org.openremote.manager.server.Main"]
