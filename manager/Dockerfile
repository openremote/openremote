FROM java:8

ENV MANAGER_DOCROOT webapp
ENV CONSOLE_DOCROOT conf/console
ENV LOGGING_CONFIG_FILE conf/logging.properties
ENV MAP_TILES_PATH conf/mapdata.mbtiles
ENV MAP_SETTINGS_PATH conf/mapsettings.json

ADD server /opt/app
ADD client /opt/app

EXPOSE 8080

WORKDIR /opt/app

ENTRYPOINT /opt/app/bin/server
