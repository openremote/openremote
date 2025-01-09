#FROM ghcr.io/zalando/spilo-16:3.3-p3
FROM spilo-test:latest

# Increment this to indicate that a re-index should be carried out on first startup with existing data; REINDEX can still be overidden
# with OR_DISABLE_REINDEX=true
ARG OR_REINDEX_COUNTER=1

ENV LC_ALL=C.UTF-8 \
    LANG=C.UTF-8 \
    # When having an interactive psql session, it is useful if the PAGER is disable
    PAGER="" \
    # OR ENV DEFAULTS

    TZ=${TZ:-Europe/Amsterdam} \
    PGTZ=${PGTZ:-Europe/Amsterdam} \
    POSTGRES_DB=${POSTGRES_DB:-openremote} \
    POSTGRES_USER=${POSTGRES_USER:-postgres} \
    POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-postgres} \
    PG_MAJOR=$PG_MAJOR \
    OR_REINDEX_COUNTER=${OR_REINDEX_COUNTER} \
    OR_DISABLE_REINDEX=${OR_DISABLE_REINDEX:-false} \
    POSTGRES_MAX_CONNECTIONS=${POSTGRES_MAX_CONNECTIONS:-50} \
    OR_DISABLE_AUTO_UPGRADE=${OR_DISABLE_AUTO_UPGRADE:-false}
