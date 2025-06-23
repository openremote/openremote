# Hetzner’s Pipeline Documentation

The goal of this update is to replicate the pipeline process used by the OpenRemote team on AWS in Hetzner. This would enable the project to be deployed on two different cloud providers with the same level of functionality.

The current Hetzner pipeline successfully creates a new instance on trigger, attaches block storage to it, and deploys and runs the OpenRemote projects. This is the core of the pipeline, but more changes are needed to achieve an analogy with the AWS one — see the 'Pending Tasks' chapter for details.

## User Guide

The pipeline is triggered only manually from the project’s GitHub page.

Use the following steps to take:
1. Go to the GitHub repository
2. Navigate to the Actions tab
3. Select the Action “Deploy to Hetzner Cloud”
4. Click the button “Run workflow”
5. Choose from which branch to use the workflow and select a name for the instance(This automatically names the instance in the Hetzner Cloud Console and creates a volume associated with the instance, names it according to the chosen name, and attaches it)
6. Wait until the pipeline exits

## Requirements 

[ ✅ ] Cloud instances

[ ✅ ] Infrastructure-as-Code 

[ ✅ ] Block storage

[ ✅ ] Monitoring

[ ✅ ] Account provisioning

[ 🚫 ] Backups

[ 🚫 ] Notifications and alarms

[ 🚫 ] Domains / DNS

## Tools
 
### Terraform 
A provisioning, IaC tool that creates instance and volume, attaches volume, runs cloud-init script, outputs IPv4 address upon success.

### Prometheus 
A system that collects metrics from services running on specific port (in this case the OpenRemote manager) and configures alerts based on thresholds.

### Grafana 
A service that collects data from a datasource (Prometheus) and uses that to display metrics in a dashboard with different types of graphs.

### Node exporter 
A Prometheus-based exporter that exposes hardware and OS-level metrics of Unix-based systems.

### Github workflow 
An on-trigger action that allows for the customization of the instance.
## Step-by-step workflow description

1. Checkout repository: It uses a Github service action/checkout that clones the repo, and it fetches the data of the most recent commit on the chosen branch pointed to by $GITHUB_REF
2. Set up Terraform: Downloads the Terraform CLI and adds it to the PATH variable
3. Terraform init: Prepares the directory to be able to use .tf files
4. Terraform apply: Starts the process of creating and provisioning the instance, while supplying them with a name for the instance and attached volume, and the API token for Hetzner. Also runs the cloud-init script, which pulls the docker-compose files and dashboard configurations and spins up the containers containing the proxy, OpenRemote manager and monitoring tools.
5. Get Terraform outputs: Outputs the public IP of the instance and returns it so that it can be accessed
Wait for the server to be ready: Periodically pings the service until it returns a valid HTTP response and then exits the pipeline.


## Known Issues

### Moving an instance to another project blocks ports 25 and 465

The current pipeline implementation aims to create multiple instances within the OpenRemote parent project. However, when a client prefers to manage their instance independently, a transfer is necessary to achieve this.

Upon a transfer request, Hetzner informs that ports 25 and 465 will be blocked until the next billing period to prevent abuse. Fortunately, the OpenRemote project uses port 587 for this purpose, which remains unblocked, so email functionality should not be affected.

For more information, please refer to the website:
https://docs.hetzner.com/cloud/servers/faq/#why-can-i-not-send-any-mails-from-my-server


### Projects are limited to 3 instances by default
As discussed, the current pipeline implementation creates new instances within a single parent project. This is because there’s no API to create or manage new projects, and it’s not possible through Terraform.

Hetzner has quite strict limits out of the box, allowing only a few instances per project to prevent abuse. Fortunately, the limit can be extended by describing the use case and proposing a new limit to Hetzner.

For more information, refer to the following link:
https://console.hetzner.cloud/limits

## Pending Tasks

The Requirements chapter provides a good overview of the features currently missing from the pipeline. Here, we will take a closer look at the most significant ones.

### Backups

Backups are an essential part of the project. They should be stored on block storage attached to the instance and have an automated schedule. They should include logs, application files and configurations.

### Alerts & Emails

Alerts and emails are closely linked. To achieve this, an email server needs to be set up automatically and the alerts must be integrated into Prometheus.