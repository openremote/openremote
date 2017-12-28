FROM debian:stretch
MAINTAINER support@openremote.io

# Install curl, wget, ca-certificates, procps
RUN apt-get update && apt-get install -y --no-install-recommends \
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

ENV JAVA_VERSION 8u151
ENV JAVA_DEBIAN_VERSION 8u151-b12-1~deb9u1

# see https://bugs.debian.org/775775
# and https://github.com/docker-library/java/issues/19#issuecomment-70546872
ENV CA_CERTIFICATES_JAVA_VERSION 20170531+nmu1

RUN set -ex; \
	\
# deal with slim variants not having man page directories (which causes "update-alternatives" to fail)
	if [ ! -d /usr/share/man/man1 ]; then \
		mkdir -p /usr/share/man/man1; \
	fi; \
	\
	apt-get update; \
	apt-get install -y \
		openjdk-8-jdk="$JAVA_DEBIAN_VERSION" \
		ca-certificates-java="$CA_CERTIFICATES_JAVA_VERSION" \
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

# see CA_CERTIFICATES_JAVA_VERSION notes above
RUN /var/lib/dpkg/info/ca-certificates-java.postinst configure

# Install dependencies for JBoss AS
RUN apt-get update \
      && apt-get install -y --no-install-recommends \
            jq xmlstarlet libsaxon-java unzip bsdtar bzip2 xz-utils \
      && rm -rf /var/lib/apt/lists/*

# Create a user and group used to launch processes
# The user ID 1000 is the default for the first "regular" user on Fedora/RHEL,
# so there is a high chance that this ID will be equal to the current user
# making it easier to use volumes (no permission issues)
RUN groupadd -r jboss -g 1000 && useradd -u 1000 -r -g jboss -m -d /opt/jboss -s /sbin/nologin -c "JBoss user" jboss && \
    chmod 755 /opt/jboss

# Set the working directory to jboss' user home directory
WORKDIR /opt/jboss
ADD docker-entrypoint.sh /opt/jboss/
RUN chmod +x /opt/jboss/docker-entrypoint.sh
ENV JBOSS_HOME /opt/jboss/keycloak

# Switch to jboss user
USER jboss

ENV KEYCLOAK_VERSION 2.5.5.Final
ENV POSTGRESQL_DRIVER_VERSION 42.1.4

# Enables signals getting passed from startup script to JVM
# ensuring clean shutdown when container is stopped.
ENV LAUNCH_JBOSS_IN_BACKGROUND 1

RUN curl -L https://downloads.jboss.org/keycloak/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz | \
    tar zx && mv /opt/jboss/keycloak-$KEYCLOAK_VERSION $JBOSS_HOME

ADD setLogLevel.xsl /opt/jboss/keycloak/
RUN java -jar /usr/share/java/saxon.jar \
    -o /opt/jboss/keycloak/standalone/configuration/standalone.xml \
    /opt/jboss/keycloak/standalone/configuration/standalone.xml \
    /opt/jboss/keycloak/setLogLevel.xsl

ADD changeDatabase.xsl /opt/jboss/keycloak/
RUN java -jar /usr/share/java/saxon.jar \
    -o /opt/jboss/keycloak/standalone/configuration/standalone.xml \
    /opt/jboss/keycloak/standalone/configuration/standalone.xml \
    /opt/jboss/keycloak/changeDatabase.xsl && \
    java -jar /usr/share/java/saxon.jar \
    -o /opt/jboss/keycloak/standalone/configuration/standalone-ha.xml \
    /opt/jboss/keycloak/standalone/configuration/standalone-ha.xml \
    /opt/jboss/keycloak/changeDatabase.xsl && \
    rm /opt/jboss/keycloak/changeDatabase.xsl
RUN mkdir -p /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main && \
    curl -o /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main/postgresql-jdbc.jar \
    http://repo1.maven.org/maven2/org/postgresql/postgresql/${POSTGRESQL_DRIVER_VERSION}/postgresql-${POSTGRESQL_DRIVER_VERSION}.jar 2>/dev/null
ADD module.xml /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main/

ADD setProxyForwarding.xsl /opt/jboss/keycloak/
RUN java -jar /usr/share/java/saxon.jar \
    -o /opt/jboss/keycloak/standalone/configuration/standalone.xml \
    /opt/jboss/keycloak/standalone/configuration/standalone.xml \
    /opt/jboss/keycloak/setProxyForwarding.xsl

RUN rm /opt/jboss/keycloak/*.xsl

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD curl --fail http://localhost:8080/auth || exit 1

EXPOSE 8080

ENTRYPOINT ["/opt/jboss/docker-entrypoint.sh"]
CMD ["-b", "0.0.0.0"]

ADD theme /opt/jboss/keycloak/themes/openremote
