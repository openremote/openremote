#!/bin/bash

# Symlink each theme from deployment this preserves the default keycloak theme, reasons for this mechanism:
# Cannot volume map a subdir of a named volume
# Want all customisation to be in the /deployment dir and deployment-data volume

# Remove any stale symlinks
for dir in /opt/jboss/keycloak/themes/*; do
  if [ -L ${dir} ]; then
    rm ${dir}
  fi
done

# Link standard themes
if [ -d "/deployment/keycloak/themes" ]; then
    for dir in /deployment/keycloak/themes/*; do
        if [ -d ${dir} ]; then
            echo "${dir##*/}"
            ln -s ${dir} /opt/jboss/keycloak/themes/${dir##*/}
        fi
    done
fi
# Additional symlinks for custom themes during dev
if [ -d "/deployment/keycloak/customthemes" ]; then
    for dir in /deployment/keycloak/customthemes/*; do
        if [ -d ${dir} ]; then
            echo "${dir##*/}"
            ln -s ${dir} /opt/jboss/keycloak/themes/${dir##*/}
        fi
    done
fi


exec /opt/jboss/tools/docker-entrypoint.sh $@
exit $?
