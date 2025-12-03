#!/bin/bash

HOST=${1}

# Retrieve root volume from host to make a snapshot from.
echo 'Retrieve root volume for host'
INSTANCE_ID=$(aws ec2 describe-instances --filters "Name=tag:Name,Values='$HOST'" --query 'Reservations[].Instances[].InstanceId' --output text)
ROOT_DEVICE_NAME=$(aws ec2 describe-instances --filters "Name=tag:Name,Values='$HOST'" --query 'Reservations[].Instances[].RootDeviceName' --output text)

if [[ -n "$INSTANCE_ID" && "$ROOT_DEVICE_NAME" ]]; then
    VOLUME_ID=$(aws ec2 describe-volumes --filters "Name=attachment.instance-id,Values='$INSTANCE_ID'" --query "Volumes[].Attachments[?Device=='$ROOT_DEVICE_NAME'].VolumeId" --output text)

    if [ -n "$VOLUME_ID" ]; then
        echo "Found root volume for this host '$VOLUME_ID'"
    else
        echo "Could not find root volume"
        exit 1
    fi
else
    echo "Could not find host"
    exit 1
fi

# Create snapshot from root volume
echo 'Create snapshot from root volume'
SNAPSHOT_ID=$(aws ec2 create-snapshot --description $HOST --volume-id $VOLUME_ID --query 'SnapshotId' --output text)

STATUS=$(aws ec2 describe-snapshots --snapshot-ids $SNAPSHOT_ID --query "Snapshots[].[State, Progress]" --output text)
while [[ "$STATUS" == 'pending' ]]; do
    echo "Snapshot creation in progress..."
    sleep 30
    STATUS=$(aws ec2 describe-snapshots --snapshot-ids $SNAPSHOT_ID --query "Snapshots[].[State, Progress]" --output text)
done