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

### Persistence

For persistence, the PVs are backed by EBS volumes.
Those must reside in the same AZ as the node that will be using them.
This means that pods must be deployed to specific nodes using node affinity.

#### PosgreSQL data directory

PostgreSQL wants the data folder to be empty (on first startup) but an empty ext4 EBS volumes contains a lost+found folder.  
This causes the following error to be reported
```
initdb: error: directory "/var/lib/postgresql/data" exists but is not empty
initdb: detail: It contains a lost+found directory, perhaps due to it being a mount point.
initdb: hint: Using a mount point directly as the data directory is not recommended.
Create a subdirectory under the mount point.
```

For this reason, the volume mount section of the PostgreSQL statefulset has been updated to use subPath
```yaml
          volumeMounts:
            - mountPath: /var/lib/postgresql/data
              subPath: psql-data
              name: postgresql-data
```

This is however causing several permission issues. To resolve those, an init container (running as root) is used to reset ownership and file persmissions.
```yaml
      initContainers:
        - name: psql-data-ownership
          image: alpine:latest
          command: ['sh', '-c', 'chown -R 70:70 /var/lib/postgresql/data && chmod -R 0750 /var/lib/postgresql/data']
          securityContext:
            runAsUser: 0
          volumeMounts:
            - mountPath: /var/lib/postgresql/data
              subPath: psql-data
              name: postgresql-data
```

### Networking

We're using [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/v2.7/) to automatically create an Application Load Balancer based on the Ingress manifests.  
An Application Load Balancer (ALB) is automatically created when an Ingress object exists in the cluster and destroyed when there are none.  

By default, each ingress creates its own ALB, using the [IngressGroup](https://kubernetes-sigs.github.io/aws-load-balancer-controller/latest/guide/ingress/annotations/#ingressgroup) feature
and the corresponding `alb.ingress.kubernetes.io/group.name` annotation on the Ingress allows to use a single ALB for multiple ingresses.

The ALB will automatically assign a public "Amazon" DNS name for access.  
We want to use our own DNS names (for ease of use, consistency but also to associate a TLS certificate with it).
For that, we use [Route 53](https://aws.amazon.com/route53/) and an alias record to directly route traffic to the load balancer, see [Routing traffic to an ELB load balancer - Amazon Route 53](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/routing-to-elb-load-balancer.html).  

To create the certificate, we use [AWS Certificate Manager](https://docs.aws.amazon.com/acm/latest/userguide/acm-overview.html).  
Domain ownership validation is performed via DNS record.


TODO: more info on annotations

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

