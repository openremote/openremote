#!/bin/bash
# -----------------------------------------------------------------------------------------------------
# Script for backing up a deployment; by default it uses pg_dump to backup the postgresql-data volume
# to the /deployment.local/backup dir
# -----------------------------------------------------------------------------------------------------
echo "Backing up postgresql-data volume using pg_dump"
docker exec or_postgresql_1 ash -c "pg_dump -Fc openremote > /postgresql.bak"
docker cp or_postgresql_1:/postgresql.bak
