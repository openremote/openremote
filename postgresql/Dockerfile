FROM centos:7

ENV CENTOS_VERSION=7 \
    PG_VERSION=9.6 \
    PG_VERSION_MINOR=3 \
    PG_NODOT=96 \
    POSTGIS_VERSION=23 \
    GOSU_VERSION=1.10

# Use fixed UID/GID
RUN groupadd -r postgres --gid=5432 && useradd -N -r -g postgres --uid=5432 postgres

# Install from Postgres website
RUN cd /tmp && \
    yum install -y epel-release && \
    curl -L -o pgdg.rpm https://yum.postgresql.org/${PG_VERSION}/redhat/rhel-${CENTOS_VERSION}-x86_64/pgdg-redhat${PG_NODOT}-${PG_VERSION}-${PG_VERSION_MINOR}.noarch.rpm && \
    yum localinstall -y pgdg.rpm && \
    yum upgrade -y && \
    yum install -y \
        postgis${POSTGIS_VERSION}_${PG_NODOT} \
        postgis${POSTGIS_VERSION}_${PG_NODOT}-client \
        postgis${POSTGIS_VERSION}_${PG_NODOT}-utils \
        pgrouting_${PG_NODOT} \
        postgresql${PG_NODOT}-contrib \
        postgresql${PG_NODOT}-server

# Need gosu
RUN curl -L -o /usr/local/bin/gosu https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-amd64 \
	&& chmod +x /usr/local/bin/gosu \
	&& gosu nobody true

# Prepare data directory
ENV PGDATA /data
RUN mkdir -p "$PGDATA" && chown -R postgres:postgres "$PGDATA"
VOLUME ${PGDATA}

# Some defaults
ENV PG_BIN=/usr/pgsql-${PG_VERSION}/bin
ENV PATH $PATH:$PG_BIN
ENV LANG en_US.utf8

EXPOSE 5432

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD gosu postgres pg_isready

COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
