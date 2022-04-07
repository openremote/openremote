#!/bin/bash

# Start/stop the EC2 instance with the specified name; if the account of the instance is not specified then a search will
# be attempted
# Arguments:
# 1 - start/stop
# 2 - HOST name of the instance (host FQDN)
# 3 - AWS_ACCOUNT_NAME name of the account that contains the instance (optional)

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

ACTION=${1,,}
HOST=${2,,}
AWS_ACCOUNT_NAME=${3,,}

source "${awsDir}login.sh"

if [ -z "$HOST" ]; then
  echo "HOST must be set"
  exit 1
fi

if [ -z "$AWS_ACCOUNT_NAME" ]; then
  echo "Attempting to find owning account for host '$HOST'"
  source "${awsDir}get_account_id_from_host.sh" $HOST
  if [ -z "$AWS_ACCOUNT_ID" ]; then
    echo "Failed to find owning account"
    exit 1
  fi
fi

# Update github-da profile with ARN for AWS_ACCOUNT_ID
source "${awsDir}set_github-da_account_arn.sh"
ACCOUNT_PROFILE="--profile github-da"

instanceId=$(aws ec2 describe-instances --filters "Name=tag:Name,Values=$HOST" --output text --query 'Reservations[*].Instances[*].InstanceId' $ACCOUNT_PROFILE)

if [ -z "$instanceId" ]; then
  echo "Failed to find instance"
  exit 1
fi

currentState=$(aws ec2 describe-instances --filters "Name=tag:Name,Values=$HOST" --output text --query 'Reservations[*].Instances[*].State.Name' $ACCOUNT_PROFILE)

if [ "$ACTION" == 'start' ]; then
  if [ "$currentState" == 'running' ]; then
    echo "Host is already running"
    exit 0
  elif [ "$currentState" != 'stopped' ]; then
    echo "Current state must be 'stopped' to start instance but it is '$currentState'"
    exit 1
  fi

  echo "Starting EC2 instance"
  aws ec2 start-instances --instance-ids $instanceId $ACCOUNT_PROFILE

elif [ "$ACTION" == 'stop' ]; then
  if [ "$currentState" == 'stopped' ]; then
    echo "Host is already stopped"
    exit 0
  elif [ "$currentState" != 'running' ]; then
    echo "Current state must be 'running' to start instance but it is '$currentState'"
    exit 1
  fi

  echo "Stopping EC2 instance"
  aws ec2 stop-instances --instance-ids $instanceId $ACCOUNT_PROFILE
else
  echo "Unknown action '$ACTION'"
  exit 1
fi
