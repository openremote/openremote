#!/bin/bash
# Fix host.docker.internal resolution
GATEWAY=$(ip route | grep default | awk '{print $3}')
if [ -n "$GATEWAY" ]; then
    if ! grep -q "$GATEWAY host.docker.internal" /etc/hosts; then
        echo "Setting host.docker.internal to $GATEWAY"
        echo "$GATEWAY host.docker.internal" >> /etc/hosts
    else
        echo "host.docker.internal already set to $GATEWAY"
    fi
fi
