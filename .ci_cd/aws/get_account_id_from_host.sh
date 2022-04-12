#!/bin/bash

# Tries to determine account ID for the specified instance

if [ -z "$HOST" ]; then
  echo "HOST must be set to use this script"
  exit 1
fi

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

echo "Trying to determine the account for host '$HOST'"

# Optionally login if AWS_ENABLED != 'true'
source "${awsDir}login.sh"

MAX=$(($(awk -F. '{print NF}' <<< "$HOST")-1))
COUNT=1
ACCOUNTS=$(aws organizations list-accounts --query "Accounts[*].[Name,Id]" --output text)

while [ $COUNT -le $MAX ]; do
  NAME=$(awk -F. "{print \$$COUNT}" <<< "$HOST")
  echo "Checking for account matching '$NAME'"
  IFS=$'\n'

  for account in $ACCOUNTS; do
    IFS=$' \t'
    accountArr=( $account )
    accName=${accountArr[0]}
    accId=${accountArr[1]}
    if [ "$accName" == "$NAME" ]; then
      echo "Possible account match '$accName'"
      # Look for host in this account
      AWS_ACCOUNT_ID=$accId
      source "${awsDir}set_github-da_account_arn.sh"
      instanceId=$(aws ec2 describe-instances --filters "Name=tag:Name,Values=$HOST" --output text --query 'Reservations[*].Instances[*].InstanceId' --profile github-da)
      if [ -n "$instanceId" ]; then
        echo "Host found"
        return
      else
        echo "Account doesn't contain an instance named '$HOST'"
        unset AWS_ACCOUNT_ID
      fi
    fi
  done
  COUNT=$((COUNT+1))
done
