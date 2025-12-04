#!/bin/bash

HOST=${1}

if [ -z "$HOST" ]; then
  echo "host must be set to use this script"
  exit 1
fi

# Search for instance
INSTANCE_ID=$(aws ec2 describe-instances --filters "Name=tag:Name,Values='$HOST'" "Name=instance-state-name,Values='running'" --query 'Reservations[].Instances[].InstanceId' --output text)

if [ -z "$INSTANCE_ID" ]; then
    echo "Could not find instance"
    exit 1
fi

# Retrieve root volume from host
echo "Retrieve root volume from host"
ROOT_DEVICE_NAME=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query 'Reservations[].Instances[].RootDeviceName' --output text)
ROOT_VOLUME_ID=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[].Instances[].BlockDeviceMappings[?DeviceName=='$ROOT_DEVICE_NAME'].Ebs.VolumeId" --output text)

echo "ROOT_DEVICE_NAME: $ROOT_DEVICE_NAME"
echo "ROOT_VOLUME_ID: $ROOT_VOLUME_ID"

# Create snapshot from root volume
echo "Create snapshot from root volume"
SNAPSHOT_ID=$(aws ec2 create-snapshot --description $HOST --volume-id $ROOT_VOLUME_ID --query "SnapshotId" --output text)

STATUS=$(aws ec2 describe-snapshots --snapshot-ids $SNAPSHOT_ID --query "Snapshots[].State" --output text)
while [[ "$STATUS" == 'pending' ]]; do
    PROGRESS=$(aws ec2 describe-snapshots --snapshot-ids $SNAPSHOT_ID --query "Snapshots[].Progress" --output text)
    echo "Snapshot creation in progress ... $PROGRESS completed .. Sleeping 120 seconds"
    sleep 120
    STATUS=$(aws ec2 describe-snapshots --snapshot-ids $SNAPSHOT_ID --query "Snapshots[].State" --output text)
done

if [ "$STATUS" != 'completed' ]; then
    echo "Snapshot creation has failed with status '$STATUS'"
    exit 1
else
    echo "Snapshot creation completed"
fi

# Disable root volume deletion when terminating instance
echo "Check if root volume is configured to be preserved"
DELETE_ON_TERMINATION=$(aws ec2 describe-volumes --volume-ids $ROOT_VOLUME_ID --query "Volumes[].Attachments[].DeleteOnTermination" --output text | tr 'A-Z' 'a-z')
if [ "$DELETE_ON_TERMINATION" != false ]; then
    echo "Root volume is configured to be deleted, change to false"
    DELETE_ON_TERMINATION=$(aws ec2 modify-instance-attribute --instance-id $INSTANCE_ID --block-device-mappings "[{\"DeviceName\": \"$ROOT_DEVICE_NAME\",\"Ebs\":{\"DeleteOnTermination\":false}}]")
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

STACK_NAME=$(tr '.' '-' <<< "$HOST")
ROOT_DISK_SIZE=$(aws ec2 describe-volumes --volume-ids $ROOT_VOLUME_ID --query "Volumes[].Size" --output text)

echo "STACK_NAME: $STACK_NAME"
echo "ROOT_DISK_SIZE: $ROOT_DISK_SIZE"

# Get role to be assumed by DLM
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_ROLE_NAME='developers-access'
AWS_REGION='eu-west-1'

ROLE_ARN="arn:aws:iam::$AWS_ACCOUNT_ID:role/$AWS_ROLE_NAME-$AWS_REGION"

# Configure parameters
echo "Configuring parameters"
PARAMS="ParameterKey=VpcId,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=SubnetId,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=SSHSecurityGroupId,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=SMTPUser,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=SMTPSecret,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=SMTPRegion,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=SMTPHost,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=Metrics,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=InstanceType,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=Host,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=HealthChecks,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=ElasticIP,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=EFSDNS,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=DataDiskSize,ParameterValue=$ROOT_DISK_SIZE"
PARAMS="$PARAMS ParameterKey=DNSHostedZoneRoleArn,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=DNSHostedZoneName,UsePreviousValue=true"
PARAMS="$PARAMS ParameterKey=DLMExecutionRoleArn,ParameterValue=$ROLE_ARN"

# Update CloudFormation Stack
echo "Updating CloudFormation Stack"
STACK_ID=$(aws cloudformation update-stack --capabilities CAPABILITY_IAM --stack-name $STACK_NAME --template-body file://$TEMPLATE_PATH --parameters $PARAMS --output text)

echo "STACK_ID: $STACK_ID"

if [ $? -ne 0 ]; then
  echo "Updating CloudFormation stack failed"
else
  echo "Updating CloudFormation stack in progress"
fi

STATUS=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[?StackId=='$STACK_ID'].StackStatus" --output text)
while [ "$STATUS" == 'UPDATE_IN_PROGRESS' ] && [ "$STATUS" == 'UPDATE_COMPLETE_CLEANUP_IN_PROGRESS' ]; do
  echo "Updating CloudFormation stack in progress .. Sleeping 120 seconds"
  sleep 120
  STATUS=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[?StackId=='$STACK_ID'].StackStatus" --output text)
done

if [ "$STATUS" != 'UPDATE_COMPLETE' ]; then
  echo "Updating CloudFormation stack has failed status is '$STATUS'"
  exit 1
else
  echo "CloudFormation stack succesfully updated"
fi

# Attach EBS data volume to instance
INSTANCE_ID=$(aws ec2 describe-instances --filters "Name=tag:Name,Values='$HOST'" "Name=instance-state-name,Values='running'" --query 'Reservations[].Instances[].InstanceId' --output text)
DATA_VOLUME_ID=$(aws ec2 describe-volumes --filters "Name=tag:Name,Values='$HOST-data'" --query "Volumes[].VolumeId" --output text)
EBS_DEVICE_NAME="/dev/sdf" # Don't change it unless you know what you are doing

# Configure parameters
PARAMS="InstanceId=$INSTANCE_ID,VolumeId=$DATA_VOLUME_ID,DeviceName=$EBS_DEVICE_NAME"

EXECUTION_ID=$(aws ssm start-automation-execution --document-name attach_volume --parameters $PARAMS --output text)

if [ $? -ne 0 ]; then
  echo "Attaching/Mounting EBS data volume failed"
  exit 1
fi

STATUS=$(aws ssm get-automation-execution --automation-execution-id $EXECUTION_ID --query "AutomationExecution.AutomationExecutionStatus" --output text)

while [[ "$STATUS" == 'InProgress' ]]; do
    echo "Attaching/Mounting EBS data volume is still in progress .. Sleeping 30 seconds"
    sleep 30
    STATUS=$(aws ssm get-automation-execution --automation-execution-id $EXECUTION_ID --query "AutomationExecution.AutomationExecutionStatus" --output text)
done

if [ "$STATUS" != 'Success' ]; then
  echo "Attaching/Mounting EBS data volume has failed status is '$STATUS'"
  exit 1
else
  echo "Attaching/Mounting EBS data volume is complete"
fi