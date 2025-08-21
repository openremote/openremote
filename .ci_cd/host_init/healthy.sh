#!/bin/bash
# -----------------------------------------------------------------------------------------------------
# Script for waiting until all docker services are healthy
#
# An argument can be provided to filter on a specific service name
# -----------------------------------------------------------------------------------------------------
echo "Waiting for up to 5mins for all services to be healthy"
COUNT=1
STATUSES_OK=false
IFS=$'\n'
while [ "$STATUSES_OK" != 'true' ] && [ $COUNT -le 60 ]; do

   echo "Checking service health...attempt $COUNT"
   STATUSES=$(docker ps --format "{{.Names}} {{.Status}}" | grep ${1:-""})
   STATUSES_OK=true

   for STATUS in $STATUSES; do
     if [[ "$STATUS" != *"healthy"* ]]; then
       STATUSES_OK=false
       break
     fi
   done

   if [ "$STATUSES_OK" == 'true' ]; then
      break
   fi

   sleep 5
   COUNT=$((COUNT+1))
done

if [ "$STATUSES_OK" == 'true' ]; then
  echo "All services are healthy"
else
  echo "One or more services are unhealthy"
  docker ps -a
  exit 1
fi
