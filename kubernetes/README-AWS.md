# Kubernetes deployment under AWS (EKS)

## TL;DR

Requirements: you need to have kubectl, helm, [aws cli](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) and [eksctl](https://docs.aws.amazon.com/eks/latest/userguide/install-kubectl.html#eksctl-install-update) installed beforehand.

The scripts have been tested within the openremote account.  
You must get proper credentials and set the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN variable before running them.

The `eks-setup-haproxy.sh` script creates a new EKS cluster from scratch and deploys an OR stack in it.  
The `eks-cleanup-haproxy.sh` script deletes all the components that have been created by the above script.

The hostname is defined as an environment variable in the script, by default, it is testmanager.openremote.app  

See comments within the script for more information.

## Setup

We have two different options for deployment: using a HAProxy container or not.  
There are pros and cons to both approaches, see [Networking](#Networking) section for more details.

We're creating an EKS cluster with a managed node group.  
By default, the group uses 2 VMs of type t2.large. The script places them in AZ eu-west-1a and eu-west-1b

### Persistence

For persistence, the PVs are backed by [EBS volumes](https://docs.aws.amazon.com/eks/latest/userguide/ebs-csi.html).  
Those must reside in the same AZ as the node that will be using them.  
This means that pods must be deployed to specific nodes using node affinity.

#### PosgreSQL data directory

PostgreSQL wants the data folder to be empty (on first startup), but an empty ext4 EBS volume contains a lost+found folder.    
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
Make sure the `useSubPath` value is set to true.  
Pods within EKS running linux, `requiresPermissionsFix` must also be set to true to enable the init container (running as root) used to reset ownership and file permissions.
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

#### Using HAProxy

In this configuration, we deploy a HAProxy pod within the cluster.  
All traffic gets routed to HAProxy at layer 4 and HAProxy is responsible for TLS termination and management of all further routing to the other pods.  
This is similar to what is done when running under docker compose on a VM.

All OR pods only expose an ClusterIP service and never an Ingress.  
HAProxy exposes a LoadBalancer service for communication from the outside world.  
We're using [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/v2.7/) to automatically create a Network Load Balancer (NLB) based on that service.  
One NLB is automatically created when a (LoadBalancer) Service object exists and destroyed when the Service is deleted.

In this configuration, only MQTTS connectivity is available, not MQTT.

#### Using Ingresses and individual LoadBalancer services

TL;DR: Use scripts `eks-setup.sh` and `eks-cleanup.sh` for this configuration.

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

For MQTT(S), the manager uses two Load Balancer services (one for MQTT, one for MQTTS).  
This creates one Network Load Balancer per service.  
We need to use different domain names to route traffic to the appropriate NLB.  
We use subdomains for that, with prefix mqtt and mqtts respectively.

For MQTTS, we use the same certificate as for the ingress.  
During certificate creation, we add a SAN for the mqtts subdomain.

#### Annotations

AWS LB Controller uses specific annotations on the Ingress and Service to configure the underlying infrastructure (ALB and NLB).  
See the values-eks.yaml files annotations fields for more information.

## Configuration

Some aspects of the setup are configurable.

### eks-common.sh script variables

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

## Running demo under EKS

### Building the OR demo image

Use ```./gradlew -PSETUP_JAR=demo clean installDist``` to build the controller with the demo setup code.

You can then push the image to the private ECR repository in our developer account with (credentials are for developer account): 
```bash
export AWS_ACCESS_KEY_ID="…"
export AWS_SECRET_ACCESS_KEY="…"
export AWS_SESSION_TOKEN="…"

aws ecr get-login-password --region eu-west-1| docker login --username AWS --password-stdin <or-developers account id>.dkr.ecr.eu-west-1.amazonaws.com

docker buildx build --no-cache --push --platform linux/amd64,linux/arm64 -t <or-developers account id>.dkr.ecr.eu-west-1.amazonaws.com/openremote/manager:demo manager/build/install/manager/
```

### Starting a cluster with the OR demo image

Modify the eks-setup-haproxy.sh script to start the manager with the appropriate configuration by replacing lines
```bash
helm install manager manager -f manager/values-haproxy-eks.yaml \
  --set-string or.hostname=$FQDN
```
with
```bash
helm install manager manager -f manager/values-haproxy-eks.yaml -f manager/values-demo.yaml \
  --set-string or.hostname=$FQDN
```

This values file does two things:
- get the demo image from the private ECR
- enable the demo setup

## Current limitations / explorations to do

Although the eksctl command reports a successful delete of the cluster, the delete operation is still in progress.    
Most of the time, it fails, not being able to delete the VPC.  
It is best to always check the CloudFormation stacks using the AWS Console and manually delete any leftovers.

We are not using EKS Auto Mode for now. When we want to move forward with EKS, we should investigate this option.

As indicated above, we're using EBS volumes and need to make sure the pod using them is on a node in the same AZ.  
This limits the possibility of distributing pods on the different worker nodes and negates
some of the automatic orchestration offered by k8s.  
At this stage, it's not critical as anyway the pods using volumes are the DB (which is a stateful set with a single pod)  
and the manager (which is not architected yet to support multiple replicas).  
We could explore using storage class and not explicit binding and/or using other volume types.  
We should anyway review the manager usage of persistent storage and limit the use of the file system for this purpose.

When moving to production usage, the shell script might be replaced with CloudFormation templates or using Terraform or similar tools.
