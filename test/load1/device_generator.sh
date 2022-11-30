#!/bin/bash

# Generates a CSV file of devices with device ID and X509 cert for each

CA_KEY_PATH=../../setup/src/load1/resources/org/openremote/setup/load1/ca.key
CA_PEM_PATH=../../setup/src/load1/resources/org/openremote/setup/load1/ca.pem
DEVICE_PREFIX=device
TEMP_CERT_DIR=tmp
CSV_PATH="$TEMP_CERT_DIR/devices.csv"
COUNT=${1,,}

if [ -z "$COUNT" ]; then
 COUNT=100
fi

if [ -f "$CSV_PATH" ]; then
    echo "CSV file '$CSV_PATH' already exists"
    exit 1
fi

if [ -d "$TEMP_CERT_DIR" ]; then
    rm -f $TEMP_CERT_DIR
fi

mkdir $TEMP_CERT_DIR
touch $CSV_PATH

echo "Generating CSV file '$CSV_NAME' for $COUNT devices..."

    i=1
    while [ $i -le $COUNT ]; do
        echo "Generating $DEVICE_PREFIX$i..."
        MSYS_NO_PATHCONV=1 openssl req -nodes --newkey rsa:4096 -keyout $TEMP_CERT_DIR/$DEVICE_PREFIX$i.key -subj '/C=NL/ST=North Brabant/O=OpenRemote/CN='$DEVICE_PREFIX$i -out $TEMP_CERT_DIR/$DEVICE_PREFIX$i.csr
        MSYS_NO_PATHCONV=1 openssl x509 -req -in $TEMP_CERT_DIR/$DEVICE_PREFIX$i.csr -CA $CA_PEM_PATH -CAkey $CA_KEY_PATH -CAcreateserial -out $TEMP_CERT_DIR/$DEVICE_PREFIX$i.pem -days 500 -sha256

        if [ $? -ne 0 ]; then
          echo "Failed to sign device certificate"
          exit 1
        fi

        echo -n "$DEVICE_PREFIX$i," >> $CSV_PATH
        awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' $TEMP_CERT_DIR/$DEVICE_PREFIX$i.pem >> $CSV_PATH
        echo "" >> $CSV_PATH
        i=$((i+1))
    done


