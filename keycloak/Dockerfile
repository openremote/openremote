FROM registry.access.redhat.com/ubi8-minimal
MAINTAINER support@openremote.io

# Add git commit label must be specified at build time using --build-arg GIT_COMMIT=dadadadadad
ARG GIT_COMMIT=unknown
LABEL git-commit=$GIT_COMMIT

ENV KEYCLOAK_VERSION 12.0.1
ENV JDBC_POSTGRES_VERSION 42.2.5
ENV JDBC_MYSQL_VERSION 8.0.22
ENV JDBC_MARIADB_VERSION 2.5.4
ENV JDBC_MSSQL_VERSION 8.2.2.jre11

ENV LAUNCH_JBOSS_IN_BACKGROUND 1
ENV PROXY_ADDRESS_FORWARDING false
ENV JBOSS_HOME /opt/jboss/keycloak
ENV LANG en_US.UTF-8

ARG GIT_REPO
ARG GIT_BRANCH
ARG KEYCLOAK_DIST=https://github.com/keycloak/keycloak/releases/download/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz

USER root

RUN microdnf update -y && microdnf install -y glibc-langpack-en gzip hostname java-11-openjdk-headless openssl tar which && microdnf clean all

ADD tools /opt/jboss/tools
RUN /opt/jboss/tools/build-keycloak.sh

RUN mkdir -p /opt/jboss/keycloak/providers
ADD build/image/themes /opt/jboss/keycloak/themes
ADD build/image/openremote-keycloak.jar /opt/jboss/keycloak/providers
ADD build/image/module.xml /opt/jboss/keycloak/providers

HEALTHCHECK --interval=3s --timeout=3s --start-period=600s --retries=30 CMD curl --fail --silent http://localhost:8080/auth || exit 1

USER 1000

EXPOSE 8080

ENTRYPOINT ["/opt/jboss/tools/docker-entrypoint.sh"]
CMD ["-b", "0.0.0.0"]
