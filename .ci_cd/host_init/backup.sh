#!/bin/bash
# -----------------------------------------------------------------------------------------------------
# Script for backing up a deployment; by default it uses pg_dump to backup the postgresql-data volume
# to the /deployment.local/backup dir
# -----------------------------------------------------------------------------------------------------
echo "Backing up postgresql-data volume using pg_dump"
mkdir -p /deployment.local/backup &>/dev/null
docker exec or-postgresql-1 pg_dump -Fc openremote -f /tmp/or_postgresql-data.bak
docker cp or-postgresql-1:/tmp/or_postgresql-data.bak /deployment.local/backup
