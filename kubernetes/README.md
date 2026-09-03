# Simple Helm charts

This folder contains Helm charts for each individual OR component to deploy within kubernetes.  
It supposes you have already installed the required tools on your machine:

- a Kubernetes installation, tests were performed with Docker Desktop on macOS.
- kubectl
- helm

There are two options on how the OR components can be accessed:

- using a HAProxy pod to manage all connections
- using the standard kubernetes network objects: Ingress and Service

In the later case, an Ingress controller must be installed in the cluster ([Nginx Controller](https://kubernetes.github.io/ingress-nginx/deploy/#quick-start) was used during testing).
In the former case, an Ingress controller should NOT be installed as this causes conflicts.

This README file covers deployment on a local machine, for information on deploying into an EKS cluster on AWS, see README-AWS.md

## Namespaced stack management

`or-stack` installs each OpenRemote stack into a namespace with the same name.
The Helm release names remain `or-setup`, `postgresql`, `keycloak`, and
`manager` inside every namespace, so the existing service names continue to
work while namespaced resources and persistent data remain independent.

The stack command provides apply, inspection, credential retrieval, uninstall,
and explicit destruction operations. It installs the internal stack components
but intentionally does not configure a proxy, Ingress, certificate, DNS record,
or other external routing.

To install or upgrade a local stack using the current Docker Desktop, kind, or
kubeadm context:

```bash
./or-stack apply \
  --name stack-a \
  --kube-context docker-desktop \
  --target local
```

The local target uses the cluster's default StorageClass and enables the
PostgreSQL volume permission init container required by Docker Desktop's
dynamically provisioned hostpath volumes.

Deploy another independent stack by choosing another name:

```bash
./or-stack apply \
  --name stack-b \
  --kube-context docker-desktop \
  --target local
```

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
accepts additional Kubernetes `NetworkPolicyPeer` entries; it is reserved for
trusted sources such as an ingress-controller namespace when external routing
is added.

`apply` accepts an optional `--values-dir`. Files named `or-setup.yaml`,
`postgresql.yaml`, `keycloak.yaml`, and `manager.yaml` in that directory are
applied after the selected target values. Command-line hostname configuration
is also available:

```bash
./or-stack apply \
  --name stack-a \
  --kube-context docker-desktop \
  --target local \
  --hostname stack-a.localhost \
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

The releases are removed in reverse dependency order. Before uninstalling,
`or-stack` refuses to continue if the stack Secret or a chart-managed PVC does
not carry Helm's `keep` resource policy. Running `apply` again with the same
stack name and context restores the workloads with the retained credentials and
volumes.

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
helm install proxy proxy
helm install postgresql postgresql
helm install keycloak keycloak
helm install manager manager
```

If running under linux, you must enable the requiresPermissionsFix flag when installing postgresql

```bash
helm install postgresql postgresql --set requiresPermissionsFix=true
```

## Caveats

### Persistent storage lifecycle

Manager and PostgreSQL PVCs have the `helm.sh/resource-policy: keep` annotation
by default. Uninstalling either chart removes its workload but preserves its PVC
and data. Set `persistence.retain=false` if Helm should delete the chart-managed
claim during uninstall.

Set `persistence.existingClaim` to use a PVC managed outside the chart. When it
is set, the chart does not create, annotate, or delete that claim.

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

MQTTS is accessible on port 8883 through HAProxy.  
Non-TLS access to MQTT requires using a manual port forward to the pod e.g.  
`kubectl port-forward manager-…  1883:1883`

#### Using with IDE for development

It is not possible to run the manager inside the IDE and have HAProxy deployed as a pod in the cluster.  
Use an ingress instead for that scenario.

#### Certificate management

HAProxy is responsible for certificate management (either using the default self-signed or using ACME).  
Please refer to the proxy documentation for more information.

### When using Ingress (no HAProxy pod)

For this scenario, make sure an Ingress controller is installed in the cluster.  
Review the values files for your deployment scenario, in particular make sure to configure and enable the ingress on keycloak and manager pods.  
Install the different charts in order

```bash
helm install postgresql postgresql
helm install keycloak keycloak
helm install manager manager
```

#### Accessing MQTT

If, in the manager values files, you enable the MQTT/MQTTS services, you can directly access them (by default on port 1883 and 8883).  
**In this configuration, the MQTTS service does not have a certificate and although named MQTTS will only accept MQTT connections !!!**

Alternatively, if you do not enable a service, you can use manual port forwarding to the pod e.g.  
`kubectl port-forward manager-…  1883:1883`

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

The default values file creates the ingress on localhost and uses the default kubernetes fake certificate for its TLS termination.  
You can change your hostname by overriding the appropriate values, e.g. using a custom values files.  
Here is an example of a `values-openremote.yaml` file that you could use

```yaml
ingress:
  hosts:
    - host: test.openremote.io
      paths:
        - path: /
          pathType: Prefix
  tls:
    - hosts:
        - test.openremote.io

or:
  hostname: "test.openremote.io"
```

You can then deploy the manager using

```bash
helm install manager manager -f values-openremote.yaml
```

If in addition to changing the hostname, you'd like to use a custom certificate for the TLS termination, you need to create a kubernetes secret with the certificate and reference it from the ingress definition.  
Supposing you have the private/public keys available in .PEM encoded files, you create the secret using

```
kubectl create secret tls or-manager-tls --key test.openremote.io.key --cert test.openremote.io.crt
```

With that in place, you can now reference the secret in the ingress configuration.  
The above example file now becomes

```yaml
ingress:
  hosts:
    - host: test.openremote.io
      paths:
        - path: /
          pathType: Prefix
  tls:
    - hosts:
        - test.openremote.io
      secretName: or-manager-tls

or:
  hostname: "test.openremote.io"
```

### Connecting to the database

Once the postgresql container is running, you can connect to it from your host using port forwarding.  
When using the above chart names for deployment, the name of the service is `postgresql` but you can always double-check by listing the available services.

```bash
kubectl get svc
kubectl port-forward svc/postgresql 5432:5432
```

## Guidelines
