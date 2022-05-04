#!/bin/bash

# Provisions the standard stack of resources using CloudFormation template (cloudformation-create-ec2.yml) in the
# specified AWS member account; if no account specified then the account of the authenticated user will be used.
# The account must already exist and be provisioned in the standard way (see provision_account.sh). To access the
# account the $AWS_ROLE_NAME role in that account will be assumed (this role must exist and must have sufficient
# privileges to run the commands contained in this script). This script will determine the DNS hosted
# zone into which the A record for the host should be inserted by searching up the domain levels of the hosts FQDN.
# Will also provision an S3 bucket with the name of host (periods replaced with hyphens) unless set to false.
#
# To be called with arguments:
# 1 - AWS_ACCOUNT_NAME where resources should be created (defaults to callers account)
# 2 - HOST FQDN for e.g. staging.demo.openremote.app
# 3 - INSTANCE_TYPE EC2 instance type see cloud formation template parameter
# 4 - PROVISION_S3_BUCKET set to 'false' to not provision an S3 bucket for this host
# 5 - WAIT_FOR_STACK if 'false' script will not wait until the cloud formation stack is running

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

AWS_ACCOUNT_NAME=${1,,}
HOST=${2,,}
INSTANCE_TYPE=${3,,}
PROVISION_S3_BUCKET=${4,,}
WAIT_FOR_STACK=${5,,}

if [ -z "$HOST" ]; then
  echo "Host must be set"
  exit 1
fi

if [ -f "${awsDir}cloudformation-create-ec2.yml" ]; then
  TEMPLATE_PATH="${awsDir}cloudformation-create-ec2.yml"
elif [ -f ".ci_cd/aws/cloudformation-create-ec2.yml" ]; then
  TEMPLATE_PATH=".ci_cd/aws/cloudformation-create-ec2.yml"
elif [ -f "openremote/.ci_cd/aws/cloudformation-create-ec2.yml" ]; then
  TEMPLATE_PATH="openremote/.ci_cd/aws/cloudformation-create-ec2.yml"
else
  echo "Cannot determine location of cloudformation-create-ec2.yml"
  exit 1
fi

# Optionally login if AWS_ENABLED != 'true'
source "${awsDir}login.sh"

ACCOUNT_PROFILE=
if [ -n "$AWS_ACCOUNT_NAME" ]; then
  # Update github-da profile with ARN for AWS_ACCOUNT_ID
  source "${awsDir}set_github-da_account_arn.sh"
  ACCOUNT_PROFILE="--profile github-da"
fi

STACK_NAME=$(tr '.' '-' <<< "$HOST")
SMTP_STACK_NAME="$STACK_NAME-smtp"

# Provision SMTP user using cloud formation (if stack doesn't already exist)
echo "Provisioning SMTP user"
STATUS=$(aws cloudformation describe-stacks --stack-name $SMTP_STACK_NAME --query "Stacks[0].StackStatus" --output text 2>/dev/null)

if [ -n "$STATUS" ] && [ "$STATUS" != 'DELETE_COMPLETE' ]; then
  echo "Stack already exists for this host's SMTP user '$HOST' current status is '$STATUS'"
  STACK_ID=$(aws cloudformation describe-stacks --stack-name $SMTP_STACK_NAME --query "Stacks[0].StackId" --output text 2>/dev/null)
else

  if [ -f "${awsDir}cloudformation-create-smtp-user.yml" ]; then
    SMTP_TEMPLATE_PATH="${awsDir}cloudformation-create-smtp-user.yml"
  elif [ -f ".ci_cd/aws/cloudformation-create-smtp-user.yml" ]; then
    SMTP_TEMPLATE_PATH=".ci_cd/aws/cloudformation-create-smtp-user.yml"
  elif [ -f "openremote/.ci_cd/aws/cloudformation-create-smtp-user.yml" ]; then
    SMTP_TEMPLATE_PATH="openremote/.ci_cd/aws/cloudformation-create-smtp-user.yml"
  else
    echo "Cannot determine location of cloudformation-create-smtp-user.yml"
    exit 1
  fi

  #Configure parameters
  PARAMS="ParameterKey=UserName,ParameterValue='$SMTP_STACK_NAME'"

  # Create standard stack resources in specified account
  STACK_ID=$(aws cloudformation create-stack --capabilities CAPABILITY_NAMED_IAM --stack-name $SMTP_STACK_NAME --template-body file://$SMTP_TEMPLATE_PATH --parameters $PARAMS --output text)

  if [ $? -ne 0 ]; then
    echo "Create stack failed"
    exit 1
  fi

  if [ "$WAIT_FOR_STACK" != 'false' ]; then
    # Wait for cloud formation stack status to be CREATE_*
    echo "Waiting for stack to be created"
    STATUS=$(aws cloudformation describe-stacks --stack-name $SMTP_STACK_NAME --query "Stacks[?StackId=='$STACK_ID'].StackStatus" --output text 2>/dev/null)

    while [[ "$STATUS" == 'CREATE_IN_PROGRESS' ]]; do
      echo "Stack creation is still in progress .. Sleeping 30 seconds"
      sleep 30
      STATUS=$(aws cloudformation describe-stacks --stack-name $SMTP_STACK_NAME --query "Stacks[?StackId=='$STACK_ID'].StackStatus" --output text 2>/dev/null)
    done

    if [ "$STATUS" != 'CREATE_COMPLETE' ]; then
      echo "Stack creation has failed status is '$STATUS'"
      exit 1
    else
      echo "Stack creation is complete"
    fi
  fi
fi

# Check stack doesn't already exist
STATUS=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[0].StackStatus" --output text $ACCOUNT_PROFILE 2>/dev/null)

if [ -n "$STATUS" ] && [ "$STATUS" != 'DELETE_COMPLETE' ]; then
  echo "Stack already exists for this host '$HOST' current status is '$STATUS'"
  STACK_ID=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[0].StackId" --output text $ACCOUNT_PROFILE 2>/dev/null)
else
  # Configure parameters
  PARAMS="ParameterKey=Host,ParameterValue=$HOST"

  if [ -n "$INSTANCE_TYPE" ]; then
    PARAMS="$PARAMS ParameterKey=InstanceType,ParameterValue=$INSTANCE_TYPE"
  fi

  # Get SMTP credentials
  SMTP_HOST="email-smtp.$AWS_REGION.amazonaws.com"
  SMTP_USER=$(aws cloudformation describe-stacks --stack-name $SMTP_STACK_NAME --query "Stacks[0].Outputs[?OutputKey=='SMTPUserKey'].OutputValue" --output text 2>/dev/null)
  SMTP_SECRET=$(aws cloudformation describe-stacks --stack-name $SMTP_STACK_NAME --query "Stacks[0].Outputs[?OutputKey=='SMTPUserSecret'].OutputValue" --output text 2>/dev/null)

  PARAMS="$PARAMS ParameterKey=SMTPHost,ParameterValue=$SMTP_HOST"

  if [ -n "$SMTP_USER" ]; then
    PARAMS="$PARAMS ParameterKey=SMTPUser,ParameterValue=$SMTP_USER"
  fi
  if [ -n "$SMTP_SECRET" ]; then
    PARAMS="$PARAMS ParameterKey=SMTPSecret,ParameterValue=$SMTP_SECRET ParameterKey=SMTPRegion,ParameterValue=$AWS_REGION"
  fi

  # Determine DNSHostedZoneName and DNSHostedZoneRoleArn (must be set if hosted zone is not in the same account as where the host is being created)
  echo "Determining DNS parameters"
  TLD_NAME=$(awk -F. '{print $(NF-1)"."$(NF)}' <<< "$HOST")
  COUNT=$(($(awk -F. '{print NF}' <<< "$HOST")-1))
  HOSTED_ZONES=$(aws route53 list-hosted-zones --query "HostedZones[?contains(Name, '$TLD_NAME.')].[Name]" --output text $ACCOUNT_PROFILE)

  if [ -n "$ACCOUNT_PROFILE" ]; then
    # Append caller account hosted zones
    read -r -d '' HOSTED_ZONES << EOF
$HOSTED_ZONES
$(aws route53 list-hosted-zones --query "HostedZones[?contains(Name, '$TLD_NAME.')].[Name,'true']" --output text)
EOF
    echo "$HOSTED_ZONES"
  fi

  if [ -n "$HOSTED_ZONES" ]; then
    # Match hosted zone with the same name as the host moving up a domain level each time
    i=1
    while [ $i -le $COUNT ]; do

      HOSTED_ZONE=$(cut -d'.' -f$i- <<< "$HOST")

      IFS=$'\n'
      for zone in $HOSTED_ZONES; do
        IFS=$' \t'
        zoneArr=( $zone )
        name=${zoneArr[0]}
        callerAccount=${zoneArr[1]}

        if [ "$name" == "$HOSTED_ZONE." ]; then
          echo "Found hosted zone for this host '$HOSTED_ZONE'"
          DNSHostedZoneName=$HOSTED_ZONE
          if [ "$callerAccount" == 'true' ]; then
            # Get Role ARN that can be assumed to allow DNS record update for this host from the host's account
            DNSHostedZoneRoleArn=$(aws ssm get-parameter --name Hosted-Zone-Access-Role-Arn --query "Parameter.Value" --output text $ACCOUNT_PROFILE)
            if [ -z "$DNSHostedZoneRoleArn" ]; then
              echo "Failed to get 'Hosted-Zone-Access-Role-Arn' from parameter store this must be set for cross account DNS support"
              exit 1
            fi
          fi
          break
        fi
      done

      if [ -n "$DNSHostedZoneName" ]; then
        break
      fi

      i=$((i+1))
    done
  fi

  if [ -n "$DNSHostedZoneName" ]; then
    PARAMS="$PARAMS ParameterKey=DNSHostedZoneName,ParameterValue=$DNSHostedZoneName"
  fi
  if [ -n "$DNSHostedZoneRoleArn" ]; then
    PARAMS="$PARAMS ParameterKey=DNSHostedZoneRoleArn,ParameterValue=$DNSHostedZoneRoleArn"
  fi

  # Create standard stack resources in specified account
  STACK_ID=$(aws cloudformation create-stack --capabilities CAPABILITY_NAMED_IAM --stack-name $STACK_NAME --template-body file://$TEMPLATE_PATH --parameters $PARAMS --output text $ACCOUNT_PROFILE)

  if [ $? -ne 0 ]; then
    echo "Create stack failed"
  else
    echo "Create stack in progress"
  fi
fi

if [ "$WAIT_FOR_STACK" != 'false' ]; then
  # Wait for cloud formation stack status to be CREATE_*
  echo "Waiting for stack to be created"
  STATUS=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[?StackId=='$STACK_ID'].StackStatus" --output text $ACCOUNT_PROFILE 2>/dev/null)

  while [[ "$STATUS" == 'CREATE_IN_PROGRESS' ]]; do
    echo "Stack creation is still in progress .. Sleeping 30 seconds"
    sleep 30
    STATUS=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[?StackId=='$STACK_ID'].StackStatus" --output text $ACCOUNT_PROFILE 2>/dev/null)
  done

  if [ "$STATUS" != 'CREATE_COMPLETE' ] && [ "$STATUS" != 'UPDATE_COMPLETE' ]; then
    echo "Stack creation has failed status is '$STATUS'"
    exit 1
  else
    echo "Stack creation is complete"
  fi
fi

# Provision S3 bucket
if [ "$PROVISION_S3_BUCKET" != 'false' ]; then
  echo "Provisioning S3 bucket for host '$HOST'"

  BUCKET=$(aws s3api list-buckets --query "Buckets[?Name=='$HOST'].Name" --output text $ACCOUNT_PROFILE)

  if [ -n "$BUCKET" ]; then
    echo "Bucket for this host already exists"
  else
    LOCATION=$(aws s3api create-bucket --bucket $HOST --acl private --create-bucket-configuration LocationConstraint=$AWS_REGION --output text $ACCOUNT_PROFILE)
    if [ $? -ne 0 ]; then
      echo "Bucket creation failed"
    else
      echo "Bucket created successfully '$LOCATION'"
      aws s3api put-bucket-versioning --bucket $HOST --versioning-configuration Status=Enabled $ACCOUNT_PROFILE
      aws s3api put-public-access-block --bucket $HOST --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true" $ACCOUNT_PROFILE
    fi
  fi
fi
