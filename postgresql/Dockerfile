# Based on https://github.com/appropriate/docker-postgis/blob/master/9.6-2.5
FROM postgres:9.6.21

ENV POSTGIS_MAJOR 2.3
ENV TZ ${TZ:-Europe/Amsterdam}
ENV PGTZ ${PGTZ:-Europe/Amsterdam}
ENV POSTGRES_DB ${POSTGRES_DB:-openremote}
ENV POSTGRES_USER ${POSTGRES_USER:-postgres}
ENV POSTGRES_PASSWORD ${POSTGRES_PASSWORD:-postgres}

RUN apt-get update \
      && apt-cache showpkg postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR \
      && apt-get install -y --no-install-recommends \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR-scripts \
      && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD gosu postgres pg_isready
