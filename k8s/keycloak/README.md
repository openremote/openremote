We're using the [Bitnami helm chart](https://github.com/bitnami/charts/blob/main/bitnami/keycloak/README.md) to run Keycloak

```
export PGPASSWORD=$(kubectl get secret postgres.or-postgresql-cluster.credentials.postgresql.acid.zalan.do -o 'jsonpath={.data.password}' | base64 -d)
helm install keycloak oci://registry-1.docker.io/bitnamicharts/keycloak -f values.yaml --set-string externalDatabase.password=$PGPASSWORD
```

Manually create the ingress to contact Keycloak using
`kubectl create -f keycloak-ingress.yaml`

The default user to log into keycloak is user, the password can be retrieved using
`kubectl get secret keycloak -o 'jsonpath={.data.admin-password}' | base64 -d`

To un-install:
`helm uninstall keycloak`
