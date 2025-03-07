# Simple Helm charts

This folder contains Helm charts for each individual OR component to deploy within kubernetes.  
It has been tested under Docker Desktop on macOS.

## TL;DR

### Create the Persistent Volumes

Edit the *-pv.yaml files and update the spec.hostPath.path entry to point to a folder on your local machine.  
Under macOS, it needs to be located under your home folder (/Users/xxx/...).  
Create the PVs by applying the files
```bash
kubectl apply -f openremote-manager-pv.yaml
kubectl apply -f openremote-psql-pv.yaml
```
### Create the required secrets

The openremote-secrets.yaml creates a secret to hold sensitive configuration from OpenRemote
(database username and password, Keycloak admin password).  
The values in this file are the default ones, update as required.

```bash
kubectl apply -f openremote-secret.yaml
```

### Install the charts

Install the different charts in order
```bash
helm install postgresql postgresql
helm install keycloak keycloak
helm install manager manager
```

## Caveats

### Persistent Volumes lifecycle

Uninstalling the charts will delete the Persistent Volume Claims but not the PVs.  
Those will go back the 'Released' state, at the point they can't be bound again.  
You need to manually transition them to 'Available' before you re-install the charts
```bash
kubectl patch pv manager-data-pv -p '{"spec":{"claimRef": null}}'
kubectl patch pv postgresql-data-pv -p '{"spec":{"claimRef": null}}'
```

### Release names

Several objects name are constructed based on the release name.  
The provided values files are based on the release name as shown above.  
If you use a different release name, this will impact the name of certain objects and require you to adapt the values accordingly.  
For instance, using psql instead of postgresql as the release name will modify the service name from postgresql to psql-postgresql.  
This service name is used in both the keycloak and manager values files to define the database hostname.

### Resources (requests and limits)

The values file do not define any values for memory and CPU requests or limits.  
Although this works for local development work, it is strongly recommended to fix values for both when deploying to production.  
Look at the commented `resources` section in the values files and provide actual values matching your deployment scenario in your custom values files.  

### Duplicated configuration

Some configuration information (e.g. the database name) are duplicated between the values files of the different charts.  
This is a conscious decision at this time to keep the charts independent.
We'll be looking into improving on this in the future ([Have a mechanism to deploy the complete OR stack in a single operation · Issue #1651 · openremote/openremote](https://github.com/openremote/openremote/issues/1651)) 

That being said, we have gone of a single Opaque secret to contain all secure information applicable to all charts instead of multiple kubernetes.io/basic-auth secrets for individual credentials.  

## Additional information

### Running custom projects

With docker compose, custom projects use a thin image based on top of Alpine that contains the changes compared to a stock manager.  
This is then used to create a docker volume that can then be mounted on top of a stock manager image.  

The corresponding way to do this in Kubernetes would be to use Image Volumes, which at the moment (Feb 2025) is still an alpha feature, disabled by default.

The way to move forward now is to create a specific docker image for each custom project, with the changes baked in.  
That is, instead of using Alpine as the base image, use the OR manager image as the base. This is already done for Balena.  

In a custom project, look at the Dockerfile under deployment and make a copy.  
Then replace the first line
`FROM alpine:latest`
with
`FROM openremote/manager:1.3.3`
using the same version as the one used in gradle.properties

We can automate it using
`./gradlew properties -q | grep openremoteVersion | awk -F':' '{gsub(/ /, "", $0);print $2 }'`
to retrieve the version and passing it as variable to the Dockerfile.  


Once this image is built, it can be referenced from the manager helm chart by setting the image.repository and image.tag values,
either on the command-line or passing an additional values files with following content
```
image:
  repository: openremote/custom
  tag: "1.2.4"
```

### Environment variables

The most important configuration information has been exposed in the helm values files.  
It is sometimes necessary to provide more configuration to the containers.  
This is done through environment variables, as it was done before with docker compose.  
In the values files, there is `or.env` property available to define any environment variable that will get passed to the container.  
For instance, this snippet of values file defines a value for the KC_HOSTNAME_STRICT_HTTPS environment variable passed to the keycloak container.
```yaml
or:
  env:
    - name: KC_HOSTNAME_STRICT_HTTPS
      value: "false"
 ```

### Accessing MQTT

TODO: port forward or service for MQTT port(s)

### Using with IDE for development

Install the postgresql and keycloak charts. For keycloak, use the specific values files to configure support plain HTTP calls and set the HTTP port.
```bash
helm install postgresql postgresql
helm install keycloak keycloak -f keycloak/values-dev.yaml
```

Forward ports for direct access to database and keycloak.
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
