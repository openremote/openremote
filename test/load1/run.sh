#!/bin/bash

# Runs the test on the servers requested ./run.sh server1 server2 server3; this script requires SSH access to each server
#
# server1 - Server where the OpenRemote Manager should run
# server2 - Server where the console-users test should run
# server3 = Server where the auto-provisioning test should run

SSH_PREFIX="ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
SCP_PREFIX="scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
DEPLOYMENT1_DOCKER_UP="docker compose -p test -f deployment1/test-load1.yml up -d"
DEPLOYMENT1_DOCKER_DOWN="docker compose -p test -f deployment1/test-load1.yml down && docker volume rm test_postgresql-data test_manager-data"
DEPLOYMENT2_DOCKER_EXEC="docker compose -p test -f deployment1/test-load1.yml up -d"
DEPLOYMENT3_DOCKER_EXEC="docker compose -p test -f deployment1/test-load1.yml up -d"
DEVICE_PREFIX=device
TEMP_CERT_DIR=tmp
SERVER1=${1,,}
SERVER2=${2,,}
SERVER3=${3,,}

invalid_args() {
  echo "Usage:"
  echo ""
  echo "./run.sh server1.com server2.com server3.com"
  echo ""
  echo "You must have SSH access already configured for each server"
}


if [ -z "$SERVER1" ] || [ -z "$SERVER2" ] || [ -z "$SERVER3" ]; then
 invalid_args
 exit 1
fi

# Build setup JAR and prepare the directories
cd ../../
./gradlew :setup:load1Jar
cd test
mkdir -p build/deployment1/manager/extensions build/deployment2/results build/deployment3/results
cp ../setup/build/libs/openremote-load1-setup-0.0.0.jar build/deployment1/manager/extensions
cp ../profile/test-load1.yml build/deployment1
cp load1/console-users.jmx load1/console-users.yml build/deployment2
cp load1/auto-provisioning.jmx load1/auto-provisioning.yml build/deployment3
cd build

if [ "$SERVER1" == "localhost" ]; then
  echo "Deploying on localhost deployment1"
  docker-compose -p test -f deployment1/test-load1.yml up -d
else
  echo "Copying files to remote host deployment1 -> $SERVER1"
  $SCP_PREFIX -r deployment1 $SERVER1:~
  echo "Deploying on remote host deployment1 -> $SERVER1"
  $SSH_PREFIX $SERVER1 << EOF

cd deployment1

if ! [ -f .env ]; then
  OR_ADMIN_PASSWORD=\$(date +%s | sha256sum | base64 | head -c 15)
  echo "OR_ADMIN_PASSWORD: \$OR_ADMIN_PASSWORD"
  touch .env
  echo OR_ADMIN_PASSWORD=\$OR_ADMIN_PASSWORD >> .env
else
  echo "Env variables:"
  cat .env
fi

echo "Stopping any existing stack"
OR_HOSTNAME=$SERVER1 docker-compose -p test -f test-load1.yml down
# Prune old data
docker volume rm test_manager-data test_postgresql-data
OR_HOSTNAME=$SERVER1 docker-compose -p test -f test-load1.yml pull
OR_HOSTNAME=$SERVER1 docker-compose -p test -f test-load1.yml up -d

if [ \$? -ne 0 ]; then
  exit 1
fi

docker image prune -af

echo "Waiting for up to 10mins for all services to be healthy"
COUNT=1
STATUSES_OK=false
IFS=\$'\n'
while [ "\$STATUSES_OK" != 'true' ] && [ \$COUNT -le 60 ]; do

   echo "Checking service health...attempt \$COUNT"
   STATUSES=\$(docker ps --format "{{.Names}} {{.Status}}")
   STATUSES_OK=true

   for STATUS in \$STATUSES; do
     if [[ "\$STATUS" != *"healthy"* ]]; then
       STATUSES_OK=false
       break
     fi
   done

   if [ "\$STATUSES_OK" == 'true' ]; then
      break
   fi

   sleep 10
   COUNT=\$((COUNT+1))
done

if [ "\$STATUSES_OK" == 'true' ]; then
  echo "All services are healthy"
else
  echo "One or more services are unhealthy"
  docker ps -a
  exit 1
fi

EOF
fi

if [ $? -ne 0 ]; then
  echo "Failed to deploy deployment1"
  exit 1
else
  echo "Deployed deployment1"
fi

if [ "$SERVER2" == "localhost" ]; then
  echo "Deploying on localhost deployment2"

else
  echo "Copying files to remote host deployment2 -> $SERVER2"
  $SCP_PREFIX -r deployment2 $SERVER2:~
  echo "Deploying on remote host deployment2 -> $SERVER2"
  $SSH_PREFIX $SERVER2 << EOF

cd deployment2
#CONTAINER_ID=\$(docker run --rm -d -v \$PWD:/bzt-configs -v \$PWD/results:/tmp/artifacts openremote/jmeter-taurus -o settings.env.MANAGER_HOSTNAME=$SERVER1 -o settings.env.DURATION=30 -o settings.env.THREAD_COUNT=10 console-users.yml)
docker run --rm -d -v \$PWD:/bzt-configs -v \$PWD/results:/tmp/artifacts openremote/jmeter-taurus -o settings.env.MANAGER_HOSTNAME=$SERVER1 -o settings.env.DURATION=30 -o settings.env.THREAD_COUNT=10 console-users.yml

if [ \$? -ne 0 ]; then
  exit 1
fi

echo "Test is now running"

EOF
fi

if [ $? -ne 0 ]; then
  echo "Failed to deploy deployment2"
  exit 1
else
  echo "Deployed deployment2"
fi

exit 0

if [ "$SERVER3" == "localhost" ]; then
  echo "Deploying on localhost deployment3"

else
  echo "Copying files to remote host deployment3 -> $SERVER3"
  $SCP_PREFIX -r deployment3 $SERVER3:~
  echo "Deploying on remote host deployment3 -> $SERVER3"

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
