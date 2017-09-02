#!/bin/bash -e

# Prepares setup for Pi64 OS

: ${SSH_PUBKEY:?"Please set the path to your SSH public key"}
: ${WIFI_SSID:?"Please set the Wifi network SSID"}
: ${WIFI_PASSWORD:?"Please set the Wifi password"}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Clean build directory
BUILD_DIR=$DIR/build
if [[ -d $BUILD_DIR ]]; then
    rm -r $BUILD_DIR
fi
BOOT_BUILD_DIR=$BUILD_DIR/boot
mkdir -p $BOOT_BUILD_DIR

# Prepare the files for the boot SD card partition
mkdir -p $BOOT_BUILD_DIR/openremote
cp $SSH_PUBKEY $BOOT_BUILD_DIR/openremote-ssh.pub
cp $DIR/src/* $BOOT_BUILD_DIR
sed -i '' "s/MY_SSID/$WIFI_SSID/g" $BOOT_BUILD_DIR/setup
sed -i '' "s/MY_PASSWORD/$WIFI_PASSWORD/g" $BOOT_BUILD_DIR/setup

echo "Setup files generated, next steps:"
echo "1. Download Pi64 SD image and write it to an SD card (e.g. using Etcher): https://github.com/bamarni/pi64/releases"
echo "2. Mount the SD card and copy files to boot partition: $BOOT_BUILD_DIR"
echo "3. Boot your Raspberry Pi3 with the SDCARD and connect: ssh pi@<IP of your device>"
