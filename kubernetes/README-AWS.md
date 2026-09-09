# Kubernetes deployment under AWS (EKS)

## TL;DR

Requirements: you need to have kubectl, helm, jq, curl, dig,
[aws cli](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html),
and [eksctl](https://docs.aws.amazon.com/eks/latest/userguide/install-kubectl.html#eksctl-install-update)
installed beforehand.

Cluster-only lifecycle management is available through
[`or-eks-cluster`](or-eks-cluster). It creates, inspects, reconciles shared
add-ons, and destroys the shared EKS cluster without deploying or deleting an
OpenRemote stack. See [`cluster/eks/README.md`](cluster/eks/README.md) for its
configuration and safety model.

[`or-eks-stack`](or-eks-stack) is the normal stack command for an EKS cluster
with OpenRemote-managed ExternalDNS. Its default HAProxy workflow coordinates
the stack NLB and ACME certificate. Its explicit Ingress workflow validates an
existing user-owned or cluster-configured shared ACM certificate, or creates
and retains a stack-owned managed ACM certificate, and coordinates the shared
ALB. Both delegate the Kubernetes installation to `or-stack`, wait for DNS to
point at the current load balancer, and verify the trusted Manager HTTPS
endpoint.

The `eks-setup*.sh` and `eks-cleanup*.sh` scripts below are the legacy combined
cluster-and-stack workflow. They will be split further as part of multi-stack
support and must not be used to remove one stack from a shared cluster.

The scripts have been tested within the openremote account.  
You must get proper credentials and set the AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN variable before running them.

The `eks-setup-haproxy.sh` script creates a new EKS cluster from scratch and deploys an OR stack in it.  
The `eks-cleanup-haproxy.sh` script deletes all the components that have been created by the above script.

The hostname is defined as an environment variable in the script, by default, it is testmanager.openremote.app

See comments within the script for more information.

## Setup

We support two public routing options: HAProxy exposed directly through a
per-stack NLB, or a private per-stack HAProxy gateway behind the shared ALB.
There are pros and cons to both approaches; see the [Networking](#networking)
section for more details.

We're creating an EKS cluster with a managed node group.  
By default, the group uses 2 VMs of type t2.large. The script places them in AZ eu-west-1a and eu-west-1b

### Persistence

For persistence, the component charts request dynamically provisioned
[EBS volumes](https://docs.aws.amazon.com/eks/latest/userguide/ebs-csi.html).
`or-eks-cluster` reconciles the `openremote-ebs` StorageClass, which uses the
EBS CSI driver, encrypted `gp3` volumes, and `WaitForFirstConsumer` binding.
The EBS volume is therefore created in the availability zone selected for its
pod; no fixed volume IDs or pod node-affinity rules are required.

The EKS values files select `openremote-ebs`. Manager and PostgreSQL PVCs are
retained by an ordinary Helm uninstall. Explicitly deleting a retained PVC
deletes its dynamically provisioned EBS volume because this StorageClass uses
the `Delete` reclaim policy.

### Namespaced stacks

After creating and configuring the shared cluster, the normal managed-DNS
HAProxy deployment is one command. HAProxy is the default exposure and creates
one internet-facing NLB per stack:

```bash
./or-eks-stack apply \
  --name stack-a \
  --cluster <cluster-name> \
  --region eu-west-1 \
  --profile <aws-profile> \
  --hostname stack-a.example.com
```

`or-eks-stack` verifies that the named cluster is active and that the selected
kubeconfig context points to that cluster. It also requires the non-dry-run,
OpenRemote-managed ExternalDNS release installed by `or-eks-cluster`, and
checks that its domain filter covers the requested hostname. The managed Route
53 controller and its cross-account IAM bootstrap are documented in
[`cluster/eks/README.md`](cluster/eks/README.md).

The portable `or-stack` command remains the lower-level interface for custom
DNS ownership, internal-only stacks, local Kubernetes, and custom orchestration.
For example, omit DNS automation on EKS with:

```bash
./or-stack apply \
  --name stack-a \
  --kube-context <cluster-name>@eu-west-1 \
  --target eks \
  --hostname stack-a.example.com \
  --dns none
```

The EKS target verifies that the EBS CSI driver and `openremote-ebs`
StorageClass exist. It also requires the Amazon VPC CNI NetworkPolicy
`PolicyEndpoint` CRD and node agent, confirming that native policy support is
configured. Public HAProxy and Ingress exposure also require the AWS Load
Balancer Controller, while `--exposure none` does not. The target selects
encrypted dynamically provisioned EBS storage for Manager and PostgreSQL.
HAProxy exposure additionally persists its certificate state; the private
Ingress gateway does not store certificates.

Each stack applies an ingress NetworkPolicy that allows traffic from its own
namespace and rejects traffic originating in other stack namespaces. Egress is
not restricted. In HAProxy mode, a second policy permits external traffic only
to the HAProxy Pod's HTTP and HTTPS ports; Manager, Keycloak, and PostgreSQL are
not directly public. In Ingress mode, the second policy permits the ALB to
reach only HTTP port 8080 on a private per-stack HAProxy gateway. That gateway
then reaches Manager and Keycloak through the same-namespace rule, so neither
application Pod is directly reachable from another stack namespace.
`or-eks-cluster create` enables NetworkPolicy on the managed VPC CNI add-on,
and `or-eks-cluster apply` reconciles that setting without changing the
installed add-on version. New clusters give the VPC CNI add-on a dedicated IAM
role with `AmazonEKS_CNI_Policy`; for existing add-ons, cluster reconciliation
aligns the `aws-node` ServiceAccount annotation with the role already recorded
by EKS.

In the low-level workflow, inspect the per-stack NLB address with:

```bash
kubectl --context <cluster-name>@eu-west-1 \
  --namespace stack-a \
  get service proxy \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}{"\n"}'
```

With `--dns external-dns`, the proxy Service declares the hostname and the
cluster controller points it at that address. The portable `or-stack` command
still makes no AWS calls. Without DNS ownership, create the record externally.
HAProxy manages its own TLS certificate and keeps Certbot account/certificate
data on its retained `proxy` PVC.

`or-eks-stack` automates the necessary ordering. It waits until the public
hostname and the current NLB hostname resolve to at least one common address,
so a stale record from an older deployment is not considered ready. It then
checks for a retained valid certificate, triggers `/entrypoint.sh add` only
when needed, and waits for a trusted response from the canonical
`/manager/` URL. Its default timeout is 20 minutes for each readiness phase.
The automated HTTP-01 flow requires public HTTP port 80; use `or-stack`
directly when a custom HTTP port is intentional.

When using `or-stack` directly, perform the ACME trigger after DNS is ready:

```bash
kubectl --context <cluster-name>@eu-west-1 \
  --namespace stack-a \
  exec deployment/proxy \
  --container proxy \
  -- /entrypoint.sh add stack-a.example.com

./or-stack status \
  --name stack-a \
  --kube-context <cluster-name>@eu-west-1
```

The proxy readiness probe and certificate status are deliberately separate.
The Pod must be ready and reachable for the HTTP ACME challenge before a
production certificate can be issued; `or-stack status` shows the certificate
issuer and expiry once managed or custom certificate material is present.

Use `--exposure none` for an internal-only EKS stack. It creates no proxy,
Ingress, ALB, or NLB.

#### Shared HTTPS routing with Ingress

Select Ingress explicitly to route web traffic through the shared OpenRemote
ALB and a private gateway in each stack namespace. An explicit existing
user-owned certificate remains available:

```bash
./or-eks-stack apply \
  --name stack-a \
  --cluster <cluster-name> \
  --region eu-west-1 \
  --profile <aws-profile> \
  --exposure ingress \
  --hostname stack-a.example.com \
  --certificate-mode existing \
  --certificate-arn arn:aws:acm:eu-west-1:123456789012:certificate/example
```

Before changing the stack, `or-eks-stack` verifies that the ACM ARN belongs to
the EKS account and region, the certificate has status `ISSUED`, and an exact
name or one-level wildcard covers the requested hostname. It also rejects the
future `openremote.io/managed-by=or-eks-stack` ownership tag in `existing`
mode. The certificate remains user-owned: apply only references it, while
uninstall and destroy never modify or delete it. The selected mode and ARN are
recorded as namespace annotations and reported by `or-eks-stack status`.

An externally managed certificate intended for multiple stacks can instead be
recorded once as cluster configuration:

```bash
./or-eks-cluster apply \
  --name <cluster-name> \
  --region eu-west-1 \
  --profile <aws-profile> \
  --shared-certificate-arn arn:aws:acm:eu-west-1:123456789012:certificate/example
```

The option only records the ARN on the `openremote-alb` IngressClass; it does
not create, tag, renew, or delete the certificate. Omitting the option on a
later cluster apply preserves the existing reference. Remove the reference,
without touching ACM, with `--clear-shared-certificate`.

Stacks then select it without repeating the ARN:

```bash
./or-eks-stack apply \
  --name stack-a \
  --cluster <cluster-name> \
  --region eu-west-1 \
  --profile <aws-profile> \
  --exposure ingress \
  --hostname stack-a.example.com \
  --certificate-mode shared
```

Mode `shared` may be omitted because it is the automatic Ingress fallback when
the cluster has a shared ARN. Each stack still validates the resolved
certificate's account, region, `ISSUED` status, hostname coverage, and
ownership before making stack changes. Its namespace records mode `shared` and
the resolved ARN. Uninstall and destroy never modify or delete it.

If the cluster has no shared certificate, omitted mode falls back to a
stack-owned managed certificate. Select it explicitly with:

```bash
./or-eks-stack apply \
  --name stack-a \
  --cluster <cluster-name> \
  --region eu-west-1 \
  --profile <aws-profile> \
  --exposure ingress \
  --hostname stack-a.example.com \
  --certificate-mode managed
```

Managed mode prepares the namespace first, then reuses its recorded ARN,
discovers a uniquely tagged certificate, or requests a new DNS-validated ACM
certificate. It stores every ACM validation CNAME in the namespace's
`acm-validation` `DNSEndpoint`; the cluster ExternalDNS controller publishes
those records through its restricted cross-account role. Certificate tags bind
ownership to `or-eks-stack`, the cluster, stack, and exact hostname. Workload
installation starts only after ACM reports `ISSUED`.

The AWS identity selected by `--profile` needs `acm:RequestCertificate`,
`acm:ListCertificates`, `acm:DescribeCertificate`, and
`acm:ListTagsForCertificate` in the EKS account, in addition to the EKS access
already required by the facade. Route 53 write permission remains confined to
the ExternalDNS roles; `or-eks-stack` does not use DNS-account credentials.

The CNAME resource and certificate are retained during `uninstall` so ACM can
renew the certificate and a later apply can reuse it. Managed stack destruction
is blocked in this increment; coordinated DNS and ACM deletion follows in the
next lifecycle increment. Existing and shared certificate destruction remains
unchanged.

Ingress mode additionally validates the shared `openremote-alb` IngressClass
and its enforced HTTPS redirect. ACM DNS validation can be hosted in another
account.

`or-eks-cluster` creates an `openremote-alb` IngressClass backed by an AWS Load
Balancer Controller `IngressClassParams` resource. It fixes the scheme to
`internet-facing`, uses VPC CNI Pod IPs as targets, and puts every matching
Ingress in the `openremote-stacks` group. It also enforces redirection from
HTTP to HTTPS. The controller therefore merges one proxy Ingress from each
stack namespace onto one ALB by default.

The resulting web path is:

```text
Client -> shared ALB (TLS/ACM) -> stack HAProxy gateway (HTTP) -> Manager/Keycloak
```

HAProxy uses its edge-terminated TLS configuration in this mode. It does not
request a certificate or expose its own HTTPS Service port; it routes `/auth`
to Keycloak and other paths to Manager.

The gateway is an intentional NetworkPolicy boundary. The external ALB has no
Kubernetes Pod or namespace labels, so a portable NetworkPolicy rule that lets
it target Manager and Keycloak directly also admits Pods from other namespaces
on those ports. Opening only the proxy port preserves direct cross-stack
isolation.

This design adds one internal HTTP hop and one small proxy Pod per stack. The
reason to select it is ALB-managed web TLS and layer-7 integration: ACM, SNI,
central HTTP redirects, and optional AWS features such as WAF. It does not
reduce the final load-balancer count. The planned MQTT(S) support requires one
additional NLB per ingress stack, while HAProxy exposure will reuse its existing
per-stack NLB.

Only namespaces carrying `app.kubernetes.io/part-of=openremote` may use this
class. `or-stack` applies that label when it creates or adopts a valid stack
namespace. This cluster-side restriction prevents unrelated namespaces from
joining the shared ALB merely by naming its group.

After applying the first stack, wait for the controller to publish the shared
ALB hostname:

```bash
kubectl --context <cluster-name>@eu-west-1 \
  --namespace stack-a \
  get ingress proxy \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}{"\n"}'
```

The stack Ingress declares its hostname and the cluster controller creates the
record that points to the ALB. The facade waits for the Ingress to publish its
ALB hostname, confirms that public DNS resolves to that current ALB, and then
verifies the trusted `/manager/` endpoint. The EKS target configures listeners
on ports 80 and 443, while `or-stack` adds the validated certificate supplied
through `--certificate-arn` to the stack's proxy Ingress.

Use `or-stack --exposure ingress` directly when DNS or endpoint readiness is
managed elsewhere. The portable command accepts the ARN but deliberately does
not inspect ACM or call Route 53.

Certificate annotations are merged across the shared IngressGroup, allowing
the ALB to use SNI when stacks use different certificates. A wildcard
certificate can instead be passed to every stack. The shared class owns the
HTTPS redirect so stacks cannot configure conflicting redirect behavior. See
the AWS Load Balancer Controller documentation for
[IngressClassParams](https://kubernetes-sigs.github.io/aws-load-balancer-controller/latest/guide/ingress/ingress_class/)
and [IngressGroup annotation behavior](https://kubernetes-sigs.github.io/aws-load-balancer-controller/latest/guide/ingress/annotations/#ingressgroup).

A stack can opt out of the default group through its `proxy.yaml` values. Set
`ingress.className: alb` and give the proxy Ingress a unique
`alb.ingress.kubernetes.io/group.name`, together with the desired `scheme` and
`target-type` annotations. Also add the `ssl-redirect` annotation because the
dedicated class does not inherit the shared class's redirect setting. That
creates a separate ALB group for that stack; the shared class remains the
default.

Use the same apply command with a different stack name to create another
namespace. For a default HAProxy stack managed through the facade, inspect it
with:

```bash
./or-eks-stack status \
  --name stack-a \
  --cluster <cluster-name> \
  --region eu-west-1 \
  --profile <aws-profile>
```

New stacks receive independently generated Manager and PostgreSQL passwords.
Retrieve a stack's Manager administrator login without exposing its database
credentials with:

```bash
./or-eks-stack credentials \
  --name stack-a \
  --cluster <cluster-name> \
  --region eu-west-1 \
  --profile <aws-profile>
```

Remove workloads while retaining their credentials and EBS-backed data with:

```bash
./or-eks-stack uninstall \
  --name stack-a \
  --cluster <cluster-name> \
  --region eu-west-1 \
  --profile <aws-profile>
```

Reapplying the stack with the same target and exposure reuses its retained
Secret and data PVCs. HAProxy exposure also reuses its retained certificate
PVC; Ingress gateways have no certificate PVC because TLS terminates at the
ALB. The selected target and exposure are stored as namespace labels; changing
either is rejected until a deliberate migration workflow is implemented. For
HAProxy and Ingress stacks using an `existing` or `shared` certificate,
explicitly delete the stack namespace, credentials, PVCs, and dynamically
provisioned EBS volumes with:

```bash
./or-eks-stack destroy \
  --name stack-a \
  --cluster <cluster-name> \
  --region eu-west-1 \
  --profile <aws-profile> \
  --confirm stack-a
```

The destroy command requires the matching stack namespace label and waits for
the `Delete` reclaim policy to remove the stack's PersistentVolumes. It
currently refuses a stack with a `managed` certificate so the retained
validation CNAME and certificate cannot be orphaned. Perform supported stack
cleanup before asking `or-eks-cluster` to destroy an otherwise empty cluster.
Once that cluster-level preflight succeeds, `or-eks-cluster` bypasses system
PodDisruptionBudgets during the final node drain so EKS add-ons cannot leave
cluster deletion waiting indefinitely.

Ingress supports `existing`, cluster-configured `shared`, and stack-owned
`managed` certificate modes. Managed ACM deletion remains the next facade
increment.

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
    command: ["sh", "-c", "chown -R 70:70 /var/lib/postgresql/data && chmod -R 0750 /var/lib/postgresql/data"]
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

The current namespaced HAProxy profile exposes HTTP and HTTPS only. Port 8883
is reserved but disabled until MQTT(S) hostname, certificate, policy, and
lifecycle behavior are implemented together.

#### Using Ingress with a per-stack gateway

In this configuration, each stack creates one proxy Ingress. The AWS Load
Balancer Controller merges those Ingresses into the shared ALB and targets the
corresponding private HAProxy gateway Pods by IP. Manager and Keycloak expose
only ClusterIP Services and are never direct ALB targets.

The shared `IngressClassParams` supplies the common
[IngressGroup](https://kubernetes-sigs.github.io/aws-load-balancer-controller/latest/guide/ingress/annotations/#ingressgroup)
name, public scheme, IP target type, and HTTPS redirect. Deleting the last
Ingress removes the shared ALB.

The ALB will automatically assign a public "Amazon" DNS name for access.  
We want to use our own DNS names (for ease of use, consistency but also to associate a TLS certificate with it).  
For that, we use [Route 53](https://aws.amazon.com/route53/) and an alias record to directly route traffic to the load balancer, see [Routing traffic to an ELB load balancer - Amazon Route 53](https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/routing-to-elb-load-balancer.html).

To create the certificate, we use [AWS Certificate Manager](https://docs.aws.amazon.com/acm/latest/userguide/acm-overview.html).  
Domain ownership validation is performed via DNS record.

The old combined scripts exposed MQTT(S) with additional Manager LoadBalancer
services. The namespaced `or-stack` workflow does not enable those services
yet. MQTT and MQTTS will be added as explicit options in a follow-up. Ingress
stacks will need a per-stack NLB in addition to the shared web ALB; HAProxy
stacks can reuse their existing NLB. Both paths must preserve the NetworkPolicy
model.

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

Use `./gradlew -PSETUP_JAR=demo clean installDist` to build the controller with the demo setup code.

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
  --set-string or.hostname=$FQDN --set-string image.repository=$AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/openremote/manager
```

This values file does two things:

- get the demo image from the private ECR
- enable the demo setup

## Current limitations / explorations to do

Although the eksctl command reports a successful delete of the cluster, the delete operation is still in progress.  
Most of the time, it fails, not being able to delete the VPC.  
It is best to always check the CloudFormation stacks using the AWS Console and manually delete any leftovers.

We are not using EKS Auto Mode for now. When we want to move forward with EKS, we should investigate this option.

The database is a single-pod StatefulSet, and the manager is not currently
architected to support multiple replicas. The manager's use of persistent file
storage should still be reviewed and reduced before production usage.

When moving to production usage, the shell script might be replaced with CloudFormation templates or using Terraform or similar tools.
