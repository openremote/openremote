terraform {
  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.42"
    }
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.4"
    }
  }
}

# --- PROVIDER CONFIGURATION ---
provider "hcloud" {
  token = var.hcloud_token
}

provider "cloudflare" {
  api_token = var.cloudflare_api_token
}

# --- NETWORKING ---
resource "hcloud_network" "private_net" {
  name     = "${var.project_name}-network"
  ip_range = "10.0.0.0/16"
}

resource "hcloud_network_subnet" "private_subnet" {
  network_id   = hcloud_network.private_net.id
  type         = "cloud"
  network_zone = "eu-central"
  ip_range     = "10.0.1.0/24"
}

resource "hcloud_firewall" "default" {
  name = "${var.project_name}-firewall"

  # Allow all outbound traffic by creating rules for major protocols
  rule {
    direction       = "out"
    protocol        = "tcp"
    port            = "any"
    destination_ips = ["0.0.0.0/0", "::/0"]
  }
  rule {
    direction       = "out"
    protocol        = "udp"
    port            = "any"
    destination_ips = ["0.0.0.0/0", "::/0"]
  }
  rule {
    direction       = "out"
    protocol        = "icmp"
    destination_ips = ["0.0.0.0/0", "::/0"]
  }

  # Allow inbound SSH, HTTP, HTTPS, and OpenRemote TCP port
  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "22"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }
  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "80"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }
  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "443"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }
  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "8883" # Standard MQTTs port used by OpenRemote
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }
}

# --- STORAGE ---
resource "hcloud_volume" "data_volume" {
  name     = "${var.project_name}-data"
  size     = var.data_disk_size
  location = "nbg1"
  format   = "ext4"
}

# --- SSH KEY ---
# Use the user's local public SSH key
resource "hcloud_ssh_key" "default" {
  name       = var.project_name
  public_key = file(pathexpand(var.ssh_public_key_path))
}

# --- FLOATING IP (ELASTIC IP) ---
resource "hcloud_floating_ip" "elastic_ip" {
  count           = var.elastic_ip ? 1 : 0
  name            = var.project_name
  type            = "ipv4"
  home_location   = "nbg1"
  description     = "Floating IP for ${var.project_name}"
}

# Determine the IP address to use for DNS. Use the floating IP if it exists, otherwise the server's primary IP.
locals {
  dns_ip = var.elastic_ip ? hcloud_floating_ip.elastic_ip[0].ip_address : hcloud_server.openremote_instance.ipv4_address
}

# --- SERVER INSTANCE ---
resource "hcloud_server" "openremote_instance" {
  name        = var.project_name
  server_type = var.instance_type
  image       = "ubuntu-22.04"
  location    = "nbg1"
  ssh_keys    = [hcloud_ssh_key.default.id]
  firewall_ids = [hcloud_firewall.default.id]
  
  network {
    network_id = hcloud_network.private_net.id
    ip         = "10.0.1.5"
  }

  # Use the built-in 'templatefile' function for robust server setup
  user_data = templatefile("${path.module}/cloud-init.yml", {
    hostname             = var.host_fqdn
    floating_ip          = var.elastic_ip ? hcloud_floating_ip.elastic_ip[0].ip_address : ""
    provision_s3_bucket  = var.provision_s3_bucket
    storage_box_host     = var.storage_box_host
    storage_box_user     = var.storage_box_user
    storage_box_password = var.storage_box_password
  })

  depends_on = [
    hcloud_network_subnet.private_subnet
  ]
}

# --- VOLUME ATTACHMENT ---
resource "hcloud_volume_attachment" "data_volume_attachment" {
  volume_id = hcloud_volume.data_volume.id
  server_id = hcloud_server.openremote_instance.id
  automount = true
}

# --- FLOATING IP ATTACHMENT ---
resource "hcloud_floating_ip_assignment" "main" {
  count          = var.elastic_ip ? 1 : 0
  floating_ip_id = hcloud_floating_ip.elastic_ip[0].id
  server_id      = hcloud_server.openremote_instance.id
}

# --- DNS CONFIGURATION (Cloudflare) ---
data "cloudflare_zone" "parent_zone" {
  name = var.parent_dns_zone
}

resource "cloudflare_record" "host_dns" {
  zone_id = data.cloudflare_zone.parent_zone.id
  name    = var.host_fqdn
  content = local.dns_ip
  type    = "A"
  ttl     = 300
  proxied = false # Set to true if you want to use Cloudflare's proxy/CDN features
}
