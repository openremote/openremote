# OpenRemote v3

[Source](https://github.com/openremote/openremote) **·** [Documentation](https://github.com/openremote/openremote/wiki) **·** [Community](https://groups.google.com/forum/#!forum/openremotecommunity) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](http://openremote.com)

## Getting started with OpenRemote

We are currently working on v3 of the OpenRemote platform. This is **alpha** software that should be used only for development.

If you want to try OpenRemote v2, [read the OpenRemote v2 documentation](https://github.com/openremote/Documentation/wiki).

## Contributing to OpenRemote

We work with Java, Groovy, JavaScript, Gradle, Docker, and a wide range of APIs and protocol implementations. Clone or checkout this project and send us pull requests.

First, checkout this project.

A demo can be started with downloaded dependencies and images (install [Docker Community Edition](https://www.docker.com/)):

```
./gradlew clean installDist
docker-compose -p openremote -f profile/demo.yml up --no-build
```

If you want to build the images instead of downloading them, execute:

```
./gradlew clean installDist
docker-compose -p openremote -f profile/demo.yml up --build
```

Access the manager UI and API on https://localhost/ with username `admin` and password `secret` (accept the 'insecure' self-signed SSL certificate).

To keep your data, backup the `deployment` directory and start the stack with `SETUP_INIT_CLEAN_DATABASE=false docker-compose ... up`. You can specify an alternative deployment directory with `DEPLOYMENT_DIRECTORY=/my/data docker-compose ... up`.

More configuration options of the images are documented [in the deploy.yml profile](https://github.com/openremote/openremote/blob/master/profile/deploy.yml).

Perform a clean build and run all tests (before committing):

```
docker-compose -p openremote -f profile/dev.yml up -d
./gradlew clean build installDist
docker build -t openremote/proxy:latest haproxy
docker build -t openremote/postgresql:latest postgresql
docker build -t openremote/keycloak:latest keycloak
docker build -t openremote/manager:latest manager/build/install
```

Push images to [Docker Hub](https://hub.docker.com/u/openremote):

```
docker push openremote/proxy:latest
docker push openremote/postgresql:latest
docker push openremote/keycloak:latest
docker push openremote/manager:latest
```

For more information and how to set up a development environment, see the [Developer Guide](https://github.com/openremote/openremote/wiki).

## Discuss OpenRemote

Join us on the [community group](https://groups.google.com/forum/#!forum/openremotecommunity).
