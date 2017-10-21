# OpenRemote v3

[Source](https://github.com/openremote/openremote) **·** [Documentation](https://github.com/openremote/openremote/wiki) **·** [Community](https://groups.google.com/forum/#!forum/openremotecommunity) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](http://openremote.com)

## Getting started with OpenRemote

We are currently working on v3 of the OpenRemote platform. This is **alpha** software that should be used only for development.

If you want to try OpenRemote v2, [read the OpenRemote v2 documentation](https://github.com/openremote/Documentation/wiki).

## Contributing to OpenRemote

We work with Java, Groovy, JavaScript, Gradle, Docker, and a wide range of APIs and protocol implementations. Clone or checkout this project and send us pull requests.

A demo preview can be started with Docker Compose (install [Docker Community Edition](https://www.docker.com/)):

```
docker-compose -p openremote -f profile/demo.yml up
```

Access the manager UI and API on https://localhost/ with username `admin` and password `secret` (accept the 'insecure' self-signed SSL certificate). Configuration options of the images are documented in the compose profiles.

You can build the Docker images from source with:

```
./gradlew clean prepareImage
docker build -t openremote/haproxy:latest haproxy
docker build -t openremote/letsencrypt:latest letsencrypt
docker build -t openremote/postgresql:latest postgresql
docker build -t openremote/keycloak:latest keycloak
docker build -t openremote/manager:latest manager/build/install
```

Push images to [Docker Hub](https://hub.docker.com/u/openremote):

```
docker push openremote/haproxy:latest
docker push openremote/letsencrypt:latest
docker push openremote/postgresql:latest
docker push openremote/keycloak:latest
docker push openremote/manager:latest
```

An instance of the application in developer mode can be started with:

```
./gradlew clean prepareImage
docker-compose -p ordev \
    -f profile/postgresql_dev.yml \
    -f profile/keycloak_dev.yml \
    -f profile/manager_dev.yml \
    -f profile/haproxy_dev.yml \
    up --build
```

Stop the containers with:

```
docker-compose -p ordev \
    -f profile/postgresql_dev.yml \
    -f profile/keycloak_dev.yml \
    -f profile/manager_dev.yml \
    -f profile/haproxy_dev.yml \
    down
```

For more information and how to set up a development environment, see the [Developer Guide](https://github.com/openremote/openremote/wiki).

## Discuss OpenRemote

Join us on the [community group](https://groups.google.com/forum/#!forum/openremotecommunity).

## OpenRemote Projects

* [Agent](https://github.com/openremote/openremote/tree/master/agent) - Connects sensors and actuators to an IoT network and provides intelligence at the edge of the network. Co-locate agents with backend services or install agents on gateways, close to devices.

* [Manager](https://github.com/openremote/openremote/tree/master/manager) - Provides IoT backend services and a web-based operations frontend and management application for agents and domain assets. Design custom data flow, rules, notifications, and build end-user interfaces.

* [Console](https://github.com/openremote/openremote/tree/master/console) - Render and deploy custom end-user interfaces as applications for Web, iOS and Android.

* [HAProxy](https://github.com/openremote/openremote/tree/master/haproxy) - SSL/TLS frontend reverse proxy, terminating SSL connections and forwarding them to the Manager. Also handles Let's Encrypt validation challenges for certificate creation and renewal.

* [Let's Encrypt](https://github.com/openremote/openremote/tree/master/letsencrypt) - Create and automatically renew free SSL certificates in the HAProxy frontend, see https://letsencrypt.org/.

