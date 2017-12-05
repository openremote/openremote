# OpenRemote v3

[Source](https://github.com/openremote/openremote) **·** [Documentation](https://github.com/openremote/openremote/wiki) **·** [Community](https://groups.google.com/forum/#!forum/openremotecommunity) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](http://openremote.com)

## Getting started with OpenRemote

We are currently working on v3 of the OpenRemote platform. This is **alpha** software that should be used only for development.

If you want to try OpenRemote v2, [read the OpenRemote v2 documentation](https://github.com/openremote/Documentation/wiki).

## Contributing to OpenRemote

We work with Java, Groovy, JavaScript, Gradle, Docker, and a wide range of APIs and protocol implementations. Clone or checkout this project and send us pull requests.

First, checkout this project or at a minimum, get the Docker Compose [profiles](profile/).

You'll need [Docker Community Edition](https://www.docker.com/) for Windows, macOS, or Linux.

Also see our [developer guide](https://github.com/openremote/openremote/wiki) for more details.

### Starting a demo stack

A demo stack can be started with downloaded dependencies and images (you only have to get the [profiles](profile/)):

```
docker-compose -p openremote -f profile/demo.yml up --no-build
```

Access the manager UI and API on https://localhost/ with username `admin` and password `secret`. Accept the 'insecure' self-signed SSL certificate.

Stop the stack and remove all unused data volumes (on your host!) with:

```
docker-compose -p openremote -f profile/demo.yml down
docker volume prune
```

More configuration options of the images are documented [in the deploy.yml profile](https://github.com/openremote/openremote/blob/master/profile/deploy.yml).

### Preserving demo data

To keep your data, don't delete the Docker volumes `openremote_manger-data` and `openremote_postgresql-data` between restarts. You must also change `SETUP_WIPE_CLEAN_INSTALL` to `false` in `demo.yml`!

### Building images from source

If you want to build the images instead of downloading them, execute:

```
./gradlew clean installDist
docker-compose -p openremote -f profile/demo.yml up --build
```

### Using `dev` profiles

Also consider using the `dev` profiles if you want to change code, they will run required services in the background for various development/build tasks. The whole stack in development mode can be started with:

```
./gradlew clean installDist
docker-compose -p openremote -f profile/dev.yml up --build
```

### Committing changes

Perform a clean build, delete all data volumes (!), and run all tests before committing:

```
docker volume prune
docker-compose -p openremote -f profile/dev-testing.yml up -d
./gradlew clean build installDist
docker-compose -p openremote -f profile/dev-testing.yml down
docker build -t openremote/proxy:latest haproxy
docker build -t openremote/postgresql:latest postgresql
docker build -t openremote/keycloak:latest keycloak
docker build -t openremote/manager:latest manager/build/install/manager
```

Push images to [Docker Hub](https://hub.docker.com/u/openremote):

```
docker login
docker push openremote/proxy:latest
docker push openremote/postgresql:latest
docker push openremote/keycloak:latest
docker push openremote/manager:latest
```

For more information and how to set up a development environment, see the [Developer Guide](https://github.com/openremote/openremote/wiki).

## Discuss OpenRemote

Join us on the [community group](https://groups.google.com/forum/#!forum/openremotecommunity).
