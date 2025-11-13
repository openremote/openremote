#!/bin/bash

# Generates a CSV file of devices with device ID and X509 cert for each
MAX_PROCESSES=5
CA_KEY_PATH=../../setup/src/load1/resources/org/openremote/setup/load1/ca.key
CA_PEM_PATH=../../setup/src/load1/resources/org/openremote/setup/load1/ca.pem
DEVICE_PREFIX=device
TEMP_CERT_DIR=tmp
COUNT=${1,,}
START=${2,,}
CSV_PATH=${3,,}

if [ -z "$COUNT" ]; then
 COUNT=100
fi

if [ -z "$START" ]; then
 START=1
fi

if [ -z "$CSV_PATH" ]; then
  CSV_PATH="devices.csv"
fi

if [ -f "$CSV_PATH" ]; then
    echo "CSV file '$CSV_PATH' already exists"
    exit 1
fi

if [ -d "$TEMP_CERT_DIR" ]; then
    rm -rf $TEMP_CERT_DIR
fi

mkdir -p $TEMP_CERT_DIR

if [ ! -f "$TEMP_CERT_DIR/device.key" ]; then
  echo "Generating a device private key"
  MSYS_NO_PATHCONV=1 openssl genrsa -out "$TEMP_CERT_DIR/device.pem" 2048
fi

# THIS DOESN'T WORK DUE TO FILE READ ISSUES ACROSS PROCESSES
#if [ $COUNT -gt 100 ]; then
#  # LIMIT PROCESSES TO 10
#  CHILD_COUNT=$(( ($COUNT + 100 - 1) / 100 ))
#  CHILD_COUNT=$(( $CHILD_COUNT > $MAX_PROCESSES ? $MAX_PROCESSES : $CHILD_COUNT ))
#  PER_PROCESS_COUNT=$(( ($COUNT + $CHILD_COUNT - 1) / $CHILD_COUNT ))
#  PER_PROCESS_COUNT=$(( $PER_PROCESS_COUNT < 100 ? 100 : $PER_PROCESS_COUNT ))
#  PER_PROCESS_COUNT_LAST=$(( $COUNT - (($CHILD_COUNT-1) * $PER_PROCESS_COUNT) ))
#  echo "Running in multi process mode, need to spawn $CHILD_COUNT processes"
#  trap 'kill 0' EXIT
#  i=1
#  while [ $i -le $CHILD_COUNT ]; do
#    echo "Starting process $i..."
#    DEVICE_COUNT=$(( $i == $CHILD_COUNT ? $PER_PROCESS_COUNT_LAST : $PER_PROCESS_COUNT ))
#    $0 $DEVICE_COUNT $(( (($i - 1) * $PER_PROCESS_COUNT) + 1 )) $TEMP_CERT_DIR/devices$i.csv &
#    i=$((i+1))
#  done
#  wait
#
#  # Patch files together
#  touch $CSV_PATH
#  i=1
#  while [ $i -le $CHILD_COUNT ]; do
#    echo "Merging devices from process $i into CSV file '$CSV_PATH'..."
#    cat $TEMP_CERT_DIR/devices$i.csv >> $CSV_PATH
#    i=$((i+1))
#  done
#
#  exit 0
#fi

echo "Generating CSV file '$CSV_PATH' for $COUNT devices starting at $START..."
touch $CSV_PATH

i=$START
while [ $i -le $(( $COUNT + $START -1 )) ]; do
    echo "Generating $DEVICE_PREFIX$i..."
    MSYS_NO_PATHCONV=1 openssl req -new -key "$TEMP_CERT_DIR/device.pem" -subj '/C=NL/ST=North Brabant/O=OpenRemote/CN='$DEVICE_PREFIX$i -out $TEMP_CERT_DIR/$DEVICE_PREFIX$i.csr 1>/dev/null 2>&1
    MSYS_NO_PATHCONV=1 openssl x509 -req -in $TEMP_CERT_DIR/$DEVICE_PREFIX$i.csr -CA $CA_PEM_PATH -CAkey $CA_KEY_PATH -CAcreateserial -out $TEMP_CERT_DIR/$DEVICE_PREFIX$i.pem -days 10000 -sha256 1>/dev/null 2>&1

    if [ $? -ne 0 ]; then
      echo "Failed to sign device certificate"
      exit 1
    fi

    echo -n "$DEVICE_PREFIX$i," >> $CSV_PATH
    awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' $TEMP_CERT_DIR/$DEVICE_PREFIX$i.pem >> $CSV_PATH
    echo "" >> $CSV_PATH
    i=$((i+1))
done


