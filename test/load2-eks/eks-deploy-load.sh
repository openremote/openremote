#!/bin/bash

if [ -z "$K_CONTEXT" ]; then
    echo "Error: K_CONTEXT environment variable is not set"
    echo "Please set it to the kubernetes context to use with: export K_CONTEXT=context"
    exit 1
fi

. ./eks-common.sh

kubectl --context $K_CONTEXT patch pv manager-data-pv -p '{"spec":{"claimRef": null}}'
kubectl --context $K_CONTEXT patch pv postgresql-data-pv -p '{"spec":{"claimRef": null}}'

helm install --kube-context=$K_CONTEXT postgresql $OR_KUBERNETES_PATH/postgresql -f $OR_KUBERNETES_PATH/postgresql/values-eks.yaml -f profiles/$OR_PROFILE/values-postgresql-eks-load.yaml

helm install --kube-context=$K_CONTEXT keycloak $OR_KUBERNETES_PATH/keycloak -f $OR_KUBERNETES_PATH/keycloak/values-haproxy.yaml \
  -f profiles/$OR_PROFILE/values-keycloak-eks-load.yaml --set-string or.hostname=$FQDN

helm install --kube-context=$K_CONTEXT manager $OR_KUBERNETES_PATH/manager -f $OR_KUBERNETES_PATH/manager/values-haproxy-eks.yaml \
  -f profiles/$OR_PROFILE/values-manager-eks-load.yaml --set-string or.hostname=$FQDN --set or.setupRunOnRestart=true \
  --set-string image.repository=$AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/openremote/manager
