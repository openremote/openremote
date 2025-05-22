terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.43.0" # Use the latest version
    }
  }
}

variable "hcloud_token" {
  description = "Hetzner Cloud API token"
  type        = string
}

provider "hcloud" {
  token = var.hcloud_token
}

resource "hcloud_volume" "openremote_data" {
  name     = "openremote-data"
  size     = 10
  location = "nbg1"
  format   = "ext4"
}

resource "hcloud_server" "openremote" {
  name        = "openremote-hetzner"
  server_type = "cx22"   # Change to your desired instance type
  image       = "ubuntu-22.04" # OS image
  location    = "nbg1"   # Data center (e.g., nbg1, fsn1, hel1)
  ssh_keys    = []
  user_data   = <<-EOF
    #!/bin/bash

    apt-get update
    apt-get install -y \
        apt-transport-https \
        ca-certificates \
        curl \
        gnupg \
        lsb-release \
        git \
        netcat

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
  EOF

  lifecycle {
    create_before_destroy = true
  }
}

resource "hcloud_volume_attachment" "openremote_volume" {
  volume_id = hcloud_volume.openremote_data.id
  server_id = hcloud_server.openremote.id
  automount = true
}

output "server_ip" {
  value = hcloud_server.openremote.ipv4_address
}
