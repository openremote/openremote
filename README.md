# OpenRemote v3

![CI/CD](https://github.com/openremote/openremote/workflows/CI/CD/badge.svg)
![tests](https://github.com/openremote/openremote/workflows/tests/badge.svg)
[![Open Source? Yes!](https://badgen.net/badge/Open%20Source%20%3F/Yes%21/blue?icon=github)](https://github.com/Naereen/badges/)


[Source](https://github.com/openremote/openremote) **·** [Documentation](https://github.com/openremote/openremote/wiki) **·** [Community](https://forum.openremote.io) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](https://openremote.io)

We are currently working on OpenRemote Manager v3, a concise 100% open source IoT platform. This is **beta** software.

## Quickstart

Before following this quickstart make sure you have [prepared your environment](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Preparing-the-environment). There are two options how to start with OpenRemote:

1. Starting OpenRemote with images from Docker Hub (easiest)
2. Starting OpenRemote with source-build images

### 0. Starting OpenRemote with command-line-interface CLI (beta)

```openremote-cli``` (short ```or```) is a command line tool which can be used for installing an instance of OpenRemote stack on local machine. You should already have ```python```, ```wget```, ```docker``` and ```docker-compose``` installed.

```bash
python3 -m pip install -U openremote-cli
openremote-cli -V
```

```bash
or deploy --action create
```

### 1. Starting OpenRemote with images from Docker Hub

We publish Docker images to [Docker Hub](https://hub.docker.com/u/openremote/). First clone or download our [source code](https://github.com/openremote/openremote) and from the root directory:

```
docker-compose pull
```

To run OpenRemote using Docker Hub images, execute the following command from the checked out root project directory:

```
docker-compose up --no-build
```

To run OpenRemote is swarm mode, which uses Docker Hub images:

```
docker stack deploy --compose-file swarm/swarm-docker-compose.yml openremote
```
you don't need to pull or build images in this case, docker swarm mode does this automatically.

### 2. Starting OpenRemote with source-build images

Alternatively you can build the Docker images locally from source, please see [here](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Preparing-the-environment) for required tooling. First build the code:

```
./gradlew clean installDist
```

We have also embeded tooling in a docker container and you can use it instead in case you have different tooling installed, e.g. higer version of JDK than 8.
```
docker run --rm -v $(pwd):/or registry.gitlab.com/openremote/openremote:master ./gradlew clean installDist
```

Next, if you are using Docker Community Edition build the Docker images and start the stack with:

```
docker-compose up --build
```

A first build will download many dependencies (and cache them locally for future builds), this can take up to 30 minutes.

### Using the OpenRemote demo

When all Docker containers are ready, you can access the OpenRemote UI and API with a web browser (if you are using Docker Toolbox replace `localhost` with `192.168.99.100`):

**OpenRemote Manager:** https://localhost  
Username: admin  
Password: secret

**Demo Smart City App:** https://localhost/main/?realm=smartcity
Username: smartcity  
Password: smartcity

You must accept and make an exception for the 'insecure' self-signed SSL certificate. You can configure a production installation of OpenRemote with a your own certificate or automatically use one from [Let's Encrypt](https://letsencrypt.org/).

### Preserving data and configuration

Interrupting the `docker-compose up` execution stops the stack running in the foreground. The OpenRemote containers will stop but not be removed. To stop **and** remove the containers, use:

```
docker-compose down
```

This will not affect your data, which is durably stored independently from containers in Docker volumes (see all with `docker volume ls`):

- `openremote_deployment-data` (map tiles, static resources)
- `openremote_postgresql-data` (user/asset database storage)
- `openremote_proxy-data` (SSL proxy configuration and certificates)

If you want to create a backup of your installation, make a copy of these volumes.

**The default configuration will wipe the user/asset database storage and import demo data when containers are started!** This can be changed with the environment variable `SETUP_WIPE_CLEAN_INSTALL`.  Set it to to `false` in `docker-compose.yml` or provide it on the command line.

When a configuration environment variable is changed, you must recreate containers. Stop and remove them with `docker-compose down` and then `docker-compose up` the stack again.

More configuration options of the images are documented [in the deploy.yml profile](https://github.com/openremote/openremote/blob/master/profile/deploy.yml).

## Contributing to OpenRemote

We work with Java, Groovy, TypeScript, Gradle, Docker, and a wide range of APIs and protocol implementations. Clone or checkout this project and send us pull requests, ensure that code is covered by tests and that the full test suite passes.

For more information and how to set up a development environment, see the [Developer Guide](https://github.com/openremote/openremote/wiki).


## Discuss OpenRemote

Join us on the [community forum](https://forum.openremote.io/).

## See also

- [Next 'Get Started' step: Connecting to an HTTP API](https://github.com/openremote/openremote/wiki/User-Guide%3A-Connecting-to-a-HTTP-API)
- [Get Started](https://openremote.io/get-started-manager/)
