#!/bin/bash

# Updates/creates the github-da AWS profile with developers-access assumed role

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

if [ -z "$ACCOUNT_ID" ]; then
  if [ -z "$ACCOUNT_NAME" ]; then
    echo "ACCOUNT_ID or ACCOUNT_NAME must be set to use this script"
    exit 1
  fi

  source "${awsDir}get_account_id.sh"
fi

if [ -z "$ACCOUNT_ID" ]; then
  echo "Cannot determine account ID"
  exit 1
fi

count=100
if [ "$RETRY" == 'true' ]; then
  count=0
fi

# Verify that the role can be assumed
ROLE_ARN="arn:aws:iam::$ACCOUNT_ID:role/developers-access-$AWS_REGION"
aws configure --profile github-da set source_profile github
aws configure --profile github-da set role_arn $ROLE_ARN
echo "Setting github-da role ARN to '$ROLE_ARN'"

# Try and assume the role
aws sts assume-role --role-arn $ROLE_ARN --role-session-name "github-developers-access-$AWS_REGION" --duration-seconds=900 &>/dev/null

until [ $count -ge 20 ] || aws sts assume-role --role-arn $ROLE_ARN --role-session-name "github-developers-access-$AWS_REGION" --duration-seconds=900 &>/dev/null; do
  echo "Failed to assume role .. Sleeping 30 seconds"
  sleep 30
  count=$((count+1))
done

if [ $? -ne 0 ]; then
  echo "Cannot assume developers access role"
  exit 1
fi
