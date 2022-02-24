#!/bin/bash

if [ ! -f "temp/docker-compose.yml" ]; then
  echo "Docker compose file missing: 'temp/docker-compose.yml'"
  exit 1
fi

if [ -f "temp/env" ]; then
  echo "Loading environment variables: 'temp/env'"
  set -a
  . ./temp/env
  set +x
fi

if [ -f "ssh.env" ]; then
  echo "Loading SSH password environment variable: 'ssh.env'"
  set -a
  . ./ssh.env
  set +x
fi

if [ -z "$HOST" ]; then
 echo "SSH Host is not set"
 exit 1
fi

sshCommandPrefix="ssh -q -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
scpCommandPrefix="scp -q -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"

if [ -f "$SSH_PORT" ]; then
  sshCommandPrefix="$sshCommandPrefix -p $SSH_PORT"
  scpCommandPrefix="$scpCommandPrefix -P $SSH_PORT"
fi

if [ -f "ssh.key" ]; then
  chmod 400 ssh.key
  sshCommandPrefix="$sshCommandPrefix -i ssh.key"
  scpCommandPrefix="$scpCommandPrefix -i ssh.key"
fi

hostStr="$HOST"

if [ ! -z "$SSH_USER" ]; then
  hostStr="${SSH_USER}@$hostStr"
fi

echo "GZipping temp dir"
tar -zcvf temp.tar.gz temp

echo "Copying temp dir to host"
$scpCommandPrefix temp.tar.gz ${hostStr}:~

echo "Running deployment on host"
echo $sshCommandPrefix ${hostStr} << EOF
  echo "Removing host temp dir"
  rm -fr temp
  
  echo "Extracting temp dir"
  tar -xvzf temp.tar.gz
  
  chmod +x -R temp/
  
  set -a
  . ./temp/env
  set +a 
  
  if [ -f "temp/manager.tar.gz" ]; then
    echo "docker load < temp/manager.tar.gz"
  fi
  
  if [ -f "temp/deployment.tar.gz" ]; then
    echo "docker load < temp/deployment.tar.gz"
  fi
  
  # Run host init
  if [ "$HOST_INIT_SCRIPT" == 'NONE' || "$HOST_INIT_SCRIPT" == 'none' ]; then
    echo "No host init requested"
  elif [ ! -z $HOST_INIT_SCRIPT ]; then
    if [ ! -f ".ci_cd/host_init/${HOST_INIT_SCRIPT}.sh" ]; then
      echo "HOST_INIT_SCRIPT (.ci_cd/host_init/${HOST_INIT_SCRIPT}.sh) does not exist"
      exit 1
    fi
    hostInitCmd=".ci_cd/host_init/${HOST_INIT_SCRIPT}.sh"
  elif [ -f ".ci_cd/host_init/$ENVIRONMENT.sh" ]; then
    hostInitCmd=".ci_cd/host_init/$ENVIRONMENT.sh"
  elif [ -f ".ci_cd/host_init/init.sh" ]; then
    hostInitCmd=".ci_cd/host_init/init.sh"
  fi  
  if [ ! -z "$hostInitCmd" ]; then
    echo "Running host init script: '$hostInitCmd'"
    echo "$hostInitCmd"
  fi
  
  # MAKE SURE WE HAVE CORRECT KEYCLOAK, PROXY AND POSTGRES IMAGES
  echo "docker-compose -p or -f temp/docker-compose.yml pull --ignore-pull-failures"
  
  # Attempt docker compose down
  echo "docker-compose -f temp/docker-compose.yml -p or down"

  # Delete postgres volume if CLEAN_INSTALL=true
  if [ $CLEAN_INSTALL == 'true' ]; then
    echo "docker volume rm or_postgresql-data"
  fi
  
  # Delete any deployment volume so we get the latest
  echo "docker volume rm or_deployment-data"

  # Start the stack
  echo "docker-compose -f temp/docker-compose.yml -p or up -d"
  
  if [ $? != 0 ];then
    echo "Deployment failed to start the stack"
    exit 1
  fi
  exit 0
  echo "Waiting for up to 5mins for manager web server https://$HOST..."
  count=0
  response=0
  while [ $response -ne 200 ] && [ $count -lt 36 ]; do
    response=$(curl --output /dev/null --silent --head --write-out "%{http_code}" https://$HOST/manager/)
    echo '.'
    count=$((count+1))
    sleep 5
  done
  if [ $response -ne 200 ]; then
    echo "Response code = $response"
    exit 1
  fi
EOF
