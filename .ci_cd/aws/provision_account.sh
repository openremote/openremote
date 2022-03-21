#!/bin/bash

# Provisions a member account of the specified name under the Custom OU using Control Tower Account Factory via Service Catalog
# The customisations for control tower will perform some standard customisations of the provisioned account as defined
# in the custom-control-tower-configuration CodeCommit repo. Also does the following:
#
# * Create Hosted domain zone (if requested) and will create the NS record in the PARENT_DNS_ZONE to delegate to the
#   new hosted zone; the zone will use the account name.
# * Adds SSH whitelist to the ssh-access security group in the new account (see set_ssh_whitelist.sh)
#
# Arguments:
# 1 - ACCOUNT_NAME - Name of the new account (required)
# 2 - PARENT_DNS_ZONE - name of parent hosted domain zone in management account
# 3 - HOSTED_DNS - If set to 'true' a sub domain hosted zone will be provisioned in the new account and delegated from the
#     PARENT_DNS_ZONE in the management account (e.g. openremote.app -> ACCOUNT_NAME.openremote.app)
# 4 - AWS_ENABLED indicates if login has already been successfully attempted and not required again

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

ACCOUNT_NAME=${1,,}
PARENT_DNS_ZONE=${2,,}
HOSTED_DNS=${3,,}
AWS_ENABLED=${4,,}

OU='Custom(ou-hbsh-9i66giju)'

if [ -z "$ACCOUNT_NAME" ]; then
  echo "ACCOUNT_NAME must be set"
  exit 1
fi

# Optionally login if AWS_ENABLED != 'true'
source "${awsDir}/login.sh"

# Get github account ID from github profile
CALLER_ACCOUNT_ID=$(aws sts get-caller-identity --query 'Account' --output text)

# Check account doesn't already exist
source "${awsDir}/get_account_id.sh"

if [ -n "$ACCOUNT_ID" ]; then
  echo "Account already exists so skipping initial provisioning step"
else
  RandomToken=$(echo $(( $RANDOM * 99999999999 )) | cut -c 1-13)
  prod_id=$(aws servicecatalog search-products --filters FullTextSearch='AWS Control Tower Account Factory' --query "ProductViewSummaries[*].ProductId" --output text)

  if [ $? -ne 0 ]; then
    echo "Failed to get product ID for Control Tower Account Factory you must be logged in to the management account"
    exit 1
  else
    echo "Provisioning product ID: $prod_id"
  fi

  pa_id=$(aws servicecatalog describe-product --id $prod_id --query "ProvisioningArtifacts[-1].Id" --output text)

  if [ $? -ne 0 ]; then
    echo "Failed to get provisioning artifact ID for Control Tower Account Factory"
    exit 1
  else
    echo "Provisioning artifact ID: $pa_id"
  fi

  CatalogName="account-$ACCOUNT_NAME"
  Params="Key=SSOUserFirstName,Value=$ACCOUNT_NAME Key=SSOUserLastName,Value=$ACCOUNT_NAME Key=SSOUserEmail,Value=$ACCOUNT_NAME@aws.openremote.io Key=AccountEmail,Value=$ACCOUNT_NAME@aws.openremote.io Key=AccountName,Value=$ACCOUNT_NAME Key=ManagedOrganizationalUnit,Value=$OU"
  aws servicecatalog provision-product --product-id $prod_id --provisioning-artifact-id $pa_id --provision-token $RandomToken --provisioned-product-name $CatalogName --provisioning-parameters $Params

  if [ $? -ne 0 ]; then
    echo "Failed to provision account using Control Tower Account Factory"
    exit 1
  fi

  STATUS=$(aws servicecatalog scan-provisioned-products --query "ProvisionedProducts[?Name=='$CatalogName'].Status" --output text)
  count=0
  while [[ $STATUS != 'AVAILABLE' ]] && [ $count -lt 20 ]; do
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
  ACCOUNT_ID=$(aws servicecatalog get-provisioned-product-outputs --provisioned-product-id $ProvisionedProductId --query "Outputs[?OutputKey=='AccountId'].OutputValue" --output text)

  # Store account ID in SSM (aws organizations list-accounts doesn't support server side filtering)
  if [ -n "$ACCOUNT_ID" ]; then
    echo "Storing account ID for future reference"
    aws ssm put-parameter --name "/Account-Ids/$ACCOUNT_NAME" --value "$ACCOUNT_ID" --type String &>/dev/null
  fi
fi

if [ -z "$ACCOUNT_ID" ]; then
  echo "Failed to get ID of new account"
  exit 1
fi

# Update developers access profile with ARN for ACCOUNT_ID
RETRY=true
source "${awsDir}/set_github-da_account_arn.sh"

# Wait for control tower customisations to complete
STATUS=$(aws codepipeline list-pipeline-executions --pipeline-name Custom-Control-Tower-CodePipeline --query "pipelineExecutionSummaries[0].status" --output text)
count=0
while [[ "$STATUS" != 'Succeeded' ]] && [ $count -lt 30 ]; do
  echo "Code pipeline for control tower customisations status is $STATUS .. Sleeping 30 seconds"
  sleep 30
  STATUS=$(aws codepipeline list-pipeline-executions --pipeline-name Custom-Control-Tower-CodePipeline --query "pipelineExecutionSummaries[0].status" --output text)
  count=$((count+1))
done

if [ "$STATUS" != 'Succeeded' ]; then
  echo "Control tower customisation pipeline has failed"
  exit 1
else
  echo "Control tower customisation pipeline has succeeded"
fi

# Get developers SSH public key from Parameter Store /SSH-Key/ and store as ~/.ssh/developers.pub
echo "Getting developers SSH public key"
mkdir ~/.ssh &>/dev/null

# Add developers SSH key to new account
DEVELOPERS_SSH=$(aws ssm get-parameter --name /SSH-Key/developers --query "Parameter.Value" --output text)

if [ $? -eq 0 ]; then
  echo "$DEVELOPERS_SSH" > ~/.ssh/developers.pub

  if [ -f ~/.ssh/developers.pub ]; then
    echo "Adding developers SSH public key to new Account"
    aws ec2 import-key-pair --key-name "developers" --public-key-material fileb://~/.ssh/developers.pub --profile github-da 1>/dev/null
  fi
else
  echo "Failed to retrieve developers SSH key cloud formation deployments will not succeed until this is resolved"
fi

# Authorise SMTP user in new account to use SES verified domain
echo "Authorising new account SMTP user to use openremote.io verified identity"
read -r -d '' POLICY << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::$ACCOUNT_ID:user/smtp"
      },
      "Action": [
        "ses:SendRawEmail",
        "ses:SendEmail"
      ],
      "Resource": "arn:aws:ses:$AWS_REGION:$CALLER_ACCOUNT_ID:identity/openremote.io",
      "Condition": {}
    }
  ]
}
EOF
aws ses put-identity-policy --identity openremote.io --policy-name smtp-$ACCOUNT_ID --policy "$POLICY" 1>/dev/null

# Create/remove hosted zone as requested
if [ -n "$PARENT_DNS_ZONE" ]; then
  HOSTED_ZONE="$ACCOUNT_NAME.$PARENT_DNS_ZONE"
  PARENT_HOSTED_ZONE_ID=$(aws route53 list-hosted-zones --query "HostedZones[?Name=='$PARENT_DNS_ZONE.'].Id" --output text 2>/dev/null)
  SUB_HOSTED_ZONE_ID=$(aws route53 list-hosted-zones --query "HostedZones[?Name=='$HOSTED_ZONE.'].Id" --output text --profile github-da 2>/dev/null)

  if [ -n "$PARENT_HOSTED_ZONE_ID" ]; then
    if [ "$HOSTED_DNS" == 'true' ]; then
      if [ -z "$SUB_HOSTED_ZONE_ID" ]; then
        echo "Creating sub domain hosted zone '$HOSTED_ZONE' in new account"
        SUB_HOSTED_ZONE_ID=$(aws route53 create-hosted-zone --name $HOSTED_ZONE --caller-reference $(date -u -Iseconds) --query "HostedZone.Id" --output text --profile github-da)
      fi

      if [ -n "$SUB_HOSTED_ZONE_ID" ]; then
        # Get name server record
        NS_RECORDS=$(aws route53 list-resource-record-sets --hosted-zone-id $SUB_HOSTED_ZONE_ID --query "ResourceRecordSets[?(Type=='NS' && Name=='$HOSTED_ZONE.')] | [0].ResourceRecords" --profile github-da)
        # Add name server record to PARENT_DNS_ZONE

read -r -d '' RECORDSET << EOF
{
  "Comment": "Creating NS record for account '$ACCOUNT_ID' subdomain",
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
        aws route53 delete-hosted-zone --id $SUB_HOSTED_ZONE_ID --profile github-da
      fi
    fi
  else
    echo "Cannot find PARENT_DNS_ZONE '$PARENT_DNS_ZONE' so cannot configure DNS"
  fi
else
  echo "PARENT_DNS_ZONE not set so no DNS configuration will be attempted"
fi

"${awsDir}/set_ssh_whitelist.sh"
