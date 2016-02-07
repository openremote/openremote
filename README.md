# OpenRemote

### Preparing your development environment

* Install Docker Toolbox: https://www.docker.com/products/overview#/docker_toolbox
* Start Kitematic and open the *Docker CLI*. The environment variable `DOCKER_HOST` should be set and you should be able to execute `docker images`, `docker ps` and any other Docker client command.

## Manager

### Development

* Run GWT code server: `./gradlew -p manager gwtSuperDev`
* Run server main class `org.openremote.manager.server.Server`
* Open http://localhost:8080/
* Run tests: `./gradlew test`

### Deployment as container

* Run `./gradlew -p manager startContainer`
* Tail the log: `docker logs -f openremote-manager`
* Open http://localhost:8080/
* Cleanup with `./gradlew stopContainer removeContainer removeImage`
