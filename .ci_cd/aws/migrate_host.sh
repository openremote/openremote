#!/bin/bash

HOST=${1}

if [ -z "$HOST" ]; then
  echo "Host must be set to use this script"
  exit 1
fi

echo "Start migration process"

# Retrieve Instance from host
INSTANCE_ID=$(aws ec2 describe-instances --filters "Name=tag:Name,Values='$HOST'" "Name=instance-state-name,Values='running'" --query 'Reservations[].Instances[].InstanceId' --output text)

echo "INSTANCE_ID: $INSTANCE_ID"

if [ -z "$INSTANCE_ID" ]; then
    echo "Could not find instance with name $HOST"
    exit 1
fi

# Retrieve root volume from host
echo "Retrieve root volume from host $HOST"
ROOT_DEVICE_NAME=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query 'Reservations[].Instances[].RootDeviceName' --output text)
ROOT_VOLUME_ID=$(aws ec2 describe-instances --instance-ids $INSTANCE_ID --query "Reservations[].Instances[].BlockDeviceMappings[?DeviceName=='$ROOT_DEVICE_NAME'].Ebs.VolumeId" --output text)

echo "ROOT_DEVICE_NAME: $ROOT_DEVICE_NAME"
echo "ROOT_VOLUME_ID: $ROOT_VOLUME_ID"

# Create snapshot from root volume
echo "Create snapshot from root volume $ROOT_VOLUME_ID"
SNAPSHOT_ID=$(aws ec2 create-snapshot --description $HOST --volume-id $ROOT_VOLUME_ID --query "SnapshotId" --output text)

echo "SNAPSHOT_ID: $SNAPSHOT_ID"

STATUS=$(aws ec2 describe-snapshots --snapshot-ids $SNAPSHOT_ID --query "Snapshots[].State" --output text)
while [[ "$STATUS" == 'pending' ]]; do
    PROGRESS=$(aws ec2 describe-snapshots --snapshot-ids $SNAPSHOT_ID --query "Snapshots[].Progress" --output text)
    echo "Snapshot creation in progress ... $PROGRESS completed .. Sleeping 60 seconds"
    sleep 60
    STATUS=$(aws ec2 describe-snapshots --snapshot-ids $SNAPSHOT_ID --query "Snapshots[].State" --output text)
done

if [ "$STATUS" != 'completed' ]; then
    echo "Snapshot creation has failed with status $STATUS"
    exit 1
else
    echo "Snapshot creation completed"
fi

# Check if root volume is configured to be preserved.
echo "Check if root volume $ROOT_VOLUME_ID is configured to be preserved"
DELETE_ON_TERMINATION=$(aws ec2 describe-volumes --volume-ids $ROOT_VOLUME_ID --query "Volumes[].Attachments[].DeleteOnTermination" --output text | tr 'A-Z' 'a-z')
if [ "$DELETE_ON_TERMINATION" != false ]; then
    echo "Root volume $ROOT_VOLUME_ID is configured to be deleted, change to false"
    DELETE_ON_TERMINATION=$(aws ec2 modify-instance-attribute --instance-id $INSTANCE_ID --block-device-mappings "[{\"DeviceName\": \"$ROOT_DEVICE_NAME\",\"Ebs\":{\"DeleteOnTermination\":false}}]")
fi

echo "Root volume configured to be preserved"

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
echo "ROOT_DISK_SIZE: $ROOT_DISK_SIZE GB"

AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_ROLE_NAME='developers-access'
AWS_REGION='eu-west-1'

# Get role to be assumed by DLM
ROLE_ARN="arn:aws:iam::$AWS_ACCOUNT_ID:role/$AWS_ROLE_NAME-$AWS_REGION"

# Configure parameters
echo "Configuring parameters for updating CloudFormation stack"
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
echo "Start updating CloudFormation stack"

STACK_ID=$(aws cloudformation update-stack --capabilities CAPABILITY_IAM --stack-name $STACK_NAME --template-body file://$TEMPLATE_PATH --parameters $PARAMS --output text)

echo "STACK_ID: $STACK_ID"

if [ $? -ne 0 ]; then
  echo "Updating CloudFormation stack failed"
fi

STATUS=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[?StackId=='$STACK_ID'].StackStatus" --output text)
while [ "$STATUS" == 'UPDATE_IN_PROGRESS' ] || [ "$STATUS" == 'UPDATE_COMPLETE_CLEANUP_IN_PROGRESS' ]; do
  echo "Updating CloudFormation stack in progress .. Sleeping 60 seconds"
  sleep 60
  STATUS=$(aws cloudformation describe-stacks --stack-name $STACK_NAME --query "Stacks[?StackId=='$STACK_ID'].StackStatus" --output text)
done

if [ "$STATUS" != 'UPDATE_COMPLETE' ]; then
  echo "Updating CloudFormation stack has failed with status $STATUS"
  exit 1
else
  echo "CloudFormation stack updated successfully"
fi

# Attach EBS data volume to instance
INSTANCE_ID=$(aws ec2 describe-instances --filters "Name=tag:Name,Values='$HOST'" "Name=instance-state-name,Values='running'" --query "Reservations[].Instances[?Tags[?Value=='$STACK_ID']].InstanceId" --output text)
DATA_VOLUME_ID=$(aws ec2 describe-volumes --filters "Name=tag:Name,Values='$HOST-data'" "Name=status,Values='available'" --query "Volumes[?Tags[?Value=='$STACK_ID']].VolumeId" --output text)

echo "INSTANCE_ID: $INSTANCE_ID"
echo "DATA_VOLUME_ID: $DATA_VOLUME_ID"

EBS_DATA_DEVICE_NAME="/dev/sdf" # Don't change it unless you know what you are doing

# Configure parameters
echo "Configuring parameters for attaching EBS data volume"
PARAMS="InstanceId=$INSTANCE_ID,VolumeId=$DATA_VOLUME_ID,DeviceName=$EBS_DATA_DEVICE_NAME"

echo "Start attaching EBS data volume to instance"

EXECUTION_ID=$(aws ssm start-automation-execution --document-name attach_volume --parameters $PARAMS --output text)

echo "EXECUTION_ID: $EXECUTION_ID"

if [ $? -ne 0 ]; then
  echo "Executing SSM automation has failed"
  exit 1
fi

STATUS=$(aws ssm get-automation-execution --automation-execution-id $EXECUTION_ID --query "AutomationExecution.AutomationExecutionStatus" --output text)

while [[ "$STATUS" == 'InProgress' ]]; do
    echo "Attaching EBS data volume in progress .. Sleeping 60 seconds"
    sleep 60
    STATUS=$(aws ssm get-automation-execution --automation-execution-id $EXECUTION_ID --query "AutomationExecution.AutomationExecutionStatus" --output text)
done

if [ "$STATUS" != 'Success' ]; then
  echo "Attaching EBS data volume has failed with status $STATUS"
  exit 1
else
  echo "EBS data volume successfully attached to instance $INSTANCE_ID"
fi

# Attach old root volume to instance
echo "Attaching old root volume to instance"

EBS_OLD_ROOT_DEVICE_NAME="/dev/sdb"

STATUS=$(aws ec2 attach-volume --volume-id $ROOT_VOLUME_ID --instance-id $INSTANCE_ID --device $EBS_OLD_ROOT_DEVICE_NAME --query "State" --output text)
while [[ "$STATUS" == 'attaching' ]]; do
    echo "Old root volume attaching in progress .. Sleeping 30 seconds"
    sleep 30
    STATUS=$(aws ec2 describe-volumes --volume-id $ROOT_VOLUME_ID --query "Volumes[].Attachments[].State" --output text)
done

if [ "$STATUS" != 'attached' ]; then
  echo "Attaching old root volume has failed with status $STATUS"
  exit 1
else
  echo "Old root volume successfully attached to instance $INSTANCE_ID"
fi

# Reboot instance

$(aws ec2 reboot-instances --instance-ids $INSTANCE_ID) # This ensures the (new) IPv4 will be updated in Route53

echo "Migration process finished ... Deploy OpenRemote Stack and move data folders"