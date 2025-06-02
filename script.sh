#!/bin/bash
echo "test"

apt-get update
apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    git

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

systemctl enable docker
systemctl start docker

usermod -aG docker root

mkdir -p /opt/openremote
cd /opt/openremote

curl -o docker-compose.yml https://raw.githubusercontent.com/openremote/openremote/master/docker-compose.yml

docker compose pull

OR_HOSTNAME=$(hostname -I | awk '{print $1}') docker compose -p openremote up -d 