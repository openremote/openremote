FROM jboss/keycloak:1.9.0.Final

ARG profile=dev

ADD profiles /opt/jboss/keycloak/profiles

RUN INPUT=`cat /opt/jboss/keycloak/standalone/configuration/keycloak-server.json` && echo $INPUT | jq -f /opt/jboss/keycloak/profiles/$profile.jq > /opt/jboss/keycloak/standalone/configuration/keycloak-server.json
