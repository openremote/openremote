# Based on https://github.com/appropriate/docker-postgis/blob/master/9.6-2.5
FROM postgres:9.6.21

ENV POSTGIS_MAJOR 2.3
ENV POSTGRES_DB ${POSTGRES_DB}
ENV POSTGRES_USER ${POSTGRES_USER}
ENV POSTGRES_PASSWORD ${POSTGRES_PASSWORD}
ENV TZ ${TZ}
ENV PGTZ ${PGTZ}

RUN apt-get update \
      && apt-cache showpkg postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR \
      && apt-get install -y --no-install-recommends \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR-scripts \
      && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD gosu postgres pg_isready
