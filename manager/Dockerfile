FROM centos:7

RUN yum update -y && yum -y install java-1.8.0-openjdk-devel && yum clean all
ENV JAVA_HOME /usr/lib/jvm/java

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
