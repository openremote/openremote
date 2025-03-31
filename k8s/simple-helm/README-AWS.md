# Kubernetes deployment under AWS (EKS)

TODO: include links to appropriate documentation

## TL;DR

Requirements: you need to have kubectl, helm, aws cli and eksctl installed beforehand.

The scripts have been tested within the openremote account.  
You must get proper credentials and set the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN variable before running them.

The `eks-setup-haproxy.sh` script creates a new EKS cluster from scratch and deploys an OR stack in it.
The `eks-cleanup-haproxy.sh` script deletes all the components that have been created by the above script.

The hostname is defined as an environment variable in the the script, by default it is testmanager.openremote.app  

See comments within the script for more information.

## Setup

We have 2 different options for deployment: using a HAProxy container or not
There are pros and cons to both approaches, see [Networking](#Networking) section for more details.

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

This is however causing several permission issues. To resolve those, an init container (running as root) is used to reset ownership and file permissions.
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

Those additions have been causing issues when running locally (at least on macOS) and are thus conditional to the useSubPath value.

### Networking

#### Using HAProxy

In this configuration, we deploy a HAProxy pod within the cluster.
All traffic gets routed to HAProxy at layer 4 and HAProxy is responsible for TLS termination and management of all further routing to the other pods.
This is similar to what is done when running under docker compose on a VM.

All OR pods only expose an IPCluster service and never an Ingress.
HAProxy exposes a LoadBalancer service for communication from the outside world.
We're using [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/v2.7/) to automatically create a Network Load Balancer (NLB) based on that service.
One NLB is automatically created when a (LoadBalancer) Service object exists and destroyed when the Service is deleted.

#### Using Ingresses and individual LoadBalancer services

In this configuration, standard kubernetes Ingress and Load Balancer services are used
by each pod to expose their APIs to the outside world. 

We're using [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/v2.7/) to automatically create an Application Load Balancer based on the Ingress manifests.  
An Application Load Balancer (ALB) is automatically created when an Ingress object exists in the cluster and destroyed when there are none.  

By default, each ingress creates its own ALB, using the [IngressGroup](https://kubernetes-sigs.github.io/aws-load-balancer-controller/latest/guide/ingress/annotations/#ingressgroup) feature
and the corresponding `alb.ingress.kubernetes.io/group.name` annotation on the Ingress allows to use a single ALB for multiple ingresses.

The ALB will automatically assign a public "Amazon" DNS name for access.  
We want to use our own DNS names (for ease of use, consistency but also to associate a TLS certificate with it).
For that, we use [Route 53](https://aws.amazon.com/route53/) and an alias record to directly route traffic to the load balancer, see [Routing traffic to an ELB load balancer - Amazon Route 53](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/routing-to-elb-load-balancer.html).  

To create the certificate, we use [AWS Certificate Manager](https://docs.aws.amazon.com/acm/latest/userguide/acm-overview.html).  
Domain ownership validation is performed via DNS record.

For MQTTS, the manager uses a Load Balancer service.
This creates a Network Load Balancer with an alias different from the ALB one created above.
As such, it is not possible to 


test routing with a subdomain for mqtt




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



Some duplication in configuration
