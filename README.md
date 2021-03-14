# OpenRemote v3

![CI/CD](https://github.com/openremote/openremote/workflows/CI/CD/badge.svg)
[![Open Source? Yes!](https://badgen.net/badge/Open%20Source%20%3F/Yes%21/blue?icon=github)](https://github.com/Naereen/badges/)
<!-- ![tests](https://github.com/openremote/openremote/workflows/tests/badge.svg) -->

[Source](https://github.com/openremote/openremote) **·** [Documentation](https://github.com/openremote/openremote/wiki) **·** [Community](https://forum.openremote.io) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](https://openremote.io)

Welcome to the OpenRemote 3.0 platform; an intuitive user-friendly 100% open source IoT platform. We have our origins in Home Automation
but our 3.0 platform is focused on generic IoT applications and is a completely different stack to any of our 2.x services. As the code
base is 100% open source then the applications are limitless. Here's an architecture overview:

![Architecture 3.0](https://github.com/openremote/Documentation/blob/master/manuscript/figures/architecture-3.jpg)

## Quickstart

You can quickly try the online demo with restricted access, login credentials are `smartcity:smartcity`:

[Online demo](https://demo.openremote.io/manager/?realm=smartcity)

The quickest way to get your own environment with full access is to make use of our docker images (both `amd64` and `arm64` are supported). First make sure you have [Docker Desktop](https://www.docker.com/products/docker-desktop) installed (v18+). Then download the docker compose file:

[OpenRemote Stack](https://raw.githubusercontent.com/openremote/openremote/master/docker-compose.yml)

In a terminal `cd` to where you just saved the compose file and then run:

`docker-compose -p openremote up`

If all goes well then you should now be able to access the OpenRemote Manager UI at [https://localhost](https://localhost), you will need to accept the self-signed 
certificate, see [here](https://www.technipages.com/google-chrome-bypass-your-connection-is-not-private-message) for details how to do this in Chrome (similar for other browsers).

### Login credentials
Username: admin  
Password: secret

## What next
Try creating assets, agents, rules, users, realms, etc. using the Manager UI, please refer to the [wiki](https://github.com/openremote/openremote/wiki) for more information, some things to try:

- [Manager UI Guide](https://github.com/openremote/openremote/wiki/Demo-Smart-City)
- [Creating a HTTP Agent](https://github.com/openremote/openremote/wiki/User-Guide%3A-Connecting-to-a-HTTP-API)
- [Setting up an IDE](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Setting-up-an-IDE)
- [Working on the UI](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Working-on-the-UI)

## Where's the data stored?
Persistent data is stored in a PostgreSQL DB which is stored in the `openremote_postgresql-data` docker volume which is durably stored independently of the running containers (see all with `docker volume ls`).
If you want to create a backup of your installation, just make a copy of this volume.


## Contributing to OpenRemote

We work with Java, Groovy, TypeScript, Gradle, Docker, and a wide range of APIs and protocol implementations. Clone or checkout this project and send us pull requests, ensure that code is covered by tests and that the full test suite passes.

For more information and how to set up a development environment, see the [Developer Guide](https://github.com/openremote/openremote/wiki).

## Discuss OpenRemote

Join us on the [community forum](https://forum.openremote.io/).
