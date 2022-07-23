#!/bin/bash

# Provisions a member account of the specified name under the specified OU using Control Tower Account Factory via
# Service Catalog; callee must have required permissions to call the service catalog and must also be in the Control
# Tower home account.
# The customisations for control tower will perform some standard customisations of the provisioned account as defined
# in the custom-control-tower-configuration CodeCommit repo. Also does the following:
#
# * Add /SSH-Key/developers store parameter to the new account's list of SSH public keys (this is used when provisioning
#   hosts to ensure it is possible to SSH into the new host)
# * Create Hosted domain zone (if requested) and will create the NS record in the PARENT_DNS_ZONE to delegate to the
#   new hosted zone; the zone will use the account name.
# * Adds SSH whitelist to the ssh-access security group in the new account (see refresh_ssh_whitelist.sh)
# * Create EFS filesystem for sharing data between instances (see cloudformation-create-efs.yml)
#
# Arguments:
# 1 - OU - Name of organizational unit where account should be provisioned (defaults to account root if not set)
# 1 - AWS_ACCOUNT_NAME - Name of the new account (required)
# 2 - PARENT_DNS_ZONE - name of parent hosted domain zone in management account
# 3 - HOSTED_DNS - If set to 'true' a sub domain hosted zone will be provisioned in the new account and delegated from the
#     PARENT_DNS_ZONE in the callee account (e.g. x.y -> AWS_ACCOUNT_NAME.x.y)
# 4 - PROVISION_EFS set to 'false' to not provision an EFS volume for the default VPC using (cloudformation-create-efs.yml)

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

OU=${1}
AWS_ACCOUNT_NAME=${2,,}
PARENT_DNS_ZONE=${3,,}
HOSTED_DNS=${4,,}
PROVISION_EFS=${5,,}
ACCOUNT_PROFILE='--profile github-da'

if [ -z "$AWS_ACCOUNT_NAME" ]; then
  echo "AWS_ACCOUNT_NAME must be set"
  exit 1
fi

# Optionally login if AWS_ENABLED != 'true'
source "${awsDir}login.sh"

# Get github account ID from github profile
CALLER_AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query 'Account' --output text)

# Check account doesn't already exist
source "${awsDir}get_account_id.sh"

CatalogName="account-$AWS_ACCOUNT_NAME"

if [ -n "$AWS_ACCOUNT_ID" ]; then
  echo "Account already exists so skipping initial provisioning step"
else
  RandomToken=$(echo $(( $RANDOM * 99999999999 )) | cut -c 1-13)
  prod_id=$(aws servicecatalog search-products --filters FullTextSearch='AWS Control Tower Account Factory' --query "ProductViewSummaries[*].ProductId" --output text)

  if [ $? -ne 0 ] || [ -z "$prod_id" ]; then
    echo "Failed to get product ID for Control Tower Account Factory you must be logged in to the management account"
    exit 1
  else
    echo "Provisioning product ID: $prod_id"
  fi

  pa_id=$(aws servicecatalog describe-product --id $prod_id --query "ProvisioningArtifacts[-1].Id" --output text)

  if [ $? -ne 0 ]  || [ -z "$pa_id" ]; then
    echo "Failed to get provisioning artifact ID for Control Tower Account Factory"
    exit 1
  else
    echo "Provisioning artifact ID: $pa_id"
  fi

  PARAMS="Key=SSOUserFirstName,Value=$AWS_ACCOUNT_NAME Key=SSOUserLastName,Value=$AWS_ACCOUNT_NAME Key=SSOUserEmail,Value=$AWS_ACCOUNT_NAME@aws.openremote.io Key=AccountEmail,Value=$AWS_ACCOUNT_NAME@aws.openremote.io Key=AccountName,Value=$AWS_ACCOUNT_NAME"
  if [ -n "$OU" ]; then
    ROOT_ID=$(aws organizations list-roots --query "Roots[0].Id" --output text)
    OU_ID=$(aws organizations list-organizational-units-for-parent --parent-id $ROOT_ID --query "OrganizationalUnits[?Name=='$OU'].Id" --output text)

    if [ -z "$OU_ID" ]; then
      echo "Couldn't find Organizational Unit ID only one level of OUs from root is currently supported"
      exit 1
    fi
    # Need OU name and ID
    PARAMS="$PARAMS Key=ManagedOrganizationalUnit,Value=$OU($OU_ID)"
  fi
  aws servicecatalog provision-product --product-id $prod_id --provisioning-artifact-id $pa_id --provision-token $RandomToken --provisioned-product-name $CatalogName --provisioning-parameters $PARAMS

  if [ $? -ne 0 ]; then
    echo "Failed to provision account using Control Tower Account Factory"
    exit 1
  fi

  STATUS=$(aws servicecatalog scan-provisioned-products --query "ProvisionedProducts[?Name=='$CatalogName'].Status" --output text)
  count=0
  while [[ $STATUS != 'AVAILABLE' ]] && [ $count -lt 30 ]; do
    echo "Found provisioned account status $STATUS .. Sleeping 30 seconds"
    sleep 30
    STATUS=$(aws servicecatalog scan-provisioned-products --query "ProvisionedProducts[?Name=='$CatalogName'].Status" --output text)
    count=$((count+1))
  done

  if [ "$STATUS" != 'AVAILABLE' ]; then
    echo "Failed to provision account"
    exit 1
  fi

  # Get account ID of newly provisioned account
  ProvisionedProductId=$(aws servicecatalog scan-provisioned-products --query "ProvisionedProducts[?Name=='$CatalogName'].Id" --output text)
  AWS_ACCOUNT_ID=$(aws servicecatalog get-provisioned-product-outputs --provisioned-product-id $ProvisionedProductId --query "Outputs[?OutputKey=='AccountId'].OutputValue" --output text 2>/dev/null)
fi

if [ -z "$AWS_ACCOUNT_ID" ]; then
  echo "Failed to get ID of new account"
  exit 1
fi

# Store account ID in SSM (aws organizations list-accounts doesn't support server side filtering)
echo "Storing account ID for future reference"
aws ssm put-parameter --name "/Account-Ids/$AWS_ACCOUNT_NAME" --value "$AWS_ACCOUNT_ID" --type String &>/dev/null

# Wait for control tower customisations to complete
STATUS=$(aws codepipeline list-pipeline-executions --pipeline-name Custom-Control-Tower-CodePipeline --query "pipelineExecutionSummaries[0].status" --output text)
count=0
while [ "$STATUS" == 'InProgress' ] && [ $count -lt 30 ]; do
  echo "Code pipeline for control tower customisations status is $STATUS .. Sleeping 30 seconds"
  sleep 30
  STATUS=$(aws codepipeline list-pipeline-executions --pipeline-name Custom-Control-Tower-CodePipeline --query "pipelineExecutionSummaries[0].status" --output text)
  count=$((count+1))
done

if [ "$STATUS" != 'Succeeded' ]; then
  echo "Control tower customisation pipeline has failed cannot continue until this is resolved"
  exit 1
else
  echo "Control tower customisation pipeline has succeeded"
fi

# Update aws github-da profile with this account
source "${awsDir}set_github-da_account_arn.sh"

# Check/Create VPC
VPCID=$(aws ec2 describe-vpcs --filters Name=tag:Name,Values=or-vpc --query "Vpcs[0].VpcId" --output text $ACCOUNT_PROFILE 2>/dev/null)

if [ "$VPCID" == 'None' ]; then
  echo "Provisioning OR VPC Stack"
  # Create a new VPC with random IPv4 CIDR (so we can easily create peer connections between accounts)
  OCTET1=$(( $RANDOM % 255 ))
  OCTET2=$(( 5 * ($RANDOM % 51) ))
  VPCIP4CIDR="10.$OCTET1.$OCTET2.0/20"

  if [ -f "${awsDir}cloudformation-create-vpc.yml" ]; then
    TEMPLATE_PATH="${awsDir}cloudformation-create-vpc.yml"
  elif [ -f ".ci_cd/aws/cloudformation-create-vpc.yml" ]; then
    TEMPLATE_PATH=".ci_cd/aws/cloudformation-create-vpc.yml"
  elif [ -f "openremote/.ci_cd/aws/cloudformation-create-vpc.yml" ]; then
    TEMPLATE_PATH="openremote/.ci_cd/aws/cloudformation-create-vpc.yml"
  else
    echo "Cannot determine location of cloudformation-create-vpc.yml"
    exit 1
  fi

  STACK_NAME=or-vpc
  #Configure parameters
  PARAMS="ParameterKey=IPV4CIDR,ParameterValue='$VPCIP4CIDR'"

  # Create standard stack resources in specified account
  STACK_ID=$(aws cloudformation create-stack --stack-name $STACK_NAME --template-body file://$TEMPLATE_PATH --parameters $PARAMS --output text)

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

else
  echo "OR VPC already exists"
fi

# Get SSH Access security group ID
echo "Getting ssh-access security group ID"
SGID=$(aws ec2 describe-security-groups --filters Name=tag:Name,Values=ssh-access --query "SecurityGroups[0].GroupId" --output text $ACCOUNT_PROFILE 2>/dev/null)

# Get developers SSH public key from Parameter Store /SSH-Key/ and store as ~/.ssh/developers.pub
echo "Getting developers SSH public key"
mkdir ~/.ssh &>/dev/null

# Add developers SSH key to new account
DEVELOPERS_SSH=$(aws ssm get-parameter --name /SSH-Key/developers --query "Parameter.Value" --output text)

if [ $? -eq 0 ]; then
  echo "$DEVELOPERS_SSH" > ~/.ssh/developers.pub

  if [ -f ~/.ssh/developers.pub ]; then
    echo "Adding developers SSH public key to new Account"
    aws ec2 import-key-pair --key-name "developers" --public-key-material fileb://~/.ssh/developers.pub $ACCOUNT_PROFILE 1>/dev/null
  fi
else
  echo "Failed to retrieve developers SSH key cloud formation deployments will not succeed until this is resolved"
fi

# Create/remove hosted zone as requested
if [ -n "$PARENT_DNS_ZONE" ]; then
  HOSTED_ZONE="$AWS_ACCOUNT_NAME.$PARENT_DNS_ZONE"
  PARENT_HOSTED_ZONE_ID=$(aws route53 list-hosted-zones --query "HostedZones[?Name=='$PARENT_DNS_ZONE.'].Id" --output text 2>/dev/null)
  SUB_HOSTED_ZONE_ID=$(aws route53 list-hosted-zones --query "HostedZones[?Name=='$HOSTED_ZONE.'].Id" --output text $ACCOUNT_PROFILE 2>/dev/null)

  if [ -n "$PARENT_HOSTED_ZONE_ID" ]; then
    if [ "$HOSTED_DNS" == 'true' ]; then
      if [ -z "$SUB_HOSTED_ZONE_ID" ]; then
        echo "Creating sub domain hosted zone '$HOSTED_ZONE' in new account"
        SUB_HOSTED_ZONE_ID=$(aws route53 create-hosted-zone --name $HOSTED_ZONE --caller-reference $(date -u -Iseconds) --query "HostedZone.Id" --output text $ACCOUNT_PROFILE)
      fi

      if [ -n "$SUB_HOSTED_ZONE_ID" ]; then
        # Get name server record
        NS_RECORDS=$(aws route53 list-resource-record-sets --hosted-zone-id $SUB_HOSTED_ZONE_ID --query "ResourceRecordSets[?(Type=='NS' && Name=='$HOSTED_ZONE.')] | [0].ResourceRecords" $ACCOUNT_PROFILE)
        # Add name server record to PARENT_DNS_ZONE

read -r -d '' RECORDSET << EOF
{
  "Comment": "Creating NS record for account '$AWS_ACCOUNT_ID' subdomain",
  "Changes": [
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "$HOSTED_ZONE",
        "Type": "NS",
        "TTL": 300,
        "ResourceRecords": $NS_RECORDS
      }
    }
  ]
}
EOF
        aws route53 change-resource-record-sets --hosted-zone-id $PARENT_HOSTED_ZONE_ID --change-batch "$RECORDSET"
      fi
    else
      if [ -n "$SUB_HOSTED_ZONE_ID" ]; then
        echo "Removing existing sub domain hosted zone"
        aws route53 delete-hosted-zone --id $SUB_HOSTED_ZONE_ID $ACCOUNT_PROFILE
      fi
    fi
  else
    echo "Cannot find PARENT_DNS_ZONE '$PARENT_DNS_ZONE' so cannot configure DNS"
  fi
else
  echo "PARENT_DNS_ZONE not set so no DNS configuration will be attempted"
fi

# Update SSH Whitelist for this account
if [ -n "$SGID" ]; then
  "${awsDir}refresh_ssh_whitelist.sh" "" "" $AWS_ACCOUNT_ID
fi

# Provision EFS unless set to false
if [ "$PROVISION_EFS" != 'false' ]; then
  echo "Provisioning EFS for account '$AWS_ACCOUNT_NAME' using cloud formation template"

  if [ -f "${awsDir}cloudformation-create-efs.yml" ]; then
    TEMPLATE_PATH="${awsDir}cloudformation-create-efs.yml"
  elif [ -f ".ci_cd/aws/cloudformation-create-efs.yml" ]; then
    TEMPLATE_PATH=".ci_cd/aws/cloudformation-create-efs.yml"
  elif [ -f "openremote/.ci_cd/aws/cloudformation-create-efs.yml" ]; then
    TEMPLATE_PATH="openremote/.ci_cd/aws/cloudformation-create-efs.yml"
  else
    echo "Cannot determine location of cloudformation-create-efs.yml so cannot provision EFS filesystem"
  fi

  STACK_NAME='hosts-filesystem'

  # Check stack doesn't already exist
  STATUS=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[0].StackStatus" --output text $ACCOUNT_PROFILE 2>/dev/null)

  if [ -n "$STATUS" ] && [ "$STATUS" != 'DELETE_COMPLETE' ]; then
    echo "EFS stack already exists"
  else

    # Find Default VPC security group and IP CIDRs
    VPCIP4CIDR=$(aws ec2 describe-vpcs --filters Name=tag:Name,Values=or-vpc --query "Vpcs[0].CidrBlockAssociationSet[0].CidrBlock" --output text $ACCOUNT_PROFILE 2>/dev/null)
    VPCIP6CIDR=$(aws ec2 describe-vpcs --filters Name=tag:Name,Values=or-vpc --query "Vpcs[0].Ipv6CidrBlockAssociationSet[0].Ipv6CidrBlock" --output text $ACCOUNT_PROFILE 2>/dev/null)

    if [ "$VPCIP4CIDR" == 'None' ]; then
      unset VPCIP4CIDR
    fi
    if [ "$VPCIP6CIDR" == 'None' ]; then
      unset VPCIP6CIDR
    fi

    if [ -z "$VPCIP4CIDR" ] && [ -z "$VPCIP6CIDR" ]; then
      echo "OR VPC not found"
    else

      SGID=$(aws ec2 describe-security-groups --filters Name=vpc-id,Values=$VPCID --group-names 'default' --query "SecurityGroups[0].GroupId" --output text $ACCOUNT_PROFILE 2>/dev/null)

      if [ -z "$SGID" ]; then
        echo "Security group for OR VPC not found"
        exit 1
      else
        SUBNETIDS=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPCID --query "Subnets[*].SubnetId" --output text $ACCOUNT_PROFILE 2>/dev/null)

        if [ -z "$SUBNETIDS" ]; then
          echo "Subnets for OR VPC not found"
        else

          PARAMS="ParameterKey=SecurityGroupID,ParameterValue=$SGID"

          if [ -n "$VPCIP4CIDR" ]; then
            PARAMS="$PARAMS ParameterKey=VPCIP4CIDR,ParameterValue=$VPCIP4CIDR"
          fi
          if [ -n "$VPCIP6CIDR" ]; then
            PARAMS="$PARAMS ParameterKey=VPCIP6CIDR,ParameterValue=$VPCIP6CIDR"
          fi

          IFS=$' \t'
          count=1
          for SUBNETID in $SUBNETIDS; do
            PARAMS="$PARAMS ParameterKey=SubnetID$count,ParameterValue=$SUBNETID"
            count=$((count+1))
          done

          echo "Creating stack using cloudformation-create-efs.yml"
          STACK_ID=$(aws cloudformation create-stack --capabilities CAPABILITY_NAMED_IAM --stack-name $STACK_NAME --template-body file://$TEMPLATE_PATH --parameters $PARAMS --output text $ACCOUNT_PROFILE)

          if [ $? -ne 0 ]; then
            echo "Create stack failed"
          else
            echo "Create stack in progress"
          fi
        fi
      fi
    fi
  fi
fi
