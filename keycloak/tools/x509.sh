#!/bin/bash

function autogenerate_keystores() {
  # Keystore infix notation as used in templates to keystore name mapping
  declare -A KEYSTORES=( ["https"]="HTTPS" )

  local KEYSTORES_STORAGE="${JBOSS_HOME}/standalone/configuration/keystores"
  if [ ! -d "${KEYSTORES_STORAGE}" ]; then
    mkdir -p "${KEYSTORES_STORAGE}"
  fi

  # Auto-generate the HTTPS keystore if volumes for OpenShift's
  # serving x509 certificate secrets service were properly mounted
  for KEYSTORE_TYPE in "${!KEYSTORES[@]}"; do

    local X509_KEYSTORE_DIR="/etc/x509/${KEYSTORE_TYPE}"
    local X509_CRT="tls.crt"
    local X509_KEY="tls.key"
    local NAME="keycloak-${KEYSTORE_TYPE}-key"
    local PASSWORD=$(openssl rand -base64 32 2>/dev/null)
    local JKS_KEYSTORE_FILE="${KEYSTORE_TYPE}-keystore.jks"
    local PKCS12_KEYSTORE_FILE="${KEYSTORE_TYPE}-keystore.pk12"

    if [ -f "${X509_KEYSTORE_DIR}/${X509_KEY}" ] && [ -f "${X509_KEYSTORE_DIR}/${X509_CRT}" ]; then

      echo "Creating ${KEYSTORES[$KEYSTORE_TYPE]} keystore via OpenShift's service serving x509 certificate secrets.."

      openssl pkcs12 -export \
      -name "${NAME}" \
      -inkey "${X509_KEYSTORE_DIR}/${X509_KEY}" \
      -in "${X509_KEYSTORE_DIR}/${X509_CRT}" \
      -out "${KEYSTORES_STORAGE}/${PKCS12_KEYSTORE_FILE}" \
      -password pass:"${PASSWORD}" >& /dev/null

      keytool -importkeystore -noprompt \
      -srcalias "${NAME}" -destalias "${NAME}" \
      -srckeystore "${KEYSTORES_STORAGE}/${PKCS12_KEYSTORE_FILE}" \
      -srcstoretype pkcs12 \
      -destkeystore "${KEYSTORES_STORAGE}/${JKS_KEYSTORE_FILE}" \
      -storepass "${PASSWORD}" -srcstorepass "${PASSWORD}" >& /dev/null

      if [ -f "${KEYSTORES_STORAGE}/${JKS_KEYSTORE_FILE}" ]; then
        echo "${KEYSTORES[$KEYSTORE_TYPE]} keystore successfully created at: ${KEYSTORES_STORAGE}/${JKS_KEYSTORE_FILE}"
      else
        echo "${KEYSTORES[$KEYSTORE_TYPE]} keystore not created at: ${KEYSTORES_STORAGE}/${JKS_KEYSTORE_FILE} (check permissions?)"
      fi

      echo "set keycloak_tls_keystore_password=${PASSWORD}" >> "$JBOSS_HOME/bin/.jbossclirc"
      echo "set keycloak_tls_keystore_file=${KEYSTORES_STORAGE}/${JKS_KEYSTORE_FILE}" >> "$JBOSS_HOME/bin/.jbossclirc"
      echo "set configuration_file=standalone.xml" >> "$JBOSS_HOME/bin/.jbossclirc"
      $JBOSS_HOME/bin/jboss-cli.sh --file=/opt/jboss/tools/cli/x509-keystore.cli >& /dev/null
      sed -i '$ d' "$JBOSS_HOME/bin/.jbossclirc"
      echo "set configuration_file=standalone-ha.xml" >> "$JBOSS_HOME/bin/.jbossclirc"
      $JBOSS_HOME/bin/jboss-cli.sh --file=/opt/jboss/tools/cli/x509-keystore.cli >& /dev/null
      sed -i '$ d' "$JBOSS_HOME/bin/.jbossclirc"
    fi

  done

  # Auto-generate the Keycloak truststore if X509_CA_BUNDLE was provided
  local -r X509_CRT_DELIMITER="/-----BEGIN CERTIFICATE-----/"
  local JKS_TRUSTSTORE_FILE="truststore.jks"
  local JKS_TRUSTSTORE_PATH="${KEYSTORES_STORAGE}/${JKS_TRUSTSTORE_FILE}"
  local PASSWORD=$(openssl rand -base64 32 2>/dev/null)
  local TEMPORARY_CERTIFICATE="temporary_ca.crt"
  if [ -n "${X509_CA_BUNDLE}" ]; then
    pushd /tmp >& /dev/null
    echo "Creating Keycloak truststore.."
    # We use cat here, so that users could specify multiple CA Bundles using space or even wildcard:
    # X509_CA_BUNDLE=/var/run/secrets/kubernetes.io/serviceaccount/*.crt
    # Note, that there is no quotes here, that's intentional. Once can use spaces in the $X509_CA_BUNDLE like this:
    # X509_CA_BUNDLE=/ca.crt /ca2.crt
    cat ${X509_CA_BUNDLE} > ${TEMPORARY_CERTIFICATE}
    csplit -s -z -f crt- "${TEMPORARY_CERTIFICATE}" "${X509_CRT_DELIMITER}" '{*}'
    for CERT_FILE in crt-*; do
      keytool -import -noprompt -keystore "${JKS_TRUSTSTORE_PATH}" -file "${CERT_FILE}" \
      -storepass "${PASSWORD}" -alias "service-${CERT_FILE}" >& /dev/null
    done

    if [ -f "${JKS_TRUSTSTORE_PATH}" ]; then
      echo "Keycloak truststore successfully created at: ${JKS_TRUSTSTORE_PATH}"
    else
      echo "Keycloak truststore not created at: ${JKS_TRUSTSTORE_PATH}"
    fi

    # Import existing system CA certificates into the newly generated truststore
    local SYSTEM_CACERTS=$(readlink -e $(dirname $(readlink -e $(which keytool)))"/../lib/security/cacerts")
    if keytool -v -list -keystore "${SYSTEM_CACERTS}" -storepass "changeit" > /dev/null; then
      echo "Importing certificates from system's Java CA certificate bundle into Keycloak truststore.."
      keytool -importkeystore -noprompt \
      -srckeystore "${SYSTEM_CACERTS}" \
      -destkeystore "${JKS_TRUSTSTORE_PATH}" \
      -srcstoretype jks -deststoretype jks \
      -storepass "${PASSWORD}" -srcstorepass "changeit" >& /dev/null
      if [ "$?" -eq "0" ]; then
        echo "Successfully imported certificates from system's Java CA certificate bundle into Keycloak truststore at: ${JKS_TRUSTSTORE_PATH}"
      else
        echo "Failed to import certificates from system's Java CA certificate bundle into Keycloak truststore!"
      fi
    fi

    echo "set keycloak_tls_truststore_password=${PASSWORD}" >> "$JBOSS_HOME/bin/.jbossclirc"
    echo "set keycloak_tls_truststore_file=${KEYSTORES_STORAGE}/${JKS_TRUSTSTORE_FILE}" >> "$JBOSS_HOME/bin/.jbossclirc"
    echo "set configuration_file=standalone.xml" >> "$JBOSS_HOME/bin/.jbossclirc"
    $JBOSS_HOME/bin/jboss-cli.sh --file=/opt/jboss/tools/cli/x509-truststore.cli >& /dev/null
    sed -i '$ d' "$JBOSS_HOME/bin/.jbossclirc"
    echo "set configuration_file=standalone-ha.xml" >> "$JBOSS_HOME/bin/.jbossclirc"
    $JBOSS_HOME/bin/jboss-cli.sh --file=/opt/jboss/tools/cli/x509-truststore.cli >& /dev/null
    sed -i '$ d' "$JBOSS_HOME/bin/.jbossclirc"

    popd >& /dev/null
  fi
}

autogenerate_keystores
