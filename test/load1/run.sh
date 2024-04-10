#!/bin/bash

# Runs the test on the servers requested ./run.sh server1 server2 server3; this script requires SSH access to each server
#
# server1 - Server where the OpenRemote Manager should run (deployment 1)
# server2 - Server where the console-users test should run (deployment 2)
# server3 = Server where the auto-provisioning test should run (deployment 3)
#
# The following environment variables can be used:
#
# `DEPLOYMENT1_DO_NOTHING` (default: false) - Do not do anything with deployment1; useful when running the test multiple times to speed up deployment
# `DEPLOYMENT1_RESTART_ONLY` (default: false) - Will only restart the manager container on deployment 1 rather than the default behaviour of stack down/up
# `CONSOLE_THREAD_COUNT` (default: unset) - Console user test `THREAD_COUNT` (set to `0` to skip deployment 2 test)
# `CONSOLE_DURATION` (default: unset) - Console user test `DURATION`
# `CONSOLE_RAMP_RATE` (default: unset) - Console user test `RAMP_RATE`
# `DEVICE_THREAD_COUNT` (default: unset) - Auto provisioning device test `THREAD_COUNT` (set to `0` to skip deployment 3 test)
# `DEVICE_DURATION` (default: unset) - Auto provisioning device test `DURATION`
# `DEVICE_RAMP_RATE` (default: unset) - Auto provisioning device test `RAMP_RATE`
# `DEVICE_TIME_BETWEEN_PUBLISHES` (default: unset) - Auto provisioning device test `TIME_BETWEEN_PUBLISHES`


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
cp -r load1/deployment1 build
cp load1/console-users.jmx load1/console-users.yml build/deployment2
cp load1/auto-provisioning.jmx load1/devices.csv load1/auto-provisioning.yml build/deployment3
cd build

if [ "$DEPLOYMENT1_DO_NOTHING" == "true" ]; then
  echo "Skipping deployment1 (assuming stack already running and will be reused)"
else
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

if [ "$DEPLOYMENT1_RESTART_ONLY" == "true" ]; then
  echo "Skipping forced redeploy of manager instance, will just try restart"
  docker restart test-manager-1 1>/dev/null 2>&1
else
  echo "Stopping any existing stack"
  OR_HOSTNAME=$SERVER1 docker-compose -p test -f test-load1.yml down
  # Prune old data
  docker volume rm test_manager-data test_postgresql-data
  OR_HOSTNAME=$SERVER1 docker-compose -p test -f test-load1.yml pull --ignore-pull-failures
fi

$DEPLOYMENT1_ENV OR_HOSTNAME=$SERVER1 docker-compose -p test -f test-load1.yml up -d
if [ \$? -ne 0 ]; then
  echo "Failed to start the stack"
  exit 1
fi
docker image prune -af 1>/dev/null 2>&1

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
    echo "Failed to deploy deployment 1"
    exit 1
  else
    echo "Deployed deployment 1"
  fi
fi

# DEPLOYMENT 2
if [ "$CONSOLE_THREAD_COUNT" != "0" ]; then
  SETTINGS="-o settings.env.MANAGER_HOSTNAME=$SERVER1"
  if [ -n "$CONSOLE_THREAD_COUNT" ]; then
    SETTINGS="$SETTINGS -o settings.env.THREAD_COUNT=$CONSOLE_THREAD_COUNT"
  fi
  if [ -n "$CONSOLE_DURATION" ]; then
    SETTINGS="$SETTINGS -o settings.env.DURATION=$CONSOLE_DURATION"
  fi
  if [ -n "$CONSOLE_RAMP_RATE" ]; then
    SETTINGS="$SETTINGS -o settings.env.RAMP_RATE=$CONSOLE_RAMP_RATE"
  fi

  COMMAND="cd deployment2; docker run --rm -d --name deployment2 -v \$PWD:/bzt-configs -v \$PWD/results:/tmp/artifacts openremote/jmeter-taurus $SETTINGS console-users.yml; cd .."
  echo "Deployment 2 launch command: $COMMAND"

  if [ "$SERVER2" == "localhost" ]; then
    echo "Deploying on localhost"
    cd deployment2
    MSYS_NO_PATHCONV=1 docker run --rm -d --name deployment2 -v \$PWD:/bzt-configs -v \$PWD/results:/tmp/artifacts openremote/jmeter-taurus $SETTINGS console-users.yml
    cd ..
  else
    echo "Copying files to remote host deployment2 -> $SERVER2"
    $SCP_PREFIX -r deployment2 $SERVER2:~
    echo "Deploying on remote host deployment2 -> $SERVER2"
    $SSH_PREFIX $SERVER2 << EOF

$COMMAND

if [ \$? -ne 0 ]; then
  exit 1
fi

echo "Test container is now running"

EOF
  fi

  if [ $? -ne 0 ]; then
    echo "Failed to deploy deployment 2"
    exit 1
  else
    echo "Deployed deployment 2"
  fi
else
  echo "Skipping deployment 2"
fi

# DEPLOYMENT 3
if [ "$DEVICE_THREAD_COUNT" != "0" ]; then
  SETTINGS="-o settings.env.MANAGER_HOSTNAME=$SERVER1"
  if [ -n "$DEVICE_THREAD_COUNT" ]; then
    SETTINGS="$SETTINGS -o settings.env.THREAD_COUNT=$DEVICE_THREAD_COUNT"
  fi
  if [ -n "$DEVICE_DURATION" ]; then
    SETTINGS="$SETTINGS -o settings.env.DURATION=$DEVICE_DURATION"
  fi
  if [ -n "$DEVICE_RAMP_RATE" ]; then
    SETTINGS="$SETTINGS -o settings.env.RAMP_RATE=$DEVICE_RAMP_RATE"
  fi
  if [ -n "$DEVICE_TIME_BETWEEN_PUBLISHES" ]; then
    SETTINGS="$SETTINGS -o settings.env.TIME_BETWEEN_PUBLISHES=$DEVICE_TIME_BETWEEN_PUBLISHES"
  fi

  COMMAND="cd deployment3; docker run --rm -d --name deployment3 -v \$PWD:/bzt-configs -v \$PWD/results:/tmp/artifacts openremote/jmeter-taurus $SETTINGS auto-provisioning.yml; cd .."
  echo "Deployment 3 launch command: $COMMAND"

  if [ "$SERVER3" == "localhost" ]; then
    echo "Deploying on localhost deployment3"
    cd deployment3
    MSYS_NO_PATHCONV=1 docker run --rm -d --name deployment3 -v $PWD:/bzt-configs -v $PWD/results:/tmp/artifacts openremote/jmeter-taurus $SETTINGS auto-provisioning.yml
    cd ..
  else
    echo "Copying files to remote host deployment3 -> $SERVER3"
    $SCP_PREFIX -r deployment3 $SERVER3:~
    echo "Deploying on remote host deployment3 -> $SERVER3"
    $SSH_PREFIX $SERVER3 << EOF

$COMMAND

if [ \$? -ne 0 ]; then
  exit 1
fi

echo "Test container is now running"

EOF
  fi

  if [ $? -ne 0 ]; then
    echo "Failed to deploy deployment 3"
    exit 1
  else
    echo "Deployed deployment 3"
  fi
else
  echo "Skipping deployment 3"
fi

echo "Waiting for test runners to finish"

if [ "$CONSOLE_THREAD_COUNT" != "0" ]; then
  if [ "$SERVER2" == "localhost" ]; then
    while [ -n "$(docker ps -q -f name=deployment2)" ]; do
      echo "Waiting for deployment 2 container to exit..."
      sleep 10
    done

    echo "Deployment 2 container has finished"

  else
    $SSH_PREFIX $SERVER2 << EOF

while [ -n "\$(docker ps -q -f name=deployment2)" ]; do
  echo "Waiting for deployment 2 container to exit..."
  sleep 10
done

echo "Deployment 2 container has finished"

EOF
  fi
fi

if [ "$DEVICE_THREAD_COUNT" != "0" ]; then
  if [ "$SERVER3" == "localhost" ]; then
    while [ -n "$(docker ps -q -f name=deployment3)" ]; do
      echo "Waiting for deployment 3 container to exit..."
      sleep 10
    done

    echo "Deployment 3 container has finished"

  else
    $SSH_PREFIX $SERVER3 << EOF

while [ -n "\$(docker ps -q -f name=deployment3)" ]; do
  echo "Waiting for deployment 3 container to exit..."
  sleep 10
done

echo "Deployment 3 container has finished"

EOF
  fi
fi
