#!/bin/bash
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER openremote PASSWORD 'secret';
    GRANT ALL PRIVILEGES ON DATABASE openremote TO openremote;
EOSQL
