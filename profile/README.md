# OpenRemote Deployment Profiles

Mix and match these [Docker Compose](https://docs.docker.com/compose/) configurations to customize an OpenRemote system.

## Configuring security

In an OpenRemote system, servers use certificates to authenticate themselves to clients. Simple default TLS certificates (and the private key only known to the server(s)) have been created with:

```
#!/bin/bash

rm /tmp/or-*.jks /tmp/or-*.cer

keytool -genkeypair -alias "openremote" \
    -noprompt -keyalg RSA -validity 9999 -keysize 2048 \
    -dname "CN=openremote" \
    -keypass CHANGE_ME_SSL_KEY_STORE_PASSWORD \
    -keystore /tmp/or-keystore.jks \
    -storepass CHANGE_ME_SSL_KEY_STORE_PASSWORD
    
keytool -exportcert \
    -alias "openremote" \
    -keystore /tmp/or-keystore.jks \
    -storepass CHANGE_ME_SSL_KEY_STORE_PASSWORD \
    -file /tmp/or-certificate.cer
    
keytool -importcert -noprompt \
    -alias "openremote" \
    -file /tmp/or-certificate.cer \
    -keystore /tmp/or-truststore.jks \
    -storepass CHANGE_ME_SSL_KEY_STORE_PASSWORD

keytool -list -v \
    -keystore /tmp/or-keystore.jks \
    -storepass CHANGE_ME_SSL_KEY_STORE_PASSWORD

cp /tmp/or-keystore.jks controller/conf/keystore.jks
cp /tmp/or-truststore.jks controller/conf/truststore.jks

cp /tmp/or-keystore.jks beehive/ccs/conf/keystore.jks

```

TODO: Copy key/trust stores into other projects that need it

At a minimum, you should re-create the stores and private key with your own passwords when deploying OpenRemote.

TODO: Unify default CAs in trust stores in all server systems, which (root) CAs should stay and what is the setup we recommend for OpenRemote users, etc.

TODO: Old Tomcat can't have separate key and store passwords...

## Deploying profiles

```
./gradlew -p beehive/ccs clean war

docker-compose -p <my_project_name> -f profile/mysql.yml -f profile/ccs.yml up
```
