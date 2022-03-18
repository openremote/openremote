#!/bin/bash
# -----------------------------------------------------------------------------------------------------
# This is called after existing docker compose stack is removed and CLEAN_INSTALL=true
# By default it just removes the postgresql-data volume
# Override in custom projects to perform different cleanup tasks
# -----------------------------------------------------------------------------------------------------
echo "Deleting existing postgres data volume"
docker volume rm or_postgresql-data 2> /dev/null
