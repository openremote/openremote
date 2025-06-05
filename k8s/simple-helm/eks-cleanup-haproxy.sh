#!/bin/bash

CLUSTER_NAME=testcluster
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

DNS_NAME=`aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[0].DNSName"`
HOSTED_ZONE_ID=`aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[0].CanonicalHostedZoneId"`

echo "Delete DNS record $FQDN"

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "DELETE", "ResourceRecordSet": { "Name": "'$FQDN'", "Type": "A", "AliasTarget":{ "HostedZoneId": '$HOSTED_ZONE_ID',"DNSName": '$DNS_NAME',"EvaluateTargetHealth": false} } } ]}' \
     --profile dnschg

helm uninstall manager
helm uninstall keycloak
helm uninstall postgresql
helm uninstall proxy

# Give the AWS LB Controller time to delete the NLB after the Service is deleted
while aws elbv2 describe-load-balancers  --profile or --query "LoadBalancers[?Type=='network']" 2>/dev/null | grep '"Code": "active"'; do
  echo "Waiting for load balancer to be deleted..."
  sleep 10
done
helm uninstall aws-load-balancer-controller -n kube-system

MANAGER_VOLUMEID=`kubectl get pv manager-data-pv -o=jsonpath='{.spec.awsElasticBlockStore.volumeID}'`
PSQL_VOLUMEID=`kubectl get pv postgresql-data-pv -o=jsonpath='{.spec.awsElasticBlockStore.volumeID}'`
kubectl delete pv manager-data-pv
kubectl delete pv postgresql-data-pv
aws ec2 delete-volume --volume-id $MANAGER_VOLUMEID
aws ec2 delete-volume --volume-id $PSQL_VOLUMEID
helm uninstall or-setup

# Manually deleting all addons, this should not be required but otherwise the delete cluster fails
# with "2 pods are unevictable from node ..." error messages
eksctl delete addon --cluster $CLUSTER_NAME --name aws-ebs-csi-driver
eksctl delete addon --cluster $CLUSTER_NAME --name vpc-cni
eksctl delete addon --cluster $CLUSTER_NAME --name metrics-server
eksctl delete addon --cluster $CLUSTER_NAME --name coredns
eksctl delete addon --cluster $CLUSTER_NAME --name kube-proxy

eksctl delete cluster --name $CLUSTER_NAME
