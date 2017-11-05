FROM centos:7

RUN yum update -y && yum -y install java-1.8.0-openjdk-devel && yum clean all
ENV JAVA_HOME /usr/lib/jvm/java
ENV JAVA_OPTS -Xmx1024m

ENV MANAGER_DOCROOT webapp
ENV CONSOLES_DOCROOT /deployment/consoles
ENV UI_DOCROOT /deployment/ui
ENV LOGGING_CONFIG_FILE /deployment/logging.properties
ENV MAP_TILES_PATH /deployment/map/mapdata.mbtiles
ENV MAP_SETTINGS_PATH /deployment/map/mapsettings.json

EXPOSE 8080

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD curl --fail http://localhost:8080 || exit 1

WORKDIR /opt/app

ADD deployment /deployment
RUN mkdir -p /deployment/extensions

ADD server /opt/app
ADD client /opt/app

ENTRYPOINT java $JAVA_OPTS -cp /opt/app/lib/*:/deployment/extensions org.openremote.manager.server.Main
