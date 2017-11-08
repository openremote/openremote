# Installing OpenRemote on Raspberry Pi 3

You need a [Raspberry Pi 3](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/) and and empty SD card. First download [Etcher](https://etcher.io/) to burn SD card images.


Download the [Pi64 Lite distribution](https://github.com/bamarni/pi64/releases) and write it to your card. This is a minimal Debian Linux variant that runs on a 64 bit kernel with OpenJDK ARM64 optimized for ths platform.

Next you must copy some files onto your new SD card, so keep it mounted on your computer. Generate the files with:

```
SSH_PUBKEY=/the/path/to/your/ssh/pubkey/id_rsa.pub ./gradlew clean prepareImage
```

Copy everything from `platform/rpi3/build/install` onto your SD card.

Boot your Raspberry Pi 3 with the SD card and wait until you can login with SSH. If you don't know the IP address of your LAN-connected PI (edit `setup` before booting first time to enable WiFi), you can scan your network with:

```
nmap -sP 10.0.0.0/24
```

Test the SSH connection with: `ssh openremote@<IP of your Raspberry Pi 3>`

Next copy from your local machine the OpenRemote app to the Raspberry Pi 3 (there is not enough space on boot partition to include it there):

```
scp -r manager/build/install/manager/client manager/build/install/manager/server openremote@<IP of your Raspberry Pi3>:/home/openremote/app/
```

Continue installation by connecting with SSH and run on the Raspberry Pi 3:
```
sudo POSTGRES_DB=openremote POSTGRES_USER=openremote POSTGRES_PASSWORD='dbsecret' ./openremote-install.sh
```

This will download packages from the Internet, so make sure your machine is connected properly.

Start OpenRemote with:

```
sudo DATABASE_PASSWORD='dbsecret' SETUP_BASIC_IDENTITY_ADMIN_PASSWORD='secret123' IDENTITY_NETWORK_HOST=<IP of your Raspberry Pi3> ./openremote-run.sh
```

Open the Manager UI on `http://<IP of your Raspberry Pi 3>/` and login with `admin` and your password.

TODO: The SSH 'openremote' user on the machine is still secured with the default 'raspberry' password. We should disable password authentication completely, see `setup`.

 <!-- ## Docker machine setup -->

 <!-- The Docker server and your workstation Docker client must be customized so you can connect and deploy OpenRemote containers on the Raspberry Pi. -->

 <!-- On your workstation you should have a Docker installation. Execute the following script to generate the necessary configuration files in the `build` directory: -->

 <!-- ``` -->
 <!-- DOCKER_MACHINE_NAME=openremote1 \ -->
 <!-- DOCKER_MACHINE_IP=10.0.0.123 \ -->
 <!-- WIFI_SSID=MyWifiNetwork \ -->
 <!-- WIFI_PASSWORD=MyWifiPassword \ -->
 <!-- SSH_PUBKEY=$HOME/.ssh/id_rsa.pub \ -->
  <!-- platform/rpi3/machine/openremote-setup.sh -->
 <!-- ``` -->

 <!-- Note that `DOCKER_MACHINE_NAME` is used for the name of the Docker Machine configuration on your workstation, as well as the host name of the Raspberry Pi. -->

 <!-- The network setup of the Raspberry Pi will request an IP on interface `wlan0` with DHCP. Make sure your LAN router provides the configured the `DOCKER_MACHINE_IP`. If another IP is assigned, you must edit the generated `config.json` Docker Machine settings. If you don't use Wifi, edit the file `build/boot/device-init.yaml` after generation. -->

 <!-- The provided `SSH_PUBKEY` will be the only authorized key for SSH connections to your Raspberry Pi, password authentication is disabled. You can connect with the user `openremote` on port 22 or simply use `docker-machine ssh openremote1`. -->

 <!-- The setup script generates files that should be copied onto the `boot` partition of your SD card, which is visible when you mount the SD card on your workstation. Also copy the Docker Machine client configuration files into the `$HOME/.docker/machine/machines/` directory. -->

 <!-- After booting the Raspberry Pi from the SD card, verify the connection with a Docker client from your workstation: -->

 <!-- ``` -->
 <!-- eval $(docker-machine env openremote1) -->
 <!-- docker version -->
 <!-- ``` -->

 <!-- TODO: Alternative OS with aarch64 support and better OpenJDK JIT, but currently no Docker CE packages exist: https://github.com/bamarni/pi64 -->
 <!-- TODO: Alternative OS: https://project31.github.io/ -->

 <!-- ## Customizing Docker authentication -->

 <!-- By default a dummy CA and self-signed certificates are generated for the Docker client connection and authentication on the Docker host. The same key is used on the server and client. See the comments in `platform/rpi3/machine/openremote-setup.sh` for customization options. -->

 <!-- ## Deploying OpenRemote containers -->

 <!-- The Docker images for OpenRemote can be build on your x86 workstation with the QEMU emulator for the ARM environment. After building the images, you can export and load them onto the Raspberry Pi. -->

 <!-- First build the base image: -->

 <!-- ``` -->
 <!-- docker build -t openremote/rpi-raspbian platform/rpi3/rpi-raspbian -->
 <!-- ``` -->

 <!-- Next build the OpenRemote service images from source with: -->

 <!-- ``` -->
 <!-- ./gradlew clean prepareImage -->
 <!-- docker build -t openremote/rpi-haproxy:latest -f haproxy/rpi3.Dockerfile haproxy -->
 <!-- docker build -t openremote/rpi-postgresql:latest -f postgresql/rpi3.Dockerfile postgresql -->
 <!-- docker build -t openremote/rpi-manager:latest -f manager/build/install/rpi3.Dockerfile manager/build/install -->
 <!-- ``` -->

 <!-- TODO: The Manager image uses Oracle JDK by default, which must be licensed if deployed in production! You can switch to the much slower OpenJDK in the `Dockerfile`. We expect a faster OpenJDK to be available with AARCH64 OS. -->

 <!-- Copy them to your Raspberry Pi (replace `openremote1` with your `DOCKER_MACHINE_NAME`): -->

 <!-- ``` -->
 <!-- docker save openremote/rpi-haproxy | (eval $(docker-machine env openremote1) && docker load) -->
 <!-- docker save openremote/rpi-manager | (eval $(docker-machine env openremote1) && docker load) -->
 <!-- docker save openremote/rpi-postgresql | (eval $(docker-machine env openremote1) && docker load) -->
 <!-- ``` -->

 <!-- Deploy the containers, providing the IP/hostname of your Raspberry Pi: -->

 <!-- ``` -->
 <!-- IDENTITY_NETWORK_HOST=10.0.0.123 docker-compose -p openremote -f profile/demo_rpi.yml up -->
 <!-- ``` -->

 <!-- Open the Manager UI on `https://10.0.0.123`, accepting the self-signed demo SSL certificate. Login with `admin` and password `secret`. Edit the profile to change the admin password and other settings. -->
