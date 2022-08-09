#!/bin/bash

# Add the specified IP address (IPv4 and/or IPv6) to AWS ssh-access security group for SSH access using specified CLI
# profile.
# To be called with three arguments:
# 1 - CIDR
# 2 - DESCRIPTION
# 3 - AWS_PROFILE
# 4 - AWS_ENABLED (optional default false)

CIDR=$1
DESCRIPTION=$2
AWS_PROFILE=$3

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

if [ -n "$AWS_PROFILE" ]; then
  PROFILE="--profile $AWS_PROFILE"
fi

if [ -n "$CIDR" ]; then
  echo "Granting SSH access for CIDR '$CIDR' on AWS"
  SGID=$(aws ec2 describe-security-groups --filters Name=tag:Name,Values=ssh-access --query "SecurityGroups[0].GroupId" --output text $PROFILE)

  if [ $? -ne 0 ]; then
    echo "Failed to find ssh-access security group"
    exit 1
  fi

  if [[ "$CIDR" == *":"* ]]; then
    aws ec2 authorize-security-group-ingress --group-id $SGID --ip-permissions "IpProtocol=tcp,FromPort=22,ToPort=22,Ipv6Ranges=[{CidrIpv6=$CIDR,Description=$DESCRIPTION}]" $PROFILE
  else
    aws ec2 authorize-security-group-ingress --group-id $SGID --ip-permissions "IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=$CIDR,Description=$DESCRIPTION}]" $PROFILE
  fi

  if [ $? -ne 0 ]; then
    echo "SSH Access failed might not be able to SSH into host(s)"
  else
    echo "SSH Access granted"
  fi
else
  echo "No CIDR provided"
fi
