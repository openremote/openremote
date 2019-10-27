# Default deployment when delpoyment-data volume is empty
FROM debian:stretch

# Add git commit label must be specified at build time using --build-arg GIT_COMMIT=dadadadadad
ARG GIT_COMMIT=unknown
LABEL git-commit=$GIT_COMMIT

RUN mkdir -p /deployment/extensions
ADD build /deployment