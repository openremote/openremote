# Kubernetes deployment under AWS (EKS)

TODO: include links to appropriate documentation
TODO: see if can make hostname configurable

## TL;DR

Requirements: you need to have kubectl, helm, aws cli and eksctl installed beforehand.

The scripts have been tested within the openremote account.  
You must get proper credentials and set the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN variable before running them.

The `eks-setup.sh` script creates a new EKS cluster from scratch and deploys on OR stack in it.
The `eks-cleanup.sh` script deletes all the components that have been created by the above script.

At this stage, the hostname is hardcoded to k8stest.openremote.app  

See comments within the script for more information.

## Setup

We're creating an EKS cluster with a managed node group.
By default, the group uses 2 VMs of type t2.large. The script places them in AZ eu-west-1a and eu-west-1b

For persistence, the PVs are backed by EBS volumes.
Those must reside in the same AZ as the node that will be using them.
This means that pods must be deployed to specific nodes using node affinity.

TODO: voume not empty, subPath, init container

We're using AWSLoadBalancer Controller to automatically create an Application Load Balancer based on the Ingress manifests.  
An Application Load Balancer is automatically created when an Ingress object exists in the cluster and destroyed when there are none.  

TODO: talk about LB Group

TODO: certificate mgt / DNS
annotation

## Configuration

Some aspects of the setup are configurable.

### eks-setup.sh script variables

The script contains several variables that can be changed.

### cluster.yaml

The cluster creation call uses a configuration file.  
This configuration file defines how the cluster is created.  
See [Creating and managing clusters - eksctl](https://eksctl.io/usage/creating-and-managing-clusters/),   
[Config File Schema - eksctl](https://eksctl.io/usage/schema/) and 
[eksctl/examples at main · eksctl-io/eksctl](https://github.com/eksctl-io/eksctl/tree/main/examples) 
for more information.

### values files

In addition to the default values, values-eks.yaml files are used for the chart deployments.   
These contain EKS specific configuration that can be adapted or complemented if required.

## Current limitations / explorations to do

Not using EKS Auto Mode for now, see if interesting for us

Using EBS volumes -> need to make sure the pod using them is on a node in the same AZ
this limits the possibility of distributing pods on the different worker nodes and negates
some of the automatic orchestration offered by k8s

Can we make it more dynamic using storage class and not explicit binding

