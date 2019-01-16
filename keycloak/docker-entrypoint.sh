#!/bin/bash

# Copy themes from deployment if they exist
if [ -d "/deployment/keycloak/themes" ]; then
  cp -r /deployment/keycloak/themes/* /opt/jboss/keycloak/themes/
fi

if [ $SETUP_ADMIN_PASSWORD ]; then
    keycloak/bin/add-user-keycloak.sh --user admin --password $SETUP_ADMIN_PASSWORD
fi

exec /opt/jboss/keycloak/bin/standalone.sh $@
exit $?