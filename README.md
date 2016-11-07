# OpenRemote 


####  This is pre-alpha software under development, we are working on internal Milestone 2. Older code we are currently migrating can be found [in the M1 repository](https://github.com/openremote/or-controller).

---

* **Live Demo**: http://demo2.openremote.com (sign-in with `test/test` or `admin/admin`)
* **Docker Images**: https://hub.docker.com/u/openremote/
* **Documentation/Wiki:** https://github.com/openremote/openremote/wiki
* **Community:** https://groups.google.com/forum/#!forum/openremotecommunity
* **Issues:** https://github.com/openremote/openremote/issues

## Getting started

You'll need a Docker host to run this software. Please see our [installation documentation](https://github.com/openremote/openremote/wiki/Installing-OpenRemote) for more information.

## Setting up a development workspace

### Preparing the environment

All Docker and Gradle commands should be executed in the project root directory.

NOTE: For Docker volume mapping to work correctly on Windows and OS X ensure that your working directory is located somewhere under your home directory

NOTE: The Docker virtual machine's time will drift when you are using VirtualBox. You will see "token is not active" verification errors if the time skew is too large. Until this is [fixed](https://github.com/boot2docker/boot2docker/issues/69), periodically run `docker-machine ssh default 'sudo ntpclient -s -h pool.ntp.org'` to update the virtual machine's clock.

NOTE: Working with Docker might leave exited containers, untagged images, and dangling volumes. The following bash function can be used to clean up:

```
function dcleanup(){
    docker rm -v $(docker ps --filter status=exited -q 2>/dev/null) 2>/dev/null
    docker rmi $(docker images --filter dangling=true -q 2>/dev/null) 2>/dev/null
    docker volume rm $(docker volume ls -qf dangling=true 2>/dev/null) 2>/dev/null
}
```

For authentication and authorization we are using [Keycloak](http://keycloak.jboss.org/).

We are using PostgreSQL as a database backend. Test and demo data will be installed fresh every time you start the services. Note that this is not suitable for a production configuration!

To access the database directly with an SQL console, run:

```
docker exec -it openremote_postgres_dev_1 su -s /bin/sh - postgres -c "sh -c 'psql openremote'"
```

### Run required services

All services required for development can be deployed with Docker Compose, configured in `docker-compose.yml`. Execute `docker-compose up` in the project root to download required images (this might take a while and give no feedback) and run containers.

The default configuration of all `*Service` classes is for host IP `192.168.99.100`. If this is not your Docker host IP, you must set various environment variables (see [Demo](https://raw.githubusercontent.com/openremote/openremote/master/docker-compose-demo.yml)).

### Run GWT code server

The GWT compiler will listen for compilation requests and produce Javascript code from our Java code. Start and keep it running in the background with `./gradlew -p manager/client gwtSuperDev`.

### Import into IntelliJ IDEA

- Create a "New Project From Existing Sources" and import as a Gradle project

### Import into Eclipse

- Run `./gradlew eclipse`
- In Eclipse go to File > Import and import the project as "Existing Projects into Workspace"

### Run Manager server

Configure your IDE and set up a *Run Configuration*:

- Module: `manager/server`
- Working directory: (Set to project root directory)
- Main class: `org.openremote.manager.server.Server`

You can now open http://localhost:8080/ in your browser.

*NOTE: Please be aware that currently by default the web server binds to all interfaces (i.e. `0.0.0.0`)*

*NOTE: During development it can be useful to disable all security checks on the HTTP remote API, for example, to test without authentication and authorization in a simple HTTP client such as `curl`. Start the Manager with the `DISABLE_API_SECURITY=true` environment variable setting to disable security.*

### Running tests

You can run the tests of the `manager` project with `./gradlew test` or run the individual test classes in your IDE directly. The working directory must always be set to the project root diretory.

Note that some of these tests are end-to-end tests that require the whole environment to be running. This means your Keycloak and PostgreSQL containers will be used, and data will be inserted and deleted during a test run. You might want to start with clean containers before running tests and you might have to restart containers after (failed) tests.

### Updating map data

We currently do not have our own pipeline for extracting/converting OSM data into vector tilesets but depend on the extracts offered on https://github.com/osm2vectortiles/osm2vectortiles.

You can extract smaller tilesets with the following procedure:

1. Install tilelive converter: 
    `npm install -g mapnik mbtiles tilelive tilelive-bridge`
1. Select and copy boundary box coordinates of desired region: 
    http://tools.geofabrik.de/calc/#tab=1 
1. Extract the region with: 
    `tilelive-copy --minzoom=0 --maxzoom=14 --bounds="BOUNDARY BOX COORDINATES" theworld.mbtiles myextract.mbtiles`

## Production

### Publishing images

Build Docker images with `./gradlew clean buildImage`. Remove old images before if you don't want to use the Docker build cache.

You can start and test these images with the `docker-compose-demo.yml` configuration.

You can also directly build and push the image to our [Docker Hub Account](https://hub.docker.com/u/openremote/): `/gradlew clean pushImage -PdockerHubUsername=username -PdockerHubPassword=secret`
