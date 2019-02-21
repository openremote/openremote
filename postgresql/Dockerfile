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

ENV PG_VERSION=9.6 \
    POSTGIS_VERSION=2.3 \
    GOSU_VERSION=1.10

# Use fixed UID/GID
RUN groupadd -r postgres --gid=5432 && useradd -N -r -g postgres --uid=5432 postgres

# Install postgresql/postgis
RUN apt-get update \
      && apt-get install -y --no-install-recommends \
            locales \
            postgresql-$PG_VERSION-postgis-$POSTGIS_VERSION \
            postgresql-$PG_VERSION-postgis-$POSTGIS_VERSION-scripts \
      && rm -rf /var/lib/apt/lists/*

# Need gosu
RUN curl -L -o /usr/local/bin/gosu https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64 \
	&& chmod +x /usr/local/bin/gosu \
	&& gosu nobody true

# Enable UTF8
RUN localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
ENV LANG en_US.utf8

# Prepare data directory
ENV PGDATA /deployment
RUN mkdir -p "$PGDATA" && chown -R postgres:postgres "$PGDATA"

# Some defaults
ENV PG_BIN=/usr/lib/postgresql/${PG_VERSION}/bin
ENV PATH $PATH:$PG_BIN

EXPOSE 5432

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD gosu postgres pg_isready

COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
