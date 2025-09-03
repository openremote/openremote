#!/bin/bash

. ./eks-common.sh

envsubst < cluster.yaml | eksctl create cluster -f - --profile or

# [Installation Guide - AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/latest/deploy/installation/)

helm repo add eks https://aws.github.io/eks-charts
helm repo update eks

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=$CLUSTER_NAME \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller

PSQL_VOLUMEID=$(aws ec2 create-volume --size 1 --availability-zone eu-west-1a --tag-specifications "ResourceType=volume,Tags=[{Key=Name,Value=psql-data}]" --query VolumeId)
MANAGER_VOLUMEID=$(aws ec2 create-volume --size 1 --availability-zone eu-west-1a --tag-specifications "ResourceType=volume,Tags=[{Key=Name,Value=manager-data}]" --query VolumeId)

# Wait for AWS LB ctrl to be ready
kubectl rollout status deployment aws-load-balancer-controller -n kube-system --timeout=300s

helm install or-setup or-setup --set aws.enabled=true --set aws.managerVolumeId=$MANAGER_VOLUMEID --set aws.psqlVolumeId=$PSQL_VOLUMEID

CERTIFICATE_ARN=$(aws acm request-certificate --domain-name $FQDN --subject-alternative-names $MQTTS_FQDN --validation-method DNS --profile or --query "CertificateArn" --output text)
aws acm describe-certificate --certificate-arn $CERTIFICATE_ARN --profile or

helm install postgresql postgresql -f postgresql/values-eks.yaml
helm install keycloak keycloak -f keycloak/values-eks.yaml \
  --set-string or.hostname=$FQDN \
  --set-string 'ingress.annotations.alb\.ingress\.kubernetes\.io\/certificate-arn'=$CERTIFICATE_ARN
helm install manager manager -f manager/values-eks.yaml \
  --set-string or.hostname=$FQDN \
  --set-string 'ingress.annotations.alb\.ingress\.kubernetes\.io\/certificate-arn'=$CERTIFICATE_ARN \
  --set-string 'service.mqtts.annotations.service\.beta\.kubernetes\.io\/aws-load-balancer-ssl-cert'=$CERTIFICATE_ARN

# For FQDN

DNS_RECORD_NAME=$(aws acm describe-certificate --certificate-arn $CERTIFICATE_ARN --profile or --query "Certificate.DomainValidationOptions[0].ResourceRecord.Name")
DNS_RECORD_VALUE=$(aws acm describe-certificate --certificate-arn $CERTIFICATE_ARN --profile or --query "Certificate.DomainValidationOptions[0].ResourceRecord.Value")

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "UPSERT", "ResourceRecordSet": { "Name": '$DNS_RECORD_NAME', "Type": "CNAME", "TTL": 300, "ResourceRecords" : [ { "Value": '$DNS_RECORD_VALUE' } ] } } ]}' \
     --profile dnschg

# For MQTTS_FQDN

DNS_RECORD_NAME=$(aws acm describe-certificate --certificate-arn $CERTIFICATE_ARN --profile or --query "Certificate.DomainValidationOptions[1].ResourceRecord.Name")
DNS_RECORD_VALUE=$(aws acm describe-certificate --certificate-arn $CERTIFICATE_ARN --profile or --query "Certificate.DomainValidationOptions[1].ResourceRecord.Value")

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "UPSERT", "ResourceRecordSet": { "Name": '$DNS_RECORD_NAME', "Type": "CNAME", "TTL": 300, "ResourceRecords" : [ { "Value": '$DNS_RECORD_VALUE' } ] } } ]}' \
     --profile dnschg

# AWS LB Controller only creates an Application LB if there's an ingress
# Following logic is assuming there is the only (application) LB in the account
while ! aws elbv2 describe-load-balancers  --profile or --query "LoadBalancers[?Type=='application']" 2>/dev/null | grep '"Code": "active"'; do
  echo "Waiting for application load balancer to be created..."
  sleep 10
done

# We're re-directing the FQDN to the application LB (web interface)
until DNS_NAME=$(aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?Type=='application'].DNSName | [0]") && [ -n "$DNS_NAME" ]; do
  echo "Waiting for LoadBalancer DNS..."; sleep 5
done
HOSTED_ZONE_ID=$(aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?Type=='application'].CanonicalHostedZoneId | [0]")

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "UPSERT", "ResourceRecordSet": { "Name": "'$FQDN'", "Type": "A", "AliasTarget":{ "HostedZoneId": '$HOSTED_ZONE_ID',"DNSName": '$DNS_NAME',"EvaluateTargetHealth": false} } } ]}' \
     --profile dnschg

# We're re-directing the MQTT_FQDN to its network load balancer
until DNS_NAME=$(kubectl get svc manager-mqtt -o jsonpath="{.status.loadBalancer.ingress[0].hostname}") && [ -n "$DNS_NAME" ]; do
  echo "Waiting for LoadBalancer DNS..."; sleep 5
done
while ! aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?DNSName=='$DNS_NAME']" 2>/dev/null | grep '"Code": "active"'; do
  echo "Waiting for network load balancer to be created for $DNS_NAME..."
  sleep 10
done
HOSTED_ZONE_ID=$(aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?DNSName=='$DNS_NAME'].CanonicalHostedZoneId | [0]")

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "UPSERT", "ResourceRecordSet": { "Name": "'$MQTT_FQDN'", "Type": "A", "AliasTarget":{ "HostedZoneId": '$HOSTED_ZONE_ID',"DNSName": '\"$DNS_NAME\"',"EvaluateTargetHealth": false} } } ]}' \
     --profile dnschg

# We're re-directing the MQTTS_FQDN to its network load balancer
until DNS_NAME=$(kubectl get svc manager-mqtts -o jsonpath="{.status.loadBalancer.ingress[0].hostname}") && [ -n "$DNS_NAME" ]; do
  echo "Waiting for LoadBalancer DNS..."; sleep 5
done
while ! aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?DNSName=='$DNS_NAME']" 2>/dev/null | grep '"Code": "active"'; do
  echo "Waiting for network load balancer to be created for $DNS_NAME..."
  sleep 10
done
HOSTED_ZONE_ID=$(aws elbv2 describe-load-balancers --profile or --query "LoadBalancers[?DNSName=='$DNS_NAME'].CanonicalHostedZoneId | [0]")

aws route53 change-resource-record-sets \
    --hosted-zone-id /hostedzone/Z08751721JH0NB6LLCB4V \
    --change-batch \
     '{"Changes": [ { "Action": "UPSERT", "ResourceRecordSet": { "Name": "'$MQTTS_FQDN'", "Type": "A", "AliasTarget":{ "HostedZoneId": '$HOSTED_ZONE_ID',"DNSName": '\"$DNS_NAME\"',"EvaluateTargetHealth": false} } } ]}' \
     --profile dnschg

echo "Access the manager at https://$FQDN"
