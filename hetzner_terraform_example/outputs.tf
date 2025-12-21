output "instance_ip_address" {
  description = "The public IPv4 address of the OpenRemote server."
  value       = local.dns_ip
}

output "instance_fqdn" {
  description = "The fully qualified domain name of the OpenRemote server."
  value       = var.host_fqdn
}

output "ssh_command" {
  description = "Command to SSH into the server using your local key."
  value       = "ssh -i ${replace(var.ssh_public_key_path, ".pub", "")} root@${local.dns_ip}"
}
