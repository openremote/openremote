#!/bin/bash

# To be called with two arguments:
# 1 - ec2 instance name (required)
# 2 - AWS_ENABLED (optional true)

INSTANCE_NAME=${1,,}

if [ "$AWS_ENABLED" != true ]; then
  AWS_ENABLED=${2,,}
fi

source "${BASH_SOURCE%/*}/login.sh"

if [ "$AWS_ENABLED" != 'true' ]; then
  exit 1
fi

echo "Attempting to stop EC2 instance with name '$INSTANCE_NAME'..."
instanceId=$(aws ec2 describe-instances --filters 'Name=tag:Name,Values=$INSTANCE_NAME' --output text --query 'Reservations[*].Instances[*].InstanceId')
if [ -n "$instanceId" ]; then
  currentState=$(aws ec2 describe-instances --filters 'Name=tag:Name,Values=$INSTANCE_NAME' --output text --query 'Reservations[*].Instances[*].State.Name')

  if [ "$currentState" == 'running' ]; then
    echo "Stopping EC2 instance"
    aws ec2 stop-instances --instance-ids $instanceId
  else
    echo "Current EC2 instance state is '$currentState' it must be in 'running' state to initiate stop"
    exit 1
  fi
else
  echo "No EC2 instance found (maybe the host is not an EC2 instance)"
  exit 1
fi
