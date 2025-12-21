# --- Provider Configuration ---

variable "hcloud_token" {
  description = "Hetzner Cloud API token. Can also be set via HCLOUD_TOKEN environment variable."
  type        = string
  sensitive   = true
}

variable "cloudflare_api_token" {
  description = "Cloudflare API token with permissions to edit DNS records."
  type        = string
  sensitive   = true
}

# --- Project and Host Configuration ---

variable "project_name" {
  description = "A name for your server and related resources."
  type        = string
  default     = "openremote-prod"
}

variable "host_fqdn" {
  description = "The fully qualified domain name for the host (e.g., demo.your-domain.com). This will be used for DNS."
  type        = string
}

variable "parent_dns_zone" {
  description = "The parent DNS zone managed in Cloudflare (e.g., your-domain.com)."
  type        = string
}

variable "ssh_public_key_path" {
  description = "Path to your public SSH key."
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "instance_type" {
  description = "Hetzner server type."
  type        = string
  default     = "cpx21" # 3 vCPU, 4 GB RAM
}

variable "data_disk_size" {
  description = "The size of the separate data volume in GB."
  type        = number
  default     = 20
}

variable "elastic_ip" {
  description = "If true, provisions and attaches a floating (elastic) IP to the server."
  type        = bool
  default     = true
}

# --- Backup Configuration ---

variable "provision_s3_bucket" {
  description = "If true, configures automated backups to a Hetzner Storage Box."
  type        = bool
  default     = true
}

variable "storage_box_host" {
  description = "The hostname of your Hetzner Storage Box (e.g., uXXXXXX.your-storagebox.de)."
  type        = string
  default     = ""
}

variable "storage_box_user" {
  description = "The username for your Hetzner Storage Box."
  type        = string
  default     = ""
}

variable "storage_box_password" {
  description = "The password for your Hetzner Storage Box."
  type        = string
  sensitive   = true
  default     = ""
}
