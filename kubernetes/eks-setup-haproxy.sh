#!/bin/bash

# Name of cluster, not exposed but must be unique within account
CLUSTER_NAME=testcluster

# Hostname to use for public access to this instance, always under the openremote.app domain
HOSTNAME=testmanager
FQDN=$HOSTNAME.openremote.app

AWS_REGION="eu-west-1"
AWS_ACCOUNT_ID="463235666115" # openremote

DNSCHG_ROLE_ARN="arn:aws:iam::134517981306:role/route53-full-access"

aws configure set region $AWS_REGION
aws configure --profile or set region $AWS_REGION
aws configure --profile or set output json
aws configure --profile dnschg set source_profile or
aws configure --profile dnschg set role_arn $DNSCHG_ROLE_ARN

if [ -n "$AWS_ACCESS_KEY_ID" ]; then
  aws configure --profile or set aws_access_key_id $AWS_ACCESS_KEY_ID
fi

if [ -n "$AWS_SECRET_ACCESS_KEY" ]; then
  aws configure --profile or set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
fi

if [ -n "$AWS_SESSION_TOKEN" ]; then
  aws configure --profile or set aws_session_token $AWS_SESSION_TOKEN
fi

aws sts assume-role --role-arn $DNSCHG_ROLE_ARN --role-session-name dnschg --profile or --query "Credentials.[AccessKeyId, SecretAccessKey, SessionToken]" --output text | awk -F'\t' '{print "aws_access_key_id "$1"\naws_secret_access_key "$2"\naws_session_token "$3 }' | xargs -L 1 aws configure --profile dnschg set

# TODO: --name $CLUSTER_NAME -> in cluster.yaml
# TODO: region is duplicated in cluster.yaml
eksctl create cluster -f cluster.yaml --profile or

# TODO: maybe extract cluster name after cluster creation so source of truth is in cluster.yaml, not other way around
# Not straightforward

# TODO: it might be possible to replace some of the operations below by configuration options in cluster.yaml
oidc_id=$(aws eks describe-cluster --profile or --name $CLUSTER_NAME --query "cluster.identity.oidc.issuer" --output text | cut -d '/' -f 5)

eksctl utils associate-iam-oidc-provider --profile or --cluster $CLUSTER_NAME --approve

eksctl create iamserviceaccount --profile or \
        --name ebs-csi-controller-sa \
        --namespace kube-system \
        --cluster $CLUSTER_NAME \
        --role-name AmazonEKS_EBS_CSI_DriverRole \
        --role-only \
        --attach-policy-arn arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy \
        --approve

eksctl create addon --profile or --cluster $CLUSTER_NAME --name aws-ebs-csi-driver --version latest \
    --service-account-role-arn arn:aws:iam::$AWS_ACCOUNT_ID:role/AmazonEKS_EBS_CSI_DriverRole --force

# [Installation Guide - AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/latest/deploy/installation/)

curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.11.0/docs/install/iam_policy.json

aws iam create-policy --profile or \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam_policy.json

eksctl create iamserviceaccount --profile or \
  --cluster=$CLUSTER_NAME \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --role-name AmazonEKSLoadBalancerControllerRole \
  --attach-policy-arn=arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve

# [Install AWS Load Balancer Controller with Helm - Amazon EKS](https://docs.aws.amazon.com/eks/latest/userguide/lbc-helm.html)

helm repo add eks https://aws.github.io/eks-charts

helm repo update eks

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=$CLUSTER_NAME \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller

PSQL_VOLUMEID=`aws ec2 create-volume --size 1 --availability-zone eu-west-1a --tag-specifications "ResourceType=volume,Tags=[{Key=Name,Value=psql-data}]" --query VolumeId`
MANAGER_VOLUMEID=`aws ec2 create-volume --size 1 --availability-zone eu-west-1a --tag-specifications "ResourceType=volume,Tags=[{Key=Name,Value=manager-data}]" --query VolumeId`

# Wait for AWS LB ctrl to be ready
kubectl rollout status deployment aws-load-balancer-controller -n kube-system --timeout=300s

CLUSTER_DNS=`kubectl get svc kube-dns -n kube-system -o jsonpath='{.spec.clusterIP}:{.spec.ports[?(@.name=="dns")].port}'`

helm install or-setup or-setup --set aws.enabled=true --set aws.managerVolumeId=$MANAGER_VOLUMEID --set aws.psqlVolumeId=$PSQL_VOLUMEID

helm install proxy proxy -f proxy/values-eks.yaml --set or.nameserver=$CLUSTER_DNS --set or.hostname=$FQDN

helm install postgresql postgresql -f postgresql/values-eks.yaml

# Waiting for the LB to be created
# AWS LB Controller only creates an Network LB if there's a service
# Following logic is assuming there is the only (network) LB in the account
while ! aws elbv2 describe-load-balancers  --profile or --query "LoadBalancers[?Type=='network']" 2>/dev/null | grep '"Code": "active"'; do
  echo "Waiting for load balancer to be created..."
  sleep 10
done

DNS_NAME=`aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?Type=='network'].DNSName | [0]"`
HOSTED_ZONE_ID=`aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?Type=='network'].CanonicalHostedZoneId | [0]"`

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "UPSERT", "ResourceRecordSet": { "Name": "'$FQDN'", "Type": "A", "AliasTarget":{ "HostedZoneId": '$HOSTED_ZONE_ID',"DNSName": '$DNS_NAME',"EvaluateTargetHealth": false} } } ]}' \
     --profile dnschg

helm install keycloak keycloak -f keycloak/values-haproxy.yaml \
  --set-string or.hostname=$FQDN
helm install manager manager -f manager/values-haproxy-eks.yaml \
  --set-string or.hostname=$FQDN

while ! dig +short $FQDN | grep -qE '^[0-9]'; do
    echo "Waiting for DNS resolution..."
    sleep 5
done

kubectl delete pod -l app.kubernetes.io/name=proxy

echo "Access the manager at https://$FQDN"
