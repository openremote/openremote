FROM openremote/rpi-raspbian:latest

ENV PG_VERSION=9.6 \
    PG_VERSION_INSTALL=9.6* \
    POSTGIS_VERSION=2.3 \
    GOSU_VERSION=1.10

RUN [ "cross-build-start" ]

# Use fixed UID/GID
RUN groupadd -r postgres --gid=5432 && useradd -N -r -g postgres --uid=5432 postgres

# Install from repo
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-common \
	&& sed -ri 's/#(create_main_cluster) .*$/\1 = false/' /etc/postgresql-common/createcluster.conf \
	&& apt-get install -y --no-install-recommends \
		postgresql=$PG_VERSION_INSTALL \
		postgresql-contrib=$PG_VERSION_INSTALL \
        postgresql-$PG_VERSION-postgis-$POSTGIS_VERSION \
        postgresql-$PG_VERSION-postgis-$POSTGIS_VERSION-scripts \
    && rm -rf /var/lib/apt/lists/*

# Need gosu
RUN curl -L -o /usr/local/bin/gosu https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-armhf \
	&& chmod +x /usr/local/bin/gosu \
	&& gosu nobody true

RUN [ "cross-build-end" ]

# Prepare data directory
ENV PGDATA /data
RUN mkdir -p "$PGDATA" && chown -R postgres:postgres "$PGDATA"
VOLUME ${PGDATA}

# Some defaults
ENV PG_BIN=/usr/lib/postgresql/${PG_VERSION}/bin
ENV PATH $PATH:$PG_BIN

EXPOSE 5432

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD gosu postgres pg_isready

COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
