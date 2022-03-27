#!/bin/bash
# -----------------------------------------------------------------------------------------------------
# Script for cleaning up an or deployment persistent files
# -----------------------------------------------------------------------------------------------------
echo "Deleting existing postgres data volume"
docker volume rm or_postgresql-data 2> /dev/null
echo "Deleting existing temp data volume"
docker volume rm or_temp-data 2> /dev/null
