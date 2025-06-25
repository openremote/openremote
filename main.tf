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

variable "instance_name" {
  description = "Name of the instance"
  type        = string
}

variable "github_repository" {
  description = "GitHub repository (owner/repo)"
  type        = string
}

variable "github_ref" {
  description = "GitHub branch or tag reference"
  type        = string
}

variable "server_type" {
  description = "Hetzner server type"
  type        = string
}

variable "volume_size" {
  description = "Volume size in GB (10-1000)"
  type        = number
  default     = 10
}

variable "enable_monitoring" {
  description = "Enable monitoring stack?"
  type        = bool
  default     = true
}

provider "hcloud" {
  token = var.hcloud_token
}

resource "hcloud_volume" "openremote_data" {
  name     = "${var.instance_name}-vol"
  size     = var.volume_size
  location = "hel1"
  format   = "ext4"
}

resource "hcloud_server" "openremote" {
  name        = var.instance_name
  server_type = var.server_type
  image       = "ubuntu-22.04" # OS image
  location    = "hel1"   # Data center (e.g., nbg1, fsn1, hel1)
  ssh_keys    = [28907178, 28907172]
  user_data   = templatefile("${path.module}/cloud-init.yml", {
    github_repository = var.github_repository
    github_ref        = var.github_ref
    enable_monitoring = var.enable_monitoring
  })

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
