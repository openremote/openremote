#!/bin/bash

# Revoke the specified IP address (IPv4 and/or IPv6) from AWS ssh-access security group for SSH access
# To be called with three arguments:
# 1 - CIDR
# 2 - AWS_PROFILE
# 3 - AWS_ENABLED (optional default false)

CIDR=$1
AWS_PROFILE=$2

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

if [ -n "$AWS_PROFILE" ]; then
  PROFILE="--profile $AWS_PROFILE"
fi

if [ -n "$CIDR" ]; then
  echo "Revoking SSH access for CIDR '$CIDR' on AWS"
  SGID=$(aws ec2 describe-security-groups --filters Name=tag:Name,Values=ssh-access --query "SecurityGroups[0].GroupId" --output text $PROFILE)

  if [ $? -ne 0 ]; then
    echo "Failed to find ssh-access security group"
    exit 1
  fi

  aws ec2 revoke-security-group-ingress --group-id $SGID --ip-permissions IpProtocol=tcp,FromPort=22,ToPort=22,Ipv6Ranges=[{CidrIpv6=$CIDR}] $PROFILE 2>/dev/null
  v6Result=$?
  aws ec2 revoke-security-group-ingress --group-id $SGID --ip-permissions IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=$CIDR}] $PROFILE 2>/dev/null
  v4Result=$?

  if [ $v6Result -ne 0 ] && [ $v4Result -ne 0 ]; then
    echo "SSH revocation failed"
  else
    echo "SSH Access revoked"
  fi
fi
