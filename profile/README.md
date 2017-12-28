# OpenRemote v3 Docker Stacks

The profiles extend each other, your project should also extend them:

* [deploy.yml](/deploy.yml) - The root profile with default settings, should be extended only

    * [demo.yml](/demo.yml) - A transient demo stack for x86 hosts, copy and modify for permanent installation

    * [demo-rpi.yml](/demo-rpi.yml) - Settings for deployment on Raspberry Pi3 aarch32 host

    * [dev.yml](/dev.yml) - A transient development stack with all services, uses local bind mounts

    * [dev-testing.yml](/dev-testing.yml) - A transient development minimal services stack used to run build tests and work in IDE

