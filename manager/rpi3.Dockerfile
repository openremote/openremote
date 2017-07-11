FROM openremote/rpi-raspbian:latest

ENV LANG C.UTF-8

RUN [ "cross-build-start" ]

# OpenJDK with slow interpreter JIT
#RUN apt-get update && apt-get install -y openjdk-8-jdk-headless --no-install-recommends && rm -rf /var/lib/apt/lists/*
#ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-armhf

# Oracle JDK with bad licensing - Make sure you understand the cost before using this in production!
RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list  && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list  && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886  && \
    apt-get update && \
    echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections  && \
    echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections  && \
    DEBIAN_FRONTEND=noninteractive  apt-get install -y --force-yes oracle-java8-installer oracle-java8-set-default && \
    rm -rf /var/cache/oracle-jdk8-installer  && \
    apt-get clean  && \
    rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

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
