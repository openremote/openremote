#!/bin/bash -e

PG_VERSION=9.6 \
    PG_VERSION_INSTALL=9.6* \
    POSTGIS_VERSION=2.3 \
    GOSU_VERSION=1.10

PGDATA=/data/postgresql

DB_NAME=${POSTGRES_DB:-}
DB_USER=${POSTGRES_USER:-}
DB_PASS=${POSTGRES_PASSWORD:-}

PG_BIN=/usr/lib/postgresql/${PG_VERSION}/bin
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "Installing packages"
apt-get update \
    && apt-get install -y curl vim openjdk-8-jdk-headless \
	&& apt-get install -y --no-install-recommends \
	    postgresql-common \
		postgresql=$PG_VERSION_INSTALL \
		postgresql-contrib=$PG_VERSION_INSTALL \
        postgresql-$PG_VERSION-postgis-$POSTGIS_VERSION \
        postgresql-$PG_VERSION-postgis-$POSTGIS_VERSION-scripts \
    && rm -rf /var/lib/apt/lists/*

curl -L -o /usr/local/bin/gosu https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-arm64 \
	&& chmod +x /usr/local/bin/gosu \
	&& gosu nobody true

echo "Moving PostgreSQL data directory to $PGDATA"
systemctl stop postgresql
mkdir -p /data
sed -i "s#^data_directory.*#data_directory = \'$PGDATA\'#g" /etc/postgresql/9.6/main/postgresql.conf
mv /var/lib/postgresql/9.6/main ${PGDATA}
systemctl start postgresql

# If POSTGRES_USER is set, create a superuser with POSTGRES_PASSWORD
if [ -n "${DB_USER}" ]; then
  if [ -z "${DB_PASS}" ]; then
    echo "No password specified for user: \"${DB_USER}\""
    exit 1
  fi
    echo "Creating user \"${DB_USER}\"..."
    gosu postgres psql -c "CREATE ROLE ${DB_USER} with superuser login PASSWORD '${DB_PASS}';"
fi

# If POSTGRES_DB is set and not 'postgres', create the database and give POSTGRES_USER full access
if [ -n "${DB_NAME}" ] && [ "${DB_NAME}" != 'postgres' ]; then
  echo "Creating database \"${DB_NAME}\"..."
  gosu postgres psql -c "CREATE DATABASE ${DB_NAME};"

  if [ -n "${DB_USER}" ]; then
    echo "Granting access to database \"${DB_NAME}\" for user \"${DB_USER}\"..."
    gosu postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} to ${DB_USER};"
  fi
fi
