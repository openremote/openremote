#!/bin/bash

if [ $SETUP_ADMIN_PASSWORD ]; then
    keycloak/bin/add-user-keycloak.sh --user admin --password $SETUP_ADMIN_PASSWORD
fi

exec /opt/jboss/keycloak/bin/standalone.sh $@
exit $?