#!/bin/bash

# Provisions the standard stack of resources using CloudFormation template (cloudformation-create-ec2.yml) in the
# specified AWS member account.
#
# To be called with arguments:
# 1 - Account name where resources should be created (will try and assume developers access role in this account);
#     the account must exist and be provisioned in the standard way (see provision_account.sh). If not specified then
#     the account of the provided credentials will be used
# 2 - FQDN for host (e.g. staging.demo.openremote.app)
# 3 - EC2 instance type see cloud formation template parameter
# 4 - WAIT_FOR_STACK if 'false' script will not wait until the cloud formation stack is running

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

ACCOUNT_NAME=${1,,}
HOST=${2,,}
INSTANCE_TYPE=${3,,}
WAIT_FOR_STACK=${4,,}
AWS_ENABLED=${5,,}

if [ -z "$HOST" ]; then
  echo "Host must be set"
  exit 1
fi

if [ -f "cloudformation-create-ec2.yml" ]; then
  TEMPLATE_PATH="cloudformation-create-ec2.yml"
elif [ -f ".ci_cd/aws/cloudformation-create-ec2.yml" ]; then
  TEMPLATE_PATH=".ci_cd/aws/cloudformation-create-ec2.yml"
elif [ -f "openremote/.ci_cd/aws/cloudformation-create-ec2.yml" ]; then
  TEMPLATE_PATH="openremote/.ci_cd/aws/cloudformation-create-ec2.yml"
else
  echo "Cannot determine location of cloudformation-create-ec2.yml"
  exit 1
fi

# Optionally login if AWS_ENABLED != 'true'
source "${awsDir}/login.sh"

if [ -n "$ACCOUNT_NAME" ]; then
  # Update github-da profile with ARN for ACCOUNT_ID
  source "${awsDir}/update_developers_access_profile.sh"
  ACCOUNT_PROFILE="--profile github-da"
else
  ACCOUNT_PROFILE="--profile github"
fi

STACK_NAME="$HOST"

# Check stack doesn't already exist
STACK_ID=$(aws cloudformation list-stacks --query "StackSummaries[?StackName=='$STACK_NAME'].StackId" --stack-status-filter CREATE_IN_PROGRESS CREATE_FAILED CREATE_COMPLETE --output text $ACCOUNT_PROFILE 2>/dev/null)

if [ -n "$STACK_ID" ]; then
  echo "Stack already exists for this host '$HOST'"
  exit 1
fi

#Configure parameters
CALLER_ACCOUNT_ID=$(aws sts get-caller-identity --query 'Account' --output text)
SMTP_ARN="arn:aws:ses:eu-west-1:$CALLER_ACCOUNT_ID:identity/openremote.io"
PARAMS="ParameterKey=Host,ParameterValue=$HOST ParameterKey=SMTPORArn, ParameterValue=$SMTP_ARN"

if [ -n "$INSTANCE_TYPE" ]; then
  PARAMS="$PARAMS ParameterKey=InstanceType, ParameterValue=$INSTANCE_TYPE"
fi
# Create standard stack resources in specified account
aws cloudformation create-stack --capabilities CAPABILITY_NAMED_IAM --stack-name $STACK_NAME --template-body file://$TEMPLATE_PATH --parameters $PARAMS $ACCOUNT_PROFILE

if [ $? -ne 0 ]; then
  echo "Create stack failed"
else
  echo "Create stack in progress"
fi

if [ "$WAIT_FOR_STACK" != 'false' ]; then
  # Wait for cloud formation stack status to be CREATE_*
  echo "Waiting for stack to be created"

fi










if [ "$AWS_ENABLED" != 'true' ]; then
  echo "Failed to login to AWS"
  exit 1
fi

