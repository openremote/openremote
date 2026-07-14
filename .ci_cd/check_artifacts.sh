#!/bin/bash
set -eo pipefail

RELEASED_VERSION="${1:?Usage: $0 <released_version>}"

echo "Check if OpenRemote artifacts '$RELEASED_VERSION' are available."
STATUS=$(curl --head --silent https://repo1.maven.org/maven2/io/openremote/openremote-manager/$RELEASED_VERSION/openremote-manager-$RELEASED_VERSION.jar --output /dev/null --write-out "%{http_code}")
COUNT=0

while [ "$STATUS" != 200 ] && [ $COUNT -lt 30 ]; do
  echo "Artifacts not yet available ... Sleeping 60 seconds"
  sleep 60
  STATUS=$(curl --head --silent https://repo1.maven.org/maven2/io/openremote/openremote-manager/$RELEASED_VERSION/openremote-manager-$RELEASED_VERSION.jar --output /dev/null --write-out "%{http_code}")
  COUNT=$((COUNT+1))
done

if [ "$STATUS" != '200' ]; then
  echo "Cannot retrieve artifacts"
  exit 1
fi

echo "Artifacts available!"
