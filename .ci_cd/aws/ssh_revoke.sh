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
  aws ec2 revoke-security-group-ingress --group-name ssh-access --ip-permissions IpProtocol=tcp,FromPort=22,ToPort=22,Ipv6Ranges=[{CidrIpv6=$CIDR}] $PROFILE
  v6Result=$?
  aws ec2 revoke-security-group-ingress --group-name ssh-access --ip-permissions IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=$CIDR}] $PROFILE
  v4Result=$?

  if [ $v6Result -ne 0 ] && [ $v4Result -ne 0 ]; then
    echo "SSH revocation failed"
  else
    echo "SSH Access revoked"
  fi
fi
