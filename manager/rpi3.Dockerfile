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
ENV JAVA_OPTS -Xmx500m

RUN [ "cross-build-end" ]

ENV APP_DOCROOT /deployment/manager/app
ENV UI_DOCROOT /deployment/manager/ui
ENV LOGGING_CONFIG_FILE /deployment/manager/logging.properties
ENV MAP_TILES_PATH /deployment/manager/map/mapdata.mbtiles
ENV MAP_SETTINGS_PATH /deployment/manager/map/mapsettings.json

EXPOSE 8080

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD curl --fail http://localhost:8080 || exit 1

WORKDIR /opt/app

ADD lib /opt/app/lib
ADD client /opt/app

ENTRYPOINT java $JAVA_OPTS -cp /opt/app/lib/*:/deployment/extensions/* org.openremote.manager.Main
