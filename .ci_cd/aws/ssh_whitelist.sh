#!/bin/bash

# Add the specified IP address (IPv4 and/or IPv6) to AWS ssh-access security group for SSH access in the specified
# To be called with three arguments:
# 1 - CIDR
# 2 - AWS_PROFILE
# 3 - AWS_ENABLED (optional default false)

CIDR=$1
PROFILE=$2

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

source "${awsDir}login.sh"

if [ "$AWS_ENABLED" != 'true' ]; then
  exit 1
fi

if [ -n "$PROFILE" ]; then
  PROFILE="--profile $PROFILE"
fi

if [ -n "$CIDR" ]; then
  echo "Granting SSH access for CIDR '$CIDR' on AWS"
  aws ec2 authorize-security-group-ingress --group-name ssh-access --protocol tcp --port 22 --cidr $CIDR $PROFILE

  if [ $? -ne 0 ]; then
    echo "SSH Access failed might not be able to SSH into host(s)"
  else
    echo "SSH Access granted"
  fi
else
  echo "No CIDR provided"
fi
