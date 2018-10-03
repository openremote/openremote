# OpenRemote v3 Docker Stacks

The profiles extend each other, your project should also extend them:

* [deploy.yml](/profile/deploy.yml) - The root profile with default settings, should be extended only

    * [demo.yml](/profile/demo.yml) - A transient demo stack for x86 hosts, copy and modify for permanent installation

    * [demo-rpi.yml](/profile/demo-rpi.yml) - Settings for deployment on Raspberry Pi3 aarch32 host

    * [dev.yml](/profile/dev.yml) - A transient development stack with all services, uses local bind mounts

    * [dev-testing.yml](/profile/dev-testing.yml) - A transient development minimal services stack used to run build tests and work in IDE
    
    * [dev-manager.yml](/profile/dev-manager.yml) - A transient development minimal services stack used to work on the manager in IDE behind the reverse proxy (replicates production setup)

