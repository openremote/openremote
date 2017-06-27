#!/bin/bash -e

# Generates SSL files for Docker client/server setup

# Prepares a Docker Machine client configuration

# Prepares a Hypriot OS boot partition custom init script with
# Wifi configuration and SSH pubkey-only authentication

: ${DOCKER_MACHINE_NAME?"Please set the name of your configuration"}
: ${DOCKER_MACHINE_IP:?"Please set the IP/hostname of your host"}
: ${SSH_PUBKEY:?"Please set the path to your SSH public key"}
: ${WIFI_SSID:?"Please set the Wifi network SSID"}
: ${WIFI_PASSWORD:?"Please set the Wifi password"}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Clean build directory
BUILD_DIR=$DIR/build
if [[ -d $BUILD_DIR ]]; then
    rm -r $BUILD_DIR
fi
MACHINE_BUILD_DIR=$BUILD_DIR/$DOCKER_MACHINE_NAME
mkdir -p $MACHINE_BUILD_DIR
BOOT_BUILD_DIR=$BUILD_DIR/boot
mkdir -p $BOOT_BUILD_DIR

# Configure this to customize generation of SSL certificates, keys, and CA
docker run --rm -v ${MACHINE_BUILD_DIR}:/certs \
    -e CA_EXPIRE=99999 \
    -e SSL_EXPIRE=99999 \
    -e SSL_SUBJECT=$DOCKER_MACHINE_IP \
    -e SSL_IP=$DOCKER_MACHINE_IP \
    -e SSL_DNS=$DOCKER_MACHINE_IP \
    paulczar/omgwtfssl

# Generate Docker Machine client configuration
cat > $MACHINE_BUILD_DIR/config.json << EOF
{
    "ConfigVersion": 3,
    "Driver": {
        "IPAddress": "$DOCKER_MACHINE_IP",
        "URL": "tcp://$DOCKER_MACHINE_IP:2376",  
        "MachineName": "$DOCKER_MACHINE_NAME",
        "SSHUser": "openremote",
        "SSHPort": 22,
        "SSHKeyPath": "",
        "StorePath": "$HOME/.docker/machine",
        "SwarmMaster": false,
        "SwarmHost": "",
        "SwarmDiscovery": "",
        "EnginePort": 2376,
        "SSHKey": ""
    },
    "DriverName": "generic",
    "HostOptions": {
        "Driver": "",
        "Memory": 0,
        "Disk": 0,
        "EngineOptions": {
            "ArbitraryFlags": [],
            "Dns": null,
            "GraphDir": "",
            "Env": [],
            "Ipv6": false,
            "InsecureRegistry": [],
            "Labels": [],
            "LogLevel": "",
            "StorageDriver": "",
            "SelinuxEnabled": false,
            "TlsVerify": true,
            "RegistryMirror": [],
            "InstallURL": "https://get.docker.com"
        },
        "SwarmOptions": {
            "IsSwarm": false,
            "Address": "",
            "Discovery": "",
            "Agent": false,
            "Master": false,
            "Host": "tcp://0.0.0.0:3376",
            "Image": "swarm:latest",
            "Strategy": "spread",
            "Heartbeat": 0,
            "Overcommit": 0,
            "ArbitraryFlags": [],
            "ArbitraryJoinFlags": [],
            "Env": null,
            "IsExperimental": false
        },
        "AuthOptions": {
            "StorePath": "$HOME/.docker/machine/machines/$DOCKER_MACHINE_NAME",
            "CertDir": "$HOME/.docker/machine/machines/$DOCKER_MACHINE_NAME",
            "CaCertPath": "$HOME/.docker/machine/machines/$DOCKER_MACHINE_NAME/ca.pem",
            "CaPrivateKeyPath": "$HOME/.docker/machine/machines/$DOCKER_MACHINE_NAME/ca-key.pem",
            "ServerCertPath": "$HOME/.docker/machine/machines/$DOCKER_MACHINE_NAME/cert.pem",
            "ServerKeyPath": "$HOME/.docker/machine/machines/$DOCKER_MACHINE_NAME/key.pem",
            "ClientCertPath": "$HOME/.docker/machine/machines/$DOCKER_MACHINE_NAME/cert.pem",
            "ClientKeyPath": "$HOME/.docker/machine/machines/$DOCKER_MACHINE_NAME/key.pem",
            "CaCertRemotePath": "",
            "ServerCertRemotePath": "",
            "ServerKeyRemotePath": "",
            "ServerCertSANs": []
        }
    },
    "Name": "$DOCKER_MACHINE_NAME"
}
EOF

# Prepare the files for the boot SD card partition
mkdir -p $BOOT_BUILD_DIR/dockerssl
cp $MACHINE_BUILD_DIR/*.pem $BOOT_BUILD_DIR/dockerssl
cp $SSH_PUBKEY $BOOT_BUILD_DIR/openremote-ssh.pub
cp $DIR/src/* $BOOT_BUILD_DIR
sed -i '' "s/MY_HOSTNAME/$DOCKER_MACHINE_NAME/g" $BOOT_BUILD_DIR/device-init.yaml
sed -i '' "s/MY_SSID/$WIFI_SSID/g" $BOOT_BUILD_DIR/device-init.yaml
sed -i '' "s/MY_PASSWORD/$WIFI_PASSWORD/g" $BOOT_BUILD_DIR/device-init.yaml

echo "Setup files generated, next steps:"
echo "1. Download HypriotOS SD image and write it to an SD card (e.g. using Etcher): https://blog.hypriot.com/downloads/"
echo "2. Mount the SD card and copy files to boot partition: $BOOT_BUILD_DIR"
echo "3. Copy Docker Machine configuration from $MACHINE_BUILD_DIR into $HOME/.docker/machine/machines/$DOCKER_MACHINE_NAME"
