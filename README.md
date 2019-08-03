# OpenRemote v3

[Source](https://github.com/openremote/openremote) **·** [Documentation](https://github.com/openremote/openremote/wiki) **·** [Community](https://groups.google.com/forum/#!forum/openremotecommunity) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](https://openremote.io)

We are currently working on v3 of the OpenRemote platform. This is **beta** software that should be used only for development.

If you want to try OpenRemote v2, [read the OpenRemote v2 documentation](https://github.com/openremote/Documentation/wiki).

## Quickstart

Before following this quickstart make sure you have [prepared your environment](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Preparing-the-environment). There are two options how to start with OpenRemote:

1. Starting OpenRemote with images from Docker Hub (easiest)
2. Starting OpenRemote with source-build images

### 1. Starting OpenRemote with images from Docker Hub

We publish Docker images to [Docker Hub](https://hub.docker.com/u/openremote/):

```
docker pull openremote/postgresql
docker pull openremote/keycloak
docker pull openremote/proxy
docker pull openremote/manager
```

To run OpenRemote using Docker Hub images, execute the following command from the checked out root project directory:

```
docker-compose up --no-build
```

***NOTE:*** If you are not using Docker Community Edition but the older **Docker Toolbox (Virtual Box),** please follow the following steps:
 
 1. Open VirtualBox and select the `docker` VM (called `default` unless specified otherwise) then go to Settings -> Network -> Adapter 1 -> Advanced -> Port Forwarding
 2. Add the following rules:
 
 | Name | Protocol | Host IP | Host Port | Guest IP | Guest Port |
 | --- | --- | --- | --- | --- | --- |
 |postgresql|TCP||5432||5432|
 | keycloak | TCP |  | 8081 |  | 8081 |
 | map | TCP |  | 8082 |  | 8082 |
 | proxy http | TCP |  | 80 |  | 80 |
 | proxy https | TCP |  | 443 |  | 443 |
  
Alternatively you will have to replace `localhost` with your docker VM IP address (typically `192.168.99.100` but type `docker-machine ip default` if unsure) in all URLs below and also you must specify the `IDENTITY_NETWORK_HOST` environment variable as the IP address of the Docker VM when executing `docker-compose` commands:

Windows Command Prompt (quotes are essential):
```
set "IDENTITY_NETWORK_HOST=192.168.99.100" && docker-compose up --no-build
```

Bash:
```
IDENTITY_NETWORK_HOST=192.168.99.100 docker-compose up --no-build
```

### 2. Starting OpenRemote with source-build images

Alternatively you can build the Docker images locally from source, please see [here](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Preparing-the-environment) for required tooling. First build the code:

```
./gradlew clean installDist
```

Next, if you are using Docker Community Edition build the Docker images and start the stack with:

```
docker-compose up --build
```

***NOTE:*** If you are not using Docker Community Edition but the older **Docker Toolbox (Virtual Box),** please follow the following steps:
 
 1. Open VirtualBox and select the `docker` VM (called `default` unless specified otherwise) then go to Settings -> Network -> Adapter 1 -> Advanced -> Port Forwarding
 2. Add the following rules:
 
 | Name | Protocol | Host IP | Host Port | Guest IP | Guest Port |
 | --- | --- | --- | --- | --- | --- |
 | postgresql | TCP |  | 5432 |  | 5432 |
 | keycloak | TCP |  | 8081 |  | 8081 |
 | map | TCP |  | 8082 |  | 8082 |
 | proxy http | TCP |  | 80 |  | 80 |
 | proxy https | TCP |  | 443 |  | 443 |
  
Alternatively you will have to replace `localhost` with your docker VM IP address (typically `192.168.99.100` but type `docker-machine ip default` if unsure) in all URLs below and also you must specify the `IDENTITY_NETWORK_HOST` environment variable as the IP address of the Docker VM when executing `docker-compose` commands:

Windows Command Prompt (quotes are essential):
```
set "IDENTITY_NETWORK_HOST=192.168.99.100" && docker-compose up --build
```
Bash:
```
IDENTITY_NETWORK_HOST=192.168.99.100 docker-compose up --build
```

A first build will download many dependencies (and cache them locally for future builds), this can take up to 30 minutes.

### Using the OpenRemote demo

When all Docker containers are ready, you can access the OpenRemote UI and API with a web browser (if you are using Docker Toolbox replace `localhost` with `192.168.99.100`):

**OpenRemote Manager:** https://localhost  
Username: admin  
Password: secret

**Demo Smart Building App:** https://localhost/smart-building-v1/  
Username: building  
Password: building

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

Join us on the [community group](https://groups.google.com/forum/#!forum/openremotecommunity).
