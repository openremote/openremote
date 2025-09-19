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

## TL;DR

The following steps use the first option, managing connections through HAProxy.

### Create the Persistent Volumes and the required secrets

This is performed via the `or-setup` helm chart.
Review the `values.yaml` file under the `or-setup` folder and create one with values appropriate for your environment. 

You should certainly edit the `basePath` value to point to a folder on your local machine.  
Under macOS, it needs to be located under your home folder (/Users/xxx/...).  

The `openremote-secrets.yaml` file under `or-setup/templates` creates a secret to hold sensitive configuration from OpenRemote
(database username and password, Keycloak admin password).  
The values in this file are the default ones, update as required.

```bash
helm install or-setup or-setup -f your_values.yaml
```

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

### Persistent Volumes lifecycle

Uninstalling the charts will delete the Persistent Volume Claims but not the PVs.  
Those will go to the 'Released' state, at which point they can't be bound again.  
You need to manually transition them to 'Available' before you re-install the charts
```bash
kubectl patch pv manager-data-pv -p '{"spec":{"claimRef": null}}'
kubectl patch pv postgresql-data-pv -p '{"spec":{"claimRef": null}}'
```

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
For instance, this snippet of a values files defines a value for the KC_HOSTNAME_STRICT_HTTPS environment variable passed to the keycloak container.  
Important: the value always needs to be enclosed in double quotes, even for boolean or numeric values.  
```yaml
or:
  env:
    - name: KC_HOSTNAME_STRICT_HTTPS
      value: "false"
 ```

### Metrics

Both the manager and HAProxy expose Prometheus metrics.  
By defaults the metrics are exposed on a dedicated ClusterIP service.  
The manager configuration has 2 different flags that related to metrics:
- `or.metricsEnabled` indicating if the manager container exposes metrics
- `service.metrics.enabled` indicating if a metrics service for the manager should be exposed  
  This is only effective if the manager exposes metrics i.e. both flags must be true for the service to be created.

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
FROM openremote/manager:1.7.0

RUN mkdir -p /deployment/manager/extensions
ADD ./build/image /deployment
```

You can locally build a project specific image using
`docker buildx build --load -t openremote/myprj:1.7.0 -f Dockerfile .`

And use the following snippet in your manager values files
```yaml
image:
  repository: openremote/myprj
  tag: "1.7.0"
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
