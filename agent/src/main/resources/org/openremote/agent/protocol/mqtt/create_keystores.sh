#!/bin/bash

# Check if the password is provided as an argument
if [ -z "$1" ]; then
  echo "Usage: $0 <password>"
  exit 1
fi

PASSWORD=$1
KEYSTORE_FILENAME="keystore.jks"
TRUSTSTORE_FILENAME="truststore.jks"
ALIAS="myalias"

# Generate a keystore
keytool -genkeypair -alias $ALIAS -keyalg RSA -keysize 2048 -validity 365 -keystore $KEYSTORE_FILENAME -storepass $PASSWORD -keypass $PASSWORD -dname "CN=example.com, OU=Example, O=Example Inc, L=City, ST=State, C=US"

# Export the certificate from the keystore
keytool -export -alias $ALIAS -file ${ALIAS}.crt -keystore $KEYSTORE_FILENAME -storepass $PASSWORD

# Create a truststore and import the certificate
keytool -import -alias $ALIAS -file ${ALIAS}.crt -keystore $TRUSTSTORE_FILENAME -storepass $PASSWORD -noprompt

# Clean up the exported certificate file
rm -f ${ALIAS}.crt

echo "Keystore and Truststore have been created with the password: $PASSWORD"

