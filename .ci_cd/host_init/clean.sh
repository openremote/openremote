#!/bin/bash
# -----------------------------------------------------------------------------------------------------
# Script for cleaning up an or deployment persistent files
# -----------------------------------------------------------------------------------------------------
echo "Deleting existing postgres data volume"
docker volume rm or_postgresql-data 2> /dev/null
echo "Deleting existing manager data volume"
docker volume rm or_manager-data 2> /dev/null
