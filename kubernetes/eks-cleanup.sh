#!/bin/bash

. ./eks-common.sh

CERTIFICATE_ARN=$(kubectl get ingress manager -o jsonpath='{.metadata.annotations.\alb\.ingress\.kubernetes\.io\/certificate-arn}')

DNS_RECORD_NAME=$(aws acm describe-certificate --certificate-arn $CERTIFICATE_ARN --profile or --query "Certificate.DomainValidationOptions[0].ResourceRecord.Name")
DNS_RECORD_VALUE=$(aws acm describe-certificate --certificate-arn $CERTIFICATE_ARN --profile or --query "Certificate.DomainValidationOptions[0].ResourceRecord.Value")

echo "Delete DNS record $DNS_RECORD_NAME"
aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "DELETE", "ResourceRecordSet": { "Name": '$DNS_RECORD_NAME', "Type": "CNAME", "TTL": 300, "ResourceRecords" : [ { "Value": '$DNS_RECORD_VALUE' } ] } } ]}' \
     --profile dnschg

DNS_RECORD_NAME=$(aws acm describe-certificate --certificate-arn $CERTIFICATE_ARN --profile or --query "Certificate.DomainValidationOptions[1].ResourceRecord.Name")
DNS_RECORD_VALUE=$(aws acm describe-certificate --certificate-arn $CERTIFICATE_ARN --profile or --query "Certificate.DomainValidationOptions[1].ResourceRecord.Value")

echo "Delete DNS record $DNS_RECORD_NAME"
aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "DELETE", "ResourceRecordSet": { "Name": '$DNS_RECORD_NAME', "Type": "CNAME", "TTL": 300, "ResourceRecords" : [ { "Value": '$DNS_RECORD_VALUE' } ] } } ]}' \
     --profile dnschg

DNS_NAME=$(aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?Type=='application'].DNSName | [0]")
HOSTED_ZONE_ID=$(aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?Type=='application'].CanonicalHostedZoneId | [0]")

echo "Delete DNS record $FQDN"

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "DELETE", "ResourceRecordSet": { "Name": "'$FQDN'", "Type": "A", "AliasTarget":{ "HostedZoneId": '$HOSTED_ZONE_ID',"DNSName": '$DNS_NAME',"EvaluateTargetHealth": false} } } ]}' \
     --profile dnschg

DNS_NAME=$(kubectl get svc manager-mqtt -o jsonpath="{.status.loadBalancer.ingress[0].hostname}")
HOSTED_ZONE_ID=$(aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?DNSName=='$DNS_NAME'].CanonicalHostedZoneId | [0]")

echo "Delete DNS record $MQTT_FQDN"

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "DELETE", "ResourceRecordSet": { "Name": "'$MQTT_FQDN'", "Type": "A", "AliasTarget":{ "HostedZoneId": '$HOSTED_ZONE_ID',"DNSName": '\"$DNS_NAME\"',"EvaluateTargetHealth": false} } } ]}' \
     --profile dnschg

DNS_NAME=$(kubectl get svc manager-mqtts -o jsonpath="{.status.loadBalancer.ingress[0].hostname}")
HOSTED_ZONE_ID=$(aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?DNSName=='$DNS_NAME'].CanonicalHostedZoneId | [0]")

echo "Delete DNS record $MQTTS_FQDN"

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "DELETE", "ResourceRecordSet": { "Name": "'$MQTTS_FQDN'", "Type": "A", "AliasTarget":{ "HostedZoneId": '$HOSTED_ZONE_ID',"DNSName": '\"$DNS_NAME\"',"EvaluateTargetHealth": false} } } ]}' \
     --profile dnschg

helm uninstall manager
helm uninstall keycloak
helm uninstall postgresql

# Give the AWS LB Controller time to delete the NLB after the Service is deleted
while aws elbv2 describe-load-balancers  --profile or --query "LoadBalancers[?Type=='network']" 2>/dev/null | grep '"Code": "active"'; do
  echo "Waiting for load balancer to be deleted..."
  sleep 10
done
helm uninstall aws-load-balancer-controller -n kube-system

echo "Delete certificate $CERTIFICATE_ARN"
aws acm delete-certificate --certificate-arn $CERTIFICATE_ARN --profile or

MANAGER_VOLUMEID=$(kubectl get pv manager-data-pv -o=jsonpath='{.spec.awsElasticBlockStore.volumeID}')
PSQL_VOLUMEID=$(kubectl get pv postgresql-data-pv -o=jsonpath='{.spec.awsElasticBlockStore.volumeID}')
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
