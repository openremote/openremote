FROM jboss/keycloak:7.0.1
MAINTAINER support@openremote.io

# Add git commit label must be specified at build time using --build-arg GIT_COMMIT=dadadadadad
ARG GIT_COMMIT=unknown
LABEL git-commit=$GIT_COMMIT

USER root
ADD docker-entrypoint.sh /opt/jboss/
RUN chmod +x /opt/jboss/docker-entrypoint.sh

HEALTHCHECK --interval=3s --timeout=3s --start-period=600s --retries=30 CMD curl --fail --silent http://localhost:8080/auth || exit 1

USER 1000

EXPOSE 8080

ENTRYPOINT ["/opt/jboss/docker-entrypoint.sh"]
CMD ["-b", "0.0.0.0"]