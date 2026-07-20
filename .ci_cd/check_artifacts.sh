#!/bin/bash
set -eo pipefail

RELEASED_VERSION="${1:?Usage: $0 <released_version> <true or false>}"
IS_RELEASE="${2:?Usage: $0 <released_version> <true or false>}"

if [ "$IS_RELEASE" == true ]; then
  echo "Check if OpenRemote artifacts '$RELEASED_VERSION' are available."
  STATUS=$(curl --head --silent https://repo1.maven.org/maven2/io/openremote/openremote-manager/$RELEASED_VERSION/openremote-manager-$RELEASED_VERSION.jar --output /dev/null --write-out "%{http_code}")
  COUNT=0

  while [ "$STATUS" != 200 ] && [ "$COUNT" -lt 30 ]; do
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
fi

if [ "$IS_RELEASE" == false ]; then
  echo "Check if OpenRemote SNAPSHOT artifacts '$RELEASED_VERSION' are available."
  VERSION=$(curl -s "https://central.sonatype.com/repository/maven-snapshots/io/openremote/openremote-manager/maven-metadata.xml" | sed -n 's:.*<latest>\(.*\)</latest>.*:\1:p')
  COUNT=0

  while [ "$VERSION" != "$RELEASED_VERSION" ] && [ "$COUNT" -lt 30 ]; do
    echo "SNAPSHOT artifacts not yet available ... Sleeping 60 seconds"
    sleep 60
    VERSION=$(curl -s "https://central.sonatype.com/repository/maven-snapshots/io/openremote/openremote-manager/maven-metadata.xml" | sed -n 's:.*<latest>\(.*\)</latest>.*:\1:p')
    COUNT=$((COUNT+1))
  done

  if [ "$VERSION" != "$RELEASED_VERSION" ]; then
    echo "Cannot retrieve SNAPSHOT artifacts"
    exit 1
  fi

  echo "SNAPSHOT artifacts available!"
fi

