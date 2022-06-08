#!/bin/bash

# Sets AWS_ACCOUNT_ID by looking up the requested AWS_ACCOUNT_NAME

if [ -z "$AWS_ACCOUNT_NAME" ]; then
  echo "AWS_ACCOUNT_NAME must be set to use this script"
  exit 1
fi

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

# Optionally login if AWS_ENABLED != 'true'
source "${awsDir}login.sh"

AWS_ACCOUNT_ID=$(aws ssm get-parameter --name "/Account-Ids/$AWS_ACCOUNT_NAME" --query "Parameter.Value" --output text 2>/dev/null)

if [ -z "$AWS_ACCOUNT_ID" ]; then
  echo "Looking for account in organization"
  AWS_ACCOUNT_ID=$(aws organizations list-accounts --query "Accounts[?Name=='$AWS_ACCOUNT_NAME'].Id" --output text 2>/dev/null)

  if [ -n "$AWS_ACCOUNT_ID" ]; then
    echo "Storing account ID for future reference"
    aws ssm put-parameter --name "/Account-Ids/$AWS_ACCOUNT_NAME" --value "$AWS_ACCOUNT_ID" --type String &>/dev/null
  fi
fi

if [ -z "$AWS_ACCOUNT_ID" ]; then
  echo "Failed to find account ID for AWS_ACCOUNT_NAME: $AWS_ACCOUNT_NAME"
else
  echo "$AWS_ACCOUNT_ID"
fi
