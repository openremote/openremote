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
  user_data   = file("${path.module}/.github/setup.sh")

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
