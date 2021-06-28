# OpenRemote v3

![CI/CD](https://github.com/openremote/openremote/workflows/CI/CD/badge.svg)
[![Open Source? Yes!](https://badgen.net/badge/Open%20Source%20%3F/Yes%21/blue?icon=github)](https://github.com/Naereen/badges/)
<!-- ![tests](https://github.com/openremote/openremote/workflows/tests/badge.svg) -->

[Source](https://github.com/openremote/openremote) **·** [Documentation](https://github.com/openremote/openremote/wiki) **·** [Forum](https://forum.openremote.io) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](https://openremote.io)

Welcome to the OpenRemote 3.0 platform; an intuitive user-friendly 100% open source IoT platform. We have our origins in Home Automation
but our 3.0 platform is focused on generic IoT applications and is a completely different stack to any of our 2.x services. As the code
base is 100% open source then the applications are limitless. Here's an architecture overview:

![Architecture 3.0](https://github.com/openremote/Documentation/blob/master/manuscript/figures/architecture-3.jpg)

## Quickstart

You can quickly try the online demo with restricted access, login credentials are `smartcity:smartcity`:

[Online demo](https://demo.openremote.io/manager/?realm=smartcity)

The quickest way to get your own environment with full access is to make use of our docker images (both `amd64` and `arm64` are supported). 
1. Make sure you have [Docker Desktop](https://www.docker.com/products/docker-desktop) installed (v18+). 
2. Download the docker compose file:
[OpenRemote Stack](https://raw.githubusercontent.com/openremote/openremote/master/docker-compose.yml) (Right click 'Save link as...')
3. In a terminal `cd` to where you just saved the compose file and then run:

    `docker-compose -p openremote up`

If all goes well then you should now be able to access the OpenRemote Manager UI at [https://localhost](https://localhost). You will need to accept the self-signed 
certificate, see [here](https://www.technipages.com/google-chrome-bypass-your-connection-is-not-private-message) for details how to do this in Chrome (similar for other browsers).

### Login credentials
Username: admin  
Password: secret

### Changing host and/or port
The URL you use to access the system is important, the default is configured as `https://localhost` if you are using a VM or want to run on a different port then you will need to set the `EXTERNAL_URL` environmnet variable and if changing ports you'll need to update your docker compose profile, so if for example you will be accessing using `https://192.168.1.1:8443` then update your `docker-compose.yml` file:

```
  proxy:
    ...
    ports:
      - "8000:80"
      - "8443:443"
      - "8883:8883"
```

Then use the following startup command:



BASH: 
```
EXTERNAL_URL=https://192.168.1.1:8443 docker-compose -p openremote up -d
```
or

CMD:
```
cmd /C "set EXTERNAL_URL=https://192.168.1.1:8443 && docker-compose -p openremote up -d"
```


## What next
Try creating assets, agents, rules, users, realms, etc. using the Manager UI, please refer to the [wiki](https://github.com/openremote/openremote/wiki) for more information, some things to try:

- [Manager UI Guide](https://github.com/openremote/openremote/wiki/User-Guide:-Manager-UI) - Learn more about the User Interface
- [Creating an HTTP Agent tutorial](https://github.com/openremote/openremote/wiki/Tutorial%3A-Open-Weather-API-using-HTTP-Agent) - Connect to an online weather service
- [Custom Deployment](https://github.com/openremote/openremote/wiki/User-Guide%3A-Custom-deployment) - Style the Manager to your brand
- [Setting up an IDE](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Setting-up-an-IDE) - Set up your development environment
- [Working on the UI](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-UI-apps-and-components) - Create a web application compatible with OpenRemote
- [Creating a custom project](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Creating-a-custom-project) - Create a project with custom protocols, asset types and setup code

## Where's the data stored?
Persistent data is stored in a PostgreSQL DB which is stored in the `openremote_postgresql-data` docker volume which is durably stored independently of the running containers (see all with `docker volume ls`).
If you want to create a backup of your installation, just make a copy of this volume.


## Contributing to OpenRemote

We work with Java, Groovy, TypeScript, Gradle, Docker, and a wide range of APIs and protocol implementations. Clone or checkout this project and send us pull requests, ensure that code is covered by tests and that the full test suite passes.

For more information and how to set up a development environment, see the [Developer Guide](https://github.com/openremote/openremote/wiki).

## Discuss OpenRemote

Join us on the [community forum](https://forum.openremote.io/).
