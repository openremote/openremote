FROM debian:stretch
MAINTAINER support@openremote.io

# Install utilities
RUN apt-get update && apt-get install -y --no-install-recommends \
        apt-transport-https \
        gnupg2 \
        software-properties-common \
        procps \
		ca-certificates \
		curl \
		wget \
	&& rm -rf /var/lib/apt/lists/*

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

# add a simple script that can auto-detect the appropriate JAVA_HOME value
# based on whether the JDK or only the JRE is installed
RUN { \
		echo '#!/bin/sh'; \
		echo 'set -e'; \
		echo; \
		echo 'dirname "$(dirname "$(readlink -f "$(which javac || which java)")")"'; \
	} > /usr/local/bin/docker-java-home \
	&& chmod +x /usr/local/bin/docker-java-home

# do some fancy footwork to create a JAVA_HOME that's cross-architecture-safe
RUN ln -svT "/usr/lib/jvm/java-8-openjdk-$(dpkg --print-architecture)" /docker-java-home
ENV JAVA_HOME /docker-java-home

RUN set -ex; \
	\
# deal with slim variants not having man page directories (which causes "update-alternatives" to fail)
	if [ ! -d /usr/share/man/man1 ]; then \
		mkdir -p /usr/share/man/man1; \
	fi; \
	\
	apt-get update; \
	apt-get install -y \
		openjdk-8-jdk \
		ca-certificates-java\
	; \
	rm -rf /var/lib/apt/lists/*; \
	\
# verify that "docker-java-home" returns what we expect
	[ "$(readlink -f "$JAVA_HOME")" = "$(docker-java-home)" ]; \
	\
# update-alternatives so that future installs of other OpenJDK versions don't change /usr/bin/java
	update-alternatives --get-selections | awk -v home="$(readlink -f "$JAVA_HOME")" 'index($3, home) == 1 { $2 = "manual"; print | "update-alternatives --set-selections" }'; \
# ... and verify that it actually worked for one of the alternatives we care about
	update-alternatives --query java | grep -q 'Status: manual'

# Run postinst because it might not happen on install
RUN /var/lib/dpkg/info/ca-certificates-java.postinst configure

# Add git commit label must be specified at build time using --build-arg GIT_COMMIT=dadadadadad
ARG GIT_COMMIT=unknown
LABEL git-commit=$GIT_COMMIT

############ EDITS ABOVE THIS LINE SHOULD BE DONE IN ALL DOCKERFILES! ################

ENV JAVA_OPTS -Xmx1g

ENV APP_DOCROOT /deployment/manager/app
ENV UI_DOCROOT /deployment/manager/ui
ENV SHARED_DOCROOT /deployment/manager/shared
ENV LOGGING_CONFIG_FILE /deployment/manager/logging.properties
ENV MAP_TILES_PATH /deployment/map/mapdata.mbtiles
ENV MAP_SETTINGS_PATH /deployment/map/mapsettings.json

EXPOSE 8080

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=120 CMD curl --fail --silent http://localhost:8080 || exit 1

WORKDIR /opt/app

RUN mkdir -p /deployment/extensions

ADD lib /opt/app/lib

ENTRYPOINT java $JAVA_OPTS -cp /opt/app/lib/*:/deployment/manager/extensions/* org.openremote.manager.Main
