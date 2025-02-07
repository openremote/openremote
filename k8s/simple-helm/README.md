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
The values in this files are the default ones, update as required.

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

Uninstalling the charts will delete the Persistent Volume Claims but not the PVs.  
Those will go back the 'Released' state, at the point they can't be bound again.  
You need to manually transition them to 'Available' before you re-install the charts
```bash
kubectl patch pv manager-data-pv -p '{"spec":{"claimRef": null}}'
kubectl patch pv postgresql-data-pv -p '{"spec":{"claimRef": null}}'
```
