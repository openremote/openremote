# OpenRemote

### Preparing your development environment

* Install Docker Toolbox: https://www.docker.com/products/overview#/docker_toolbox
* Start Kitematic and open the *Docker CLI*. The environment variable `DOCKER_HOST` should be set and you should be able to execute `docker images`, `docker ps` and any other Docker client command.

All Docker and Gradle commands should be executed in the project root directory.

## Context Broker

We are using the [Orion Context Broker](https://fiware-orion.readthedocs.org/en/develop/) with a MongoDB backend. You can start an instance with non-persistent data store using Docker:

* Get the IP of your Docker host VM: `docker-machine ip default`
* Run the containers: `docker-compose up`

## Manager

### Development

* Run GWT code server: `./gradlew -p manager gwtSuperDev`
* Start the server:
    - Change working directory to `manager/` or set the `WEB_SERVER_DOCROOT` environment variable to `manager/src/main/webapp/`
    - Set the environment variable `CONTEXT_BROKER_HOST` to your Docker host VM IP
    - Run main class `org.openremote.manager.server.Server`
* Open http://localhost:8080/
* Run tests: `./gradlew -p manager test`

### Deployment as container

* Run `./gradlew -p manager startContainer`
* Tail the log: `docker logs -f openremote-manager`
* Open http://your_docker_host_ip:8080/
* Cleanup with `./gradlew stopContainer removeContainer removeImage`

## Updating map data

We currently do not have our own pipeline for extracting/converting OSM data into vector tilesets but depend on the extracts offered on https://github.com/osm2vectortiles/osm2vectortiles.

You can extract smaller tilesets with the following procedure:

1. Install tilelive converter: 
    `npm install -g mapnik mbtiles tilelive tilelive-bridge`
1. Select and copy boundary box coordinates of desired region: 
    http://tools.geofabrik.de/calc/#tab=1 
1. Extract the region with: 
    `tilelive-copy --minzoom=0 --maxzoom=14 --bounds="BOUNDARY BOX COORDINATES" theworld.mbtiles myextract.mbtiles`

## Updating Keycloak

If you change any of the profile configuration `keycloak/profiles/` you must rebuild the Keycloak image with `docker-compose build`.
