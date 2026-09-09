# Simple Helm charts

This folder contains Helm charts for each individual OR component to deploy within kubernetes.  
It supposes you have already installed the required tools on your machine:

- a Kubernetes installation, tests were performed with Docker Desktop on macOS.
- kubectl
- helm

There are three exposure modes: public HAProxy, Kubernetes Ingress, and
internal-only. Both public modes use a HAProxy Pod in each stack namespace.
With HAProxy exposure it is the public TLS endpoint; with Ingress exposure it
is a private gateway behind the Ingress controller. An Ingress controller must
be installed for the latter mode
([Nginx Controller](https://kubernetes.github.io/ingress-nginx/deploy/#quick-start)
was used during local testing).

This README file covers deployment on a local machine, for information on deploying into an EKS cluster on AWS, see README-AWS.md

For EKS with the OpenRemote-managed ExternalDNS controller, use
[`or-eks-stack`](or-eks-stack) for the end-to-end workflow. It keeps `or-stack`
portable while coordinating load balancer, Route 53, certificate, and HTTPS
readiness. HAProxy with ACME remains the default; explicit Ingress currently
accepts a validated existing ACM certificate, the externally managed shared
certificate configured on the EKS cluster, or a stack-owned managed ACM
certificate.

## Namespaced stack management

`or-stack` installs each OpenRemote stack into a namespace with the same name.
The Helm release names remain `or-setup`, `postgresql`, `keycloak`, and
`manager`, plus `proxy` in either public mode, inside every namespace. Existing
component service names therefore continue to work while namespaced resources
and persistent data remain independent.

The stack command provides namespace preparation, apply, inspection, credential
retrieval, uninstall, and explicit destruction operations. Target platform and
public exposure are separate choices. `--target` selects local or EKS storage
and cluster requirements; `--exposure` selects `haproxy`, `ingress`, or `none`.
HAProxy is the default exposure for both targets. DNS ownership is a separate,
portable choice: `--dns external-dns` declares the hostname to a compatible
cluster controller, while the default `--dns none` leaves it externally
managed.

An AWS-aware orchestrator can reserve and verify namespace ownership before it
creates external resources:

```bash
./or-stack prepare \
  --name stack-a \
  --kube-context cluster@eu-west-1 \
  --target eks \
  --exposure ingress \
  --dns external-dns
```

`prepare` creates only the namespace and its OpenRemote ownership/configuration
labels. It installs no Helm release, workload, Service, Ingress, Secret, or
PVC, and it performs no AWS or cluster-capability checks. Repeating it is safe
when the namespace has the matching stack label, target, and exposure. It
refuses an unrelated namespace or an attempt to change the stored target or
exposure. Normal users can go straight to `apply`; this separate operation is
primarily a lifecycle building block for higher-level tooling.

To install or upgrade a local stack using the current Docker Desktop, kind, or
kubeadm context:

```bash
./or-stack apply \
  --name stack-a \
  --kube-context docker-desktop \
  --target local \
  --http-port 8080 \
  --https-port 8443
```

The local target uses the cluster's default StorageClass and enables the
PostgreSQL volume permission init container required by Docker Desktop's
dynamically provisioned hostpath volumes.

The local HAProxy Service uses the cluster's `LoadBalancer` support and reports
the configured HTTPS port to Manager and Keycloak. On Docker Desktop, deploy a
second independent stack with another name and different host-facing ports:

```bash
./or-stack apply \
  --name stack-b \
  --kube-context docker-desktop \
  --target local \
  --http-port 9080 \
  --https-port 9443
```

The Managers are then available at `https://localhost:8443/manager` and
`https://localhost:9443/manager`, using the proxy's locally generated
certificates. Browser warnings for those local certificates are expected.
The HAProxy Pod readiness probe checks that the proxy can serve traffic;
`or-stack status` reports certificate material separately. Certificate
issuance must not gate Pod readiness because an HTTP ACME challenge needs the
Service to send traffic to that Pod.

For kind, kubeadm, or another cluster without a LoadBalancer implementation,
put this in the stack's `proxy.yaml` values override:

```yaml
service:
  http:
    type: ClusterIP
```

Apply with explicit local ports, then keep a port-forward running:

```bash
./or-stack apply \
  --name stack-a \
  --kube-context kind-openremote \
  --target local \
  --hostname localhost \
  --http-port 8080 \
  --https-port 8443 \
  --values-dir ./stacks/stack-a

kubectl --context kind-openremote --namespace stack-a \
  port-forward service/proxy 8080:8080 8443:8443
```

Select Kubernetes Ingress explicitly with `--exposure ingress`, or use
`--exposure none` for an internal-only stack. Ingress traffic terminates TLS at
the configured controller and then passes through a private per-stack HAProxy
gateway to Manager or Keycloak. A local Ingress installation and its TLS Secret
are user-managed. `or-stack` declares the TLS hostname; a controller may use its
default certificate when no `secretName` is provided. The selected target and
exposure are stored on the namespace; reapplying with a different choice is
rejected because changing public routing in place needs a deliberate migration.

Show resources belonging to one stack:

```bash
./or-stack status --name stack-a --kube-context docker-desktop
```

Each new stack receives a generated Manager administrator password and a
separate generated PostgreSQL password. Retrieve the Manager login at any time
with:

```bash
./or-stack credentials --name stack-a --kube-context docker-desktop
```

The Manager username is `admin`. The command deliberately does not display the
PostgreSQL credentials.

### Network isolation

Every stack installs a `NetworkPolicy` that selects all Pods in its namespace.
Inbound traffic is allowed from Pods in the same namespace and denied from
other namespaces. Outbound traffic remains unrestricted so Manager and
Keycloak can still use DNS and external integrations.

NetworkPolicy enforcement is provided by the cluster's CNI plugin, not by the
Kubernetes API. A cluster can accept and display the policy while ignoring it.
Docker Desktop, kind, and kubeadm installations must therefore use a
NetworkPolicy-capable CNI and be verified with a connectivity test.

The Docker Desktop cluster used during development accepted the policies but
did not enforce them: cross-namespace connections succeeded in both directions.
Consequently, deploying with `--target local` on Docker Desktop isolates names
and storage, but does not by itself guarantee network isolation. Treat that
environment as non-enforcing unless the connectivity test below proves
otherwise. For local enforcement testing, use a cluster configured with a
policy-capable CNI; for example, create a kind cluster with its default CNI
disabled and install Calico or Cilium.

With two running stacks, create a controller-managed test Pod in stack A:

```bash
kubectl create deployment network-policy-test \
  --namespace stack-a \
  --image busybox:1.36 \
  -- sleep 3600

kubectl rollout status deployment/network-policy-test \
  --namespace stack-a \
  --timeout 2m
```

Its own PostgreSQL service must be reachable:

```bash
kubectl exec --namespace stack-a deployment/network-policy-test -- \
  nc -z -w 3 postgresql.stack-a.svc.cluster.local 5432
```

The PostgreSQL service in stack B must time out or be rejected:

```bash
kubectl exec --namespace stack-a deployment/network-policy-test -- \
  nc -z -w 3 postgresql.stack-b.svc.cluster.local 5432
```

The same-stack command must succeed and the cross-stack command must fail. If
both commands succeed, as observed with the development Docker Desktop cluster,
the policies are installed but the cluster is not enforcing them. Remove the
test workload afterwards:

```bash
kubectl delete deployment network-policy-test --namespace stack-a
```

Set `networkPolicy.enabled: false` in a stack's `or-setup.yaml` only when
isolation is deliberately not required. `networkPolicy.additionalIngressFrom`
accepts additional Kubernetes `NetworkPolicyPeer` entries for trusted sources.
Ingress exposure permits external traffic only to port 8080 on the stack's
proxy Pod. HAProxy exposure permits external traffic only to ports 8080 and
8443 on that Pod. In both modes, the proxy reaches Manager and Keycloak through
the same-namespace rule, while direct access from other namespaces to Manager,
Keycloak, and PostgreSQL remains denied. `none` adds no public ingress rule.

`apply` accepts an optional `--values-dir`. Files named `or-setup.yaml`,
`postgresql.yaml`, `keycloak.yaml`, `manager.yaml`, and `proxy.yaml` in that
directory are applied after the selected target and exposure values.
Command-line hostname configuration is also available:

```bash
./or-stack apply \
  --name stack-a \
  --kube-context docker-desktop \
  --target local \
  --hostname stack-a.localhost \
  --http-port 8080 \
  --https-port 8443 \
  --values-dir ./stacks/stack-a
```

To choose initial credentials instead of generating them, put the following in
`or-setup.yaml` in that values directory:

```yaml
credentials:
  adminPassword: "replace-me"
  postgresqlUsername: "postgres"
  postgresqlPassword: "replace-me-too"
```

Avoid committing credential values to source control. These settings apply only
when the stack Secret is first created. A later `apply` preserves the existing
values and rejects a conflicting override because changing a Kubernetes Secret
alone does not rotate credentials inside an initialized PostgreSQL or Keycloak
database.

### Uninstalling or destroying a stack

Uninstall a stack's Helm releases while preserving its namespace, credentials,
PVCs, and data:

```bash
./or-stack uninstall \
  --name stack-a \
  --kube-context docker-desktop
```

The releases are removed in reverse dependency order, beginning with HAProxy
when present. Before uninstalling, `or-stack` refuses to continue if the stack
Secret or a chart-managed PVC does not carry Helm's `keep` resource policy.
Running `apply` again with the same stack name, context, target, exposure, and
ports restores the workloads with the retained credentials, data volumes, and
HAProxy ACME/certificate state.

Destroying a stack is the explicit data-purge operation:

```bash
./or-stack destroy \
  --name stack-a \
  --kube-context docker-desktop \
  --confirm stack-a
```

Destroy requires both exact-name confirmation and a matching
`openremote.io/stack` label on the namespace. It uninstalls remaining releases
and deletes the entire namespace, including every PVC and other namespaced
resource in it. Backing volume deletion then follows the PV's StorageClass
reclaim policy; Docker Desktop's default StorageClass and the OpenRemote EKS
StorageClass currently use `Delete`.

For the same reason, `apply` refuses to adopt a pre-existing namespace that
does not already have the matching stack label. This prevents an unrelated
namespace from later becoming eligible for stack destruction.

### DNS ownership

`or-stack` does not call Route 53 or any other DNS provider. With the default
`--dns none`, it adds no DNS metadata. With `--dns external-dns`, it places the
GA `external-dns.kubernetes.io/hostname` annotation and the
`openremote.io/managed-dns=true` selection label on exactly one resource:

- the proxy `Ingress` for ingress exposure;
- the proxy `LoadBalancer` Service for HAProxy exposure.

The cluster's ExternalDNS installation must watch `Ingress` and `Service`
sources and select that label. A higher-level orchestrator may also use
namespaced `DNSEndpoint` resources for records that do not derive their target
from an Ingress or Service, such as ACM validation CNAMEs. The managed EKS
facade uses that contract for stack-owned certificates, but `or-stack` itself
does not create such records. The managed EKS setup is documented in
[`cluster/eks/README.md`](cluster/eks/README.md). A bring-your-own controller on
another Kubernetes platform can use the same contract. ExternalDNS `0.22` or
later understands the GA annotation prefix by default; an older controller
must be configured with the matching annotation prefix.

DNS ownership requires public exposure and a fully qualified, non-local
hostname. For example:

```bash
./or-stack apply \
  --name stack-a \
  --kube-context cluster@eu-west-1 \
  --target eks \
  --hostname stack-a.example.com \
  --dns external-dns
```

The DNS mode is recorded in the stack namespace and shown by
`or-stack status`. Unlike target or exposure, it may be changed deliberately: reapply
with `--dns none` to remove the annotation and return the hostname to external
management. A TXT-registry controller using `sync` policy then removes only
records it owns. Wait for that reconciliation before creating a manual record
with the same name or destroying the cluster.

## TL;DR

The following manual steps deploy one stack in the current namespace using the
first access option, managing connections through HAProxy. Prefer `or-stack`
for namespaced multi-stack deployment.

### Create the required secrets

The `or-setup` chart creates `openremote-secret` to hold the PostgreSQL and
Manager administrator credentials. It generates passwords unless initial
values are supplied through the chart's `credentials` values.

```bash
helm install or-setup or-setup
```

For this legacy manual installation, retrieve the generated Manager password
directly from the Secret:

```bash
kubectl get secret openremote-secret \
  -o 'go-template={{index .data "admin-password" | base64decode}}{{"\n"}}'
```

The Manager and PostgreSQL charts create their own namespaced PersistentVolumeClaims.
By default, they use the cluster's default StorageClass for dynamic provisioning.
Set `persistence.storageClass` in each chart when the cluster has no default or
when a specific provisioner must be used.

### Install the charts

Install the different charts in order

```bash
helm install postgresql postgresql
helm install keycloak keycloak
helm install manager manager
helm install proxy proxy
```

If running under linux, you must enable the requiresPermissionsFix flag when installing postgresql

```bash
helm install postgresql postgresql --set requiresPermissionsFix=true
```

## Caveats

### Persistent storage lifecycle

Manager, PostgreSQL, and HAProxy PVCs have the `helm.sh/resource-policy: keep`
annotation by default. HAProxy mounts its claim at `/deployment`, the proxy
image's location for Certbot account data and generated certificates. Replacing
the Pod or uninstalling/reapplying the release therefore does not needlessly
request a new certificate. Set `persistence.retain=false` if Helm should delete
a chart-managed claim during uninstall.

Set `persistence.existingClaim` to use a PVC managed outside the chart. When it
is set, the chart does not create, annotate, or delete that claim.

The proxy can instead load full-chain PEM files (including their private keys)
from a Secret by setting `certificate.existingSecret`. Secret keys are mounted
as files under `/data/proxy/certs`; manage and rotate that Secret outside this
chart. ACME is disabled when this setting is present. If no proxy-managed state
is required, also disable its PVC:

```yaml
certificate:
  existingSecret: stack-certificate
persistence:
  enabled: false
```

Deleting a retained PVC or its namespace is an explicit data-purge operation.
Whether the backing volume is also deleted then depends on its StorageClass
reclaim policy.

### Release names

Several object names are constructed based on the release name.  
The provided values files are based on the release name as shown above.  
If you use a different release name, this will impact the name of certain objects and require you to adapt the values accordingly.  
For instance, using psql instead of postgresql as the release name will modify the service name from postgresql to psql-postgresql.  
This service name is used in both the keycloak and manager values files to define the database hostname.

### Resources (requests and limits)

The values files do not define any values for memory and CPU requests or limits.  
Although this works for local development work, it is strongly recommended to fix values for both when deploying to production.  
Look at the commented `resources` section in the values files and provide actual values matching your deployment scenario in your custom values files.

### Duplicated configuration

Some configuration information (e.g. the database name) is duplicated between the values files of the different charts.  
This is a conscious decision at this time to keep the charts independent.
We'll be looking into improving on this in the future ([Have a mechanism to deploy the complete OR stack in a single operation · Issue #1651 · openremote/openremote](https://github.com/openremote/openremote/issues/1651))

That being said, we are using a single Opaque secret to contain all secure information applicable to all charts instead of multiple kubernetes.io/basic-auth secrets for individual credentials.

## Additional information

### Environment variables

The most important configuration information has been exposed in the helm values files.  
It is sometimes necessary to provide more configuration to the containers.  
This is done through environment variables, as it was done before with docker compose.  
In the values files, there is `or.env` property available to define any environment variable that will get passed to the container.  
For example the following sets `MY_VARIABLE=false` (NOTE: the value always needs to be enclosed in double quotes, even for boolean or numeric values).

```yaml
or:
  env:
    - name: MY_VARIABLE
      value: "false"
```

### Logging configuration

The manager chart supports 3 modes for logging configuration:

- no logging override in the values file: the manager uses the built in `logging.properties` or `logging-dev.properties` depending on `or.devMode`
- `logging.config`: provide the full content of a custom `logging.properties` file directly in the values file
- `logging.existingConfigMap`: reference an existing `ConfigMap` that contains a custom `logging.properties` entry

Example using an inline logging configuration:

```yaml
logging:
  config: |
    handlers=java.util.logging.ConsoleHandler
    .level=INFO
```

Example using an existing `ConfigMap`:

```yaml
logging:
  existingConfigMap: "my-manager-logging"
  existingConfigMapKey: "logging.properties"
  restartToken: "v1"
```

When `logging.config` is used, Helm creates a `ConfigMap` and adds a checksum annotation on the manager pod template.  
Changing the logging configuration in the values file and running `helm upgrade` updates that checksum and causes the Deployment to roll out a new pod.

When `logging.existingConfigMap` is used, Helm mounts the referenced `ConfigMap` but does not track changes made directly to it.  
If you edit that `ConfigMap` with `kubectl edit cm ...`, Kubernetes does not restart the pod because the Deployment specification has not changed.  
To trigger a restart through Helm after changing the external `ConfigMap`, bump the `logging.restartToken` value and run `helm upgrade`.
`existingConfigMapKey` allows the manager chart to work with whatever structure that existing `ConfigMap` has.  
One handy use of this is defining multiple logging configuration in one `ConfigMap` under different keys and easily switching between them.

Also note that the manager reads the logging configuration only once during startup.  
Kubernetes may refresh the mounted `ConfigMap` file on disk after a short delay, but the running JVM does not automatically reload the logging settings.  
In practice, any change to the logging configuration requires a pod restart to take effect.

### Metrics

Both the manager and HAProxy expose Prometheus metrics.  
By defaults the metrics are exposed on a dedicated ClusterIP service.  
The manager configuration has 2 different flags that related to metrics:

- `or.metricsEnabled` indicating if the manager container exposes metrics
- `service.metrics.enabled` indicating if a metrics service for the manager should be exposed  
  This is only effective if the manager exposes metrics i.e. both flags must be true for the service to be created.

### OpenTelemetry tracing

The manager image contains a pinned OpenTelemetry Java agent, but does not attach it unless
`or.otel.enabled` is explicitly set to `true`. Configure the agent through `or.otel` in an environment-specific
values file. For example, if Alloy is exposed by the `alloy-otel` service in the `observability` namespace:

```yaml
or:
  otel:
    enabled: true
    serviceName: openremote-manager
    endpoint: http://alloy-otel.observability.svc.cluster.local:4318
    protocol: http/protobuf
```

Replace the service name and namespace with those of the local Alloy deployment. No Tempo credentials belong in
the manager configuration: Alloy is the local OTLP endpoint and is responsible for authenticated forwarding.
Additional OpenTelemetry Java agent settings, such as sampling, can be supplied through `or.env`; environment
variables managed by `or.otel` must not be duplicated there when tracing is enabled. See
the [top-level OpenTelemetry tracing documentation](../README.md#opentelemetry-tracing) for validation and data-safety
guidance.

### JMX

The manager can optionally (it is disabled by default) expose a service to provide JMX access.  
Check the `service.jmx` section of the values files.

Enabling the service does not configure the manager for JMX access, this requires passing additional configuration flags to the JVM.  
You can for instance add the following section to your values files

```yaml
or:
  env:
    - name: JAVA_TOOL_OPTIONS
      value: "-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=8085 -Dcom.sun.management.jmxremote.rmi.port=8085 -Djava.rmi.server.hostname=localhost"
```

Note that the hostname is set to localhost. If you're using a ClusterIP service (as configured by default)
and port forwarding, this is the hostname you need to use for the JMX configuration.

### When using HAProxy

#### Accessing MQTT

MQTT(S) is intentionally disabled in the current namespaced exposure profiles.
It will be added as a separate option with its DNS, certificate, load-balancer,
and NetworkPolicy lifecycle handled together.

#### Using with IDE for development

Both public exposure modes now use a HAProxy Pod and expect Manager to be
available through its namespace-local Service. To run Manager in the IDE, use
the direct development workflow and port-forward its Kubernetes dependencies;
do not apply either managed public exposure profile.

#### Certificate management

HAProxy is responsible for certificate management (either using the default self-signed or using ACME).  
Please refer to the proxy documentation for more information.

### When using Ingress

Ingress mode still deploys a HAProxy Pod, but it has a narrower role than in
HAProxy exposure mode:

```text
Client -> Ingress controller -> per-stack HAProxy gateway -> Manager/Keycloak
```

The Kubernetes Ingress targets the proxy Service, never the Manager or
Keycloak Services. TLS terminates at the Ingress controller, ACME and
certificate persistence are disabled in the proxy, and HAProxy receives plain
HTTP before routing `/auth` to Keycloak and other paths to Manager.

This gateway is required for NetworkPolicy isolation. An external Ingress data
plane such as an AWS ALB has no Pod or namespace labels that a portable
NetworkPolicy can select. Allowing it to connect directly to Manager and
Keycloak would also allow Pods in other namespaces to use those same ports. A
single public rule on the proxy keeps the application Pods namespace-private.

The tradeoff is one additional internal HTTP hop and one small proxy Pod per
stack. In return, Ingress mode retains controller-managed TLS and layer-7
features. On EKS that means ACM, a shared ALB, SNI, HTTP redirects, and optional
ALB integrations such as WAF. It is not a load-balancer cost optimization:
once MQTT(S) is implemented, an Ingress stack will also require its own NLB for
MQTT in addition to the shared web ALB.

Apply this mode through `or-stack` so the proxy, its Ingress, and its
NetworkPolicy are configured consistently:

```bash
./or-stack apply \
  --name stack-a \
  --kube-context <context> \
  --target local \
  --exposure ingress \
  --hostname stack-a.example.com
```

#### Accessing MQTT

The HTTP Ingress does not carry MQTT traffic. MQTT(S) remains disabled pending
the dedicated per-stack NLB implementation. A development-only plaintext MQTT
connection can still use a manual port-forward to Manager port 1883.

#### Running a custom project

Unlike what's done with docker compose, it's not (yet) possible to mount the custom project specific resources via an image
in a pod running an otherwise standard OpenRemote controller image.  
The way to a run custom project under kubernetes is to create a project specific image, with the project specific resources baked in.  
This is easily achieved by modifying the Dockerfile for the custom project to use openremote/manager:\<version>
instead of alpine:latest as its base docker image.

Once this image is created, use the image.repository and image.tag entries in your values file to reference the desired image.

##### Example

Having the following Dockerfile in your `myprj` custom project deployment folder

```dockerfile
FROM openremote/manager:1.8.1

RUN mkdir -p /deployment/manager/extensions
ADD ./build/image /deployment
```

You can locally build a project specific image using
`docker buildx build --load -t openremote/myprj:1.8.1 -f Dockerfile .`

And use the following snippet in your manager values files

```yaml
image:
  repository: openremote/myprj
  tag: "1.8.1"
```

to have the pod run the built image.

##### Additional information

Also see the "Running demo under EKS" section in the README-AWS.md file for related information.

[Kubernetes Documentation - Use an Image Volume With a Pod](https://kubernetes.io/docs/tasks/configure-pod-container/image-volumes/)
is an upcoming kubernetes feature that would allow using a mechanism similar to what's currently done in docker compose
with kubernetes, using a separate image for project specific resources.  
It is however currently (Aug-2025) in beta and disabled by default.

#### Using with IDE for development

Install the postgresql and keycloak charts. For keycloak, use the specific values files to configure support for plain HTTP calls and set the HTTP port.

```bash
helm install postgresql postgresql
helm install keycloak keycloak -f keycloak/values-dev.yaml
```

Forward ports for direct access to postgresql and keycloak.

```bash
kubectl port-forward svc/postgresql 5432:5432
kubectl port-forward svc/keycloak 8081:8080
```

Make sure to start the manager with the `OR_KEYCLOAK_HOST` set to `localhost` (e.g. define the environment variable in your run configuration).

Make sure that other ports (e.g. MQTT 1883) are not used / forwarded from pods.

### Changing hostname / adding custom certificate

With `--exposure ingress`, the proxy chart owns the Ingress. Pass its hostname
to `or-stack` with `--hostname`. For a local controller, configure TLS through
the stack's `proxy.yaml` override. For example:

```yaml
ingress:
  hostname: test.openremote.io
  tls:
    - hosts:
        - test.openremote.io
```

Apply it as part of the complete stack:

```bash
./or-stack apply \
  --name stack-a \
  --kube-context <context> \
  --target local \
  --exposure ingress \
  --hostname test.openremote.io \
  --values-dir ./stacks/stack-a
```

If in addition to changing the hostname, you'd like to use a custom certificate for the TLS termination, you need to create a kubernetes secret with the certificate and reference it from the ingress definition.  
Supposing you have the private/public keys available in .PEM encoded files, you create the secret using

```
kubectl --namespace stack-a create secret tls openremote-web-tls \
  --key test.openremote.io.key \
  --cert test.openremote.io.crt
```

With that in place, you can now reference the secret in the ingress configuration.  
The above example file now becomes

```yaml
ingress:
  hostname: test.openremote.io
  tls:
    - hosts:
        - test.openremote.io
      secretName: openremote-web-tls
```

### Connecting to the database

Once the postgresql container is running, you can connect to it from your host using port forwarding.  
When using the above chart names for deployment, the name of the service is `postgresql` but you can always double-check by listing the available services.

```bash
kubectl get svc
kubectl port-forward svc/postgresql 5432:5432
```

## Guidelines
