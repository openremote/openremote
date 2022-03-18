#!/bin/bash

# Provisions the standard stack of resources using CloudFormation template (cloudformation-create-ec2.yml) in the
# specified AWS member account.
#
# To be called with arguments:
# 1 - OUs comma separated list of OUs to find accounts to apply SSH whitelist to
# 2 - Account Names to apply SSH whitelist to

if [[ $BASH_SOURCE = */* ]]; then
 awsDir=${BASH_SOURCE%/*}/
else
  awsDir=./
fi

OUS=$1
ACCOUNT_NAMES=$2
AWS_ENABLED=${3,,}

# Optionally login if AWS_ENABLED != 'true'
source "${awsDir}/login.sh"

if [ -n "$OUS" ]; then
  ROOT_ID=$(aws organizations list-roots --query "Roots[0].Id" --output text)
  IFS=$','
  for ou in $OUS; do
    OU_ID=$(aws organizations list-organizational-units-for-parent --parent-id $ROOT_ID --query "OrganizationalUnits[?Name=='$ou'].Id" --output text)
    if [ $? -ne 0 ]; then
      echo "Couldn't resolve OU '$ou'"
      exit 1
    fi
    ACCOUNT_IDS=$(aws organizations list-accounts-for-parent --parent-id ou-hbsh-9i66giju --query "Accounts[?Status=='ACTIVE'].Id" --output text)
  done
elif [ -n "$ACCOUNT_NAMES" ]; then
  IFS=$','
  for ACCOUNT_NAME in $ACCOUNT_NAMES; do
    # Get ACCOUNT_ID
    source "${awsDir}/get_account_id.sh" &>/dev/null
    if [ $? -ne 0 ]; then
      echo "Couldn't resolve Account name '$ACCOUNT_NAME'"
      exit 1
    fi
    ACCOUNT_IDS="$ACCOUNT_IDS $AWS_ACCOUNT_ID"
  done
else
  echo "Either OUs or Account names must be supplied"
  exit 1
fi

if [ -z "$ACCOUNT_IDS" ]; then
  echo "Failed to resolve OUs or accounts"
  exit 1
fi

SSH_LIST=$(aws ssm get-parameters-by-path --path "/SSH-Whitelist" --query "Parameters[*].[Name,Value]" --output text)
IFS=$' \t'
for AWS_ACCOUNT_ID in $ACCOUNT_IDS; do
  # Update github-da profile with ARN for ACCOUNT_ID
  source "${awsDir}/update_developers_access_profile.sh"

  echo "Revoking existing SSH Whitelist for Account '$AWS_ACCOUNT_ID'"
  LIST=$(aws ec2 describe-security-groups --profile github-da --filters Name=ip-permission.from-port,Values=22 Name=group-name,Values=ssh-access --query 'SecurityGroups[0].IpPermissions[?(IpProtocol==`tcp` && FromPort==`22`)]' --output json)
  if [ -n "$LIST" ] && [ "$LIST" != "null" ]; then
    aws ec2 revoke-security-group-ingress --profile github-da --group-name ssh-access --ip-permissions "$LIST"
  fi

  if [ -n "$SSH_LIST" ]; then
    IFS=$'\n'
    for entry in $SSH_LIST; do
      IFS=$' \t'
      entryArr=( $entry )
      echo "Adding SSH whitelist for '${entryArr[0]}' '${entryArr[1]}'"
      "$awsDir/ssh_whitelist.sh" "${entryArr[1]}" "github-da" "$AWS_ENABLED"
    done
  fi
done
