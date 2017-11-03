#!/bin/bash
set -e

DB_NAME=${POSTGRES_DB:-}
DB_USER=${POSTGRES_USER:-}
DB_PASS=${POSTGRES_PASSWORD:-}

EXECUTE_SINGLE_COMMAND="gosu postgres ${PG_BIN}/postgres --single -c config_file=${PGDATA}/postgresql.conf -D ${PGDATA}"

# If there is no config file, create config, database, user
if ! [ -f ${PGDATA}/postgresql.conf ]; then
    echo "Init database directory (mount a volume on your host to keep data): ${PGDATA}"

    # Create initial database files
    gosu postgres ${PG_BIN}/initdb --auth='ident' -E UTF8
    chown -R postgres:postgres ${PGDATA}

    # Trust all local connections, allow authenticated IPv4 remote connections
    echo "local all all trust" > ${PGDATA}/pg_hba.conf
    echo "host all all 127.0.0.1/32 trust" >> ${PGDATA}/pg_hba.conf
    echo "host all all 0.0.0.0/0 md5" >> ${PGDATA}/pg_hba.conf
    echo "listen_addresses = '0.0.0.0'" >> ${PGDATA}/postgresql.conf

    # Log to stderr
    sed -i 's/^logging_collector = on/logging_collector = off/g' ${PGDATA}/postgresql.conf

    # If POSTGRES_USER is set, create a superuser with POSTGRES_PASSWORD
    if [ -n "${DB_USER}" ]; then
      if [ -z "${DB_PASS}" ]; then
        echo "No password specified for user: \"${DB_USER}\""
        exit 1
      fi
        echo "Creating user \"${DB_USER}\"..."
        echo "CREATE ROLE ${DB_USER} with superuser login PASSWORD '${DB_PASS}';" | ${EXECUTE_SINGLE_COMMAND} >/dev/null
    fi

    # If POSTGRES_DB is set and not 'postgres', create the database and give POSTGRES_USER full access
    if [ -n "${DB_NAME}" ] && [ "${DB_NAME}" != 'postgres' ]; then
      echo "Creating database \"${DB_NAME}\"..."
      echo "CREATE DATABASE ${DB_NAME};" | ${EXECUTE_SINGLE_COMMAND} >/dev/null

      if [ -n "${DB_USER}" ]; then
        echo "Granting access to database \"${DB_NAME}\" for user \"${DB_USER}\"..."
        echo "GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} to ${DB_USER};" | ${EXECUTE_SINGLE_COMMAND} >/dev/null
      fi
    fi
else
    echo "Using existing database(s) in: ${PGDATA}"
fi

echo "Starting database server..."
gosu postgres ${PG_BIN}/pg_ctl start -w "$@"
/bin/bash -c "trap : TERM INT; sleep infinity & wait"