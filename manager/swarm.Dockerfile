# hadolint ignore=DL3007
FROM openremote/deployment:latest AS deployment
# hadolint ignore=DL3007
FROM openremote/manager:latest

# Add git commit label must be specified at build time using --build-arg GIT_COMMIT=dadadadadad
ARG GIT_COMMIT=unknown
LABEL git-commit=$GIT_COMMIT

COPY --from=deployment /deployment/ /deployment/
# hadolint ignore=DL3025
