We're using the Zalando operator to deploy PostgreSQL pods.
```
helm repo add postgres-operator-charts https://opensource.zalando.com/postgres-operator/charts/postgres-operator

helm install postgres-operator postgres-operator-charts/postgres-operator
```

This uses a specific base image (spilo: high availability image that includes PostgreSQL and Patroni).
We thus need to base our own image on that instead of the timescaledb one (this spilo image also includes timescaledb).

The Dockerfile in this folder is currently a very limited version of the original OR postgresql Dockerfile.  
It will need to be updated for the final version but is enough for current testing.

Build the image using
`docker build . -t openremote/k8s-postgresql:1.0`

The Zalando operator uses a postgresql CRD to define the pods to run.
The postgresql-manifest.yaml files provides the configuration required for OR.

Install it using
`kubectl create -f postgresql-manifest.yaml`


Once the pods have been started, use
```
export PGMASTER=$(kubectl get pods -o jsonpath={.items..metadata.name} -l application=spilo,cluster-name=or-postgresql-cluster,spilo-role=master -n default)

kubectl port-forward $PGMASTER 5432:5432 -n default
```
to create a port forward for local access from the host machine.

Then use
```
export PGHOST=localhost
export PGPASSWORD=$(kubectl get secret postgres.or-postgresql-cluster.credentials.postgresql.acid.zalan.do -o 'jsonpath={.data.password}' | base64 -d)
export PGSSLMODE=require
psql -U postgres
```
to connect using psql

At this stage, you'll need to copy the value of $PGPASSWORD in the configuration for Keycloak and manager.  
This is obviously something to be changed for the release version.

To delete the pod, use
```
kubectl delete postgresql or-postgresql-cluster
```
