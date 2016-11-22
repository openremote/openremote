# OpenRemote 

* **Docker Images**: https://hub.docker.com/u/openremote/
* **Documentation/Wiki:** https://github.com/openremote/Documentation/wiki
* **Community:** https://groups.google.com/forum/#!forum/openremotecommunity
* **Issues:** https://github.com/openremote/openremote/issues

## Getting started with OpenRemote

Please see our [installation documentation](https://github.com/openremote/openremote/wiki/Installing-OpenRemote) for more information.

## Contributing to OpenRemote

We work with Java, Groovy, Gradle, Docker, and wide range of APIs and protocol implementations. Clone or checkout this project and send us pull requests. 

Please see our [developer documentation](https://github.com/openremote/openremote/wiki/Contributing-to-OpenRemote) for more information.

## Discuss OpenRemote

Join us on the [community group](https://groups.google.com/forum/#!forum/openremotecommunity).

## OpenRemote Projects

This is the platform project of OpenRemote, where we integrate services and code from many projects. Some projects developed by OpenRemote are co-located in this repository, others are dependencies that are developed and released independently.

* [Controller](https://github.com/openremote/Controller) - An IoT gateway with an HTTP API, it manages connections and protocols to devices and 3rd party services. Local state and rule execution provides intelligence at the edge of the network. Controllers can be provisioned directly through the file system or synchronize automatically with the Beehive service.

* [Designer](https://github.com/openremote/Designer) - Web application for configuring controllers and devices, writing rules, and editing user interfaces.

* [Web Console](https://github.com/openremote/WebConsole) - Connecting to a controller or its proxy, this web application renders panel configurations hosted on the controller and allows the user to monitor and interact with a controller.

* [Controller Command Service (CCS)](https://github.com/openremote/CCS) - A queue where you can put a command for any controller through HTTP. The controller receives commands through push or pull and executes them. Commands for establishing a secure tunnel on a socket of choice are included, thus implementing a remote proxy of a Controller.

TODO: Document other projects

```
docker-compose -p myproject -f profile/dependencies/postgresql.yml -f profile/dependencies/keycloak.yml up

org.openremote.ccs.Server#main()

GET localhost:8080/master/command/controller/123
(Should result in 403 NOT ALLOWED)
```
