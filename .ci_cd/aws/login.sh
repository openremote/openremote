#!/bin/bash

# Logs into AWS by creating and/or using an AWS profile called github; if $AWS_ACCESS_KEY_ID, $AWS_SECRET_ACCESS_KEY,
# AWS_REGION environment variables are set then the github profile is configured with these values. AWS_REGION defaults
# to eu-west-1

if [ "$AWS_ENABLED" == 'true' ]; then
  return
fi

# Check AWS environment variables and verify login success
AWS_ENABLED=false
if [ -z "$AWS_REGION" ]; then
  AWS_REGION=eu-west-1
fi

if [ -n "$AWS_ACCESS_KEY_ID" ]; then
  aws configure --profile github set aws_access_key_id $AWS_ACCESS_KEY_ID
fi

if [ -n "$AWS_SECRET_ACCESS_KEY" ]; then
  aws configure --profile github set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
fi

if [ -n "$AWS_REGION" ]; then
  aws configure --profile github set region $AWS_REGION
  aws configure --profile github-da set region $AWS_REGION
fi

AWS_PROFILE=github
export AWS_PROFILE=$AWS_PROFILE

echo "Validating AWS credentials"
aws sts get-caller-identity

if [ $? -ne 0 ]; then
  echo "Failed to login to AWS"
  exit 1
else
  echo "Login succeeded"
  AWS_ENABLED=true
fi
