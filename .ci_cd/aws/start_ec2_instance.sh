#!/bin/bash

# To be called with two arguments:
# 1 - ec2 instance name (required)
# 2 - AWS_ENABLED (optional true)

INSTANCE_NAME=$1
AWS_ENABLED=$2

# Optionally login if AWS_ENABLED != 'true'
source "${BASH_SOURCE%/*}/login.sh"

if [ "$AWS_ENABLED" != 'true' ]; then
  exit 1
fi

echo "Attempting to start EC2 instance with name '$INSTANCE_NAME'..."
instanceId=$(aws ec2 describe-instances --filters 'Name=tag:Name,Values=$INSTANCE_NAME' --output text --query 'Reservations[*].Instances[*].InstanceId')
if [ -n "$instanceId" ]; then
  currentState=$(aws ec2 describe-instances --filters 'Name=tag:Name,Values=$INSTANCE_NAME' --output text --query 'Reservations[*].Instances[*].State.Name')

  if [ "$currentState" == 'stopped' ]; then
    echo "Starting EC2 instance"
    aws ec2 start-instances --instance-ids $instanceId

    echo "Waiting for up to 5mins for instance to be running"
    count=0
    while [ "$currentState" != 'running' ] && [ $count -lt 10 ]; do
      echo "attempt...$count"
      sleep 30
      currentState=$(aws ec2 describe-instances --filters 'Name=tag:Name,Values=$INSTANCE_NAME' --output text --query 'Reservations[*].Instances[*].State.Name')
      count=$((count+1))
    done

    if [ "$currentState" != 'running' ]; then
      echo "EC2 instance failed to start state is '$currentState'"
    else
      echo "EC2 instance started successfully"
    fi
  else
    echo "Current EC2 instance state is '$currentState' it must be in 'stopped' state to initiate auto start"
    exit 1
  fi
else
  echo "No EC2 instance found (maybe the host is not an EC2 instance)"
  exit 1
fi
