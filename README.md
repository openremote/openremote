# OpenRemote 
> IoT Management Simplified

---

* **Live Demo**: http://demo2.openremote.com (test/test)
* **Docker Images**: https://hub.docker.com/u/openremote/
* **Documentation/Wiki:** https://github.com/openremote/openremote/wiki
* **Community:** https://groups.google.com/forum/#!forum/openremotecommunity
* **Issues:** https://github.com/openremote/openremote/issues

## Quickstart

You'll need a Docker host to run this software.

1. Install [Docker Toolbox](https://www.docker.com/products/overview#/docker_toolbox)
1. Start Kitematic and open the *Docker CLI*
1. Get the IP of your Docker host VM with: `docker-machine ip default`
1. Download [docker-compose-quickstart.yml](https://raw.githubusercontent.com/openremote/openremote/master/docker-compose-quickstart.yml)
1. Edit this file and review the settings
1. Deploy the whole stack with: `docker-compose -f docker-compose-quickstart.yml [up|down]`
1. Open http://DOCKER_HOST_IP:MAPPED_PUBLIC_PORT in your browser (default http://192.168.99.100:8080/)

## Development

### Preparing the environment

All Docker and Gradle commands should be executed in the project root directory.

NOTE: For Docker volume mapping to work correctly on Windows and OS X ensure that your working directory is located somewhere under your home directory

NOTE: The Docker virtual machine's time will drift when you are using VirtualBox. Until this is [fixed](https://github.com/boot2docker/boot2docker/issues/69), periodically run `docker-machine ssh default 'sudo ntpclient -s -h pool.ntp.org'` to update the virtual machine's clock.

We are using the [Orion Context Broker](https://fiware-orion.readthedocs.org/en/develop/) with a MongoDB backend. For development, this is an instance with a non-persistent data store.

For authentication and authorization we are using [Keycloak](http://keycloak.jboss.org/) with a non-persistent data store.

Test and sample data will be installed fresh every time you start the server(s).

### Run required services

All services required for development can be deployed with Docker Compose, configured in `docker-compose.yml`. Execute `docker-compose up` in the project root to download required images (this might take a while and give no feedback) and run containers.

The default configuration of all `*Service` classes is for host IP `192.168.99.100`. If this is not your Docker host IP, you must set various environment variables (see [Quickstart](https://raw.githubusercontent.com/openremote/openremote/master/docker-compose-quickstart.yml)).

### Run GWT code server

The GWT compiler will listen for compilation requests and produce Javascript code from our Java code. Start and keep it running in the background with `./gradlew -p manager gwtSuperDev`.

### Import project to Eclipse (optional)

- run `./gradlew eclipse`
- In Eclipse go to File > Import and import the project as "Existing Projects into Workspace"

### Run Manager server

Configure your IDE and set up a *Run Configuration*:

- Module: `manager`
- Working directory: `manager/`
- Main class: `org.openremote.manager.server.Server`

You can now open http://localhost:8080/ in your browser.

### Running tests

You can run the tests of the `manager` project with `./gradlew test` or run the individual test classes in your IDE directly. Note that some of these tests are end-to-end tests that require the whole environment to be running. This means your Keycloak and Orion containers will be used, and data will be inserted and deleted during a test run. You might want to start with clean containers before running tests and you might have to restart containers after (failed) tests.

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


Build Docker images with `./gradlew buildImage`, you might want to `clean` before.

You can also directly build and push the image to our [Docker Hub Account](https://hub.docker.com/u/openremote/): `/gradlew pushImage -PdockerHubUsername=username -PdockerHubPassword=secret`
