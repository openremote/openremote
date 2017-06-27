# Installing OpenRemote on Raspberry Pi 3

You need a [Raspberry Pi 3](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/) and and empty SD card. First download [Etcher](https://etcher.io/) to burn SD card images and then download the [HypriotOS SD card image](https://blog.hypriot.com/downloads/) and write it to your card. This is a minimal Linux variant that is optimized for running Docker containers.

## Docker machine setup

The Docker server and your workstation Docker client must be customized so you can connect and deploy OpenRemote containers on the Raspberry Pi.

On your workstation you should have a Docker installation. Execute the following script to generate the necessary configuration files in the `build` directory:

```
DOCKER_MACHINE_NAME=openremote1 \
DOCKER_MACHINE_IP=10.0.0.123 \
WIFI_SSID=MyWifiNetwork \
WIFI_PASSWORD=MyWifiPassword \
SSH_PUBKEY=$HOME/.ssh/id_dsa.pub \
 platform/rpi3/machine/openremote-setup.sh
```

Note that `DOCKER_MACHINE_NAME` is used for the name of the Docker Machine configuration on your workstation, as well as the host name of the Raspberry Pi.

The `DOCKER_MACHINE_IP` should be the IP address you expect your Raspberry Pi to have when it connects to your LAN. We recommend you lease a static IP address to the machine in your LAN router's DHCP setup. If you don't use Wifi, edit the file `build/boot/device-init.yaml` after generation.

The provided `SSH_PUBKEY` will be the only authorized key for SSH connections to your Raspberry Pi, password authentication is disabled. You can connect with the user `openremote` on port 22 or simply use `docker-machine ssh openremote1`.

The setup script generates files that should be copied onto the `boot` partition of your SD card, which is visible when you mount the SD card on your workstation. Also copy the Docker Machine client configuration files into the `$HOME/.docker/machine/machines/` directory.

After booting the Raspberry Pi from the SD card, verify the connection with a Docker client from your workstation:

```
eval $(docker-machine env openremote1)
docker version
```

## Customizing Docker authentication

By default a dummy CA and self-signed certificates are generated for the Docker client connection and authentication on the Docker host. The same key is used on the server and client. See the comments in `platform/rpi3/machine/openremote-setup.sh` for customization options.

## Deploying OpenRemote containers

TBC