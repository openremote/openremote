#!/bin/bash

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

if [ ! -z "$ENV_COMPOSE_FILE" ]; then
  if [ -f "profile/$ENVIRONMENT.yml" ]; then
    cp "profile/$ENVIRONMENT.yml" temp/docker-compose.yml
  elif [ -f "docker-compose.yml" ]; then
    cp docker-compose.yml temp/docker-compose.yml
  fi
elif [ -f "$ENV_COMPOSE_FILE" ]; then
  cp "$ENV_COMPOSE_FILE" temp/docker-compose.yml
else
  cp docker-compose.yml temp/docker-compose.yml
fi

if [ ! -f "temp/docker-compose.yml" ]; then
  echo "Docker compose file missing: 'temp/docker-compose.yml'"
  exit 1
fi

sshCommandPrefix="ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
scpCommandPrefix="scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"

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

# Get host platform
PLATFORM=$($sshCommandPrefix $hostStr -- uname -m)
if [ "$?" != 0 -o -z "$PLATFORM" ]; then
  echo "Failed to determine host platform"
  exit 1
fi
if [ "$PLATFORM" == "x86_64" ]; then
  PLATFORM="amd64"
fi
PLATFORM="linux/$PLATFORM"

# Create docker image tarballs as required
if [ "$MANAGER_TAG" != '#ref' ]; then
  docker manifest inspect openremote/manager:$MANAGER_TAG > /dev/null 2> /dev/null
  if [ $? != 0 ]; then
    echo "Specified manager tag does not exist in docker hub"
    exit 1
  fi
else
  echo "Using commit SHA for manager docker tag: $MANAGER_REF"
  MANAGER_TAG="$MANAGER_REF"
  # Export manager docker image for host platform
  docker build -o type=docker,dest=- --build-arg GIT_REPO=$REPO_NAME --build-arg GIT_COMMIT=$MANAGER_REF --platform $PLATFORM -t openremote/manager:$MANAGER_REF $MANAGER_DOCKER_BUILD_PATH | gzip > temp/manager.tar.gz
  if [ $? -ne 0 -o ! -f temp/manager.tar.gz ]; then
    echo "Failed to export manager image with tag: $MANAGER_REF"
    exit 1
  fi
fi

if [ ! -z "$DEPLOYMENT_REF" ]; then
  # Export deployment docker image for host platform
  docker build -o type=docker,dest=- --build-arg GIT_REPO=$REPO_NAME --build-arg GIT_COMMIT=$DEPLOYMENT_REF --platform $PLATFORM -t openremote/deployment:$DEPLOYMENT_REF $DEPLOYMENT_DOCKER_BUILD_PATH | gzip > temp/deployment.tar.gz
  if [ $? -ne 0 -o ! -f temp/deployment.tar.gz ]; then
    echo "Failed to export deployment image"
    exit 1
  fi
fi

if [ -d ".ci_cd/host_init" ]; then
  cp -r .ci_cd/host_init temp/
fi

# Set version variables
MANAGER_VERSION="$MANAGER_TAG"
DEPLOYMENT_VERSION="$DEPLOYMENT_REF"
echo "MANAGER_VERSION=\"$MANAGER_VERSION\"" >> temp/env
echo "DEPLOYMENT_VERSION=\"$DEPLOYMENT_VERSION\"" >> temp/env

echo "GZipping temp dir"
tar -zcvf temp.tar.gz temp

echo "Copying temp dir to host"
$scpCommandPrefix temp.tar.gz ${hostStr}:~

echo "Running deployment on host"
$sshCommandPrefix ${hostStr} << EOF
  echo "Removing host temp dir"
  rm -fr temp
  
  echo "Extracting temp dir"
  tar -xvzf temp.tar.gz
  
  chmod +x -R temp/
  
  set -a
  . ./temp/env
  set +a 
  
  if [ -f "temp/manager.tar.gz" ]; then
    docker load < temp/manager.tar.gz
  fi
  
  if [ -f "temp/deployment.tar.gz" ]; then
    docker load < temp/deployment.tar.gz
  fi
  
  # Run host init
  if [ "$HOST_INIT_SCRIPT" == 'NONE' -o "$HOST_INIT_SCRIPT" == 'none' ]; then
    echo "No host init requested"
  elif [ ! -z "$HOST_INIT_SCRIPT" ]; then
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
    $hostInitCmd
  fi
  
  # MAKE SURE WE HAVE CORRECT KEYCLOAK, PROXY AND POSTGRES IMAGES
  docker-compose -p or -f temp/docker-compose.yml pull --ignore-pull-failures
  
  # Attempt docker compose down
  docker-compose -f temp/docker-compose.yml -p or down

  # Delete postgres volume if CLEAN_INSTALL=true
  if [ $CLEAN_INSTALL == 'true' ]; then
    docker volume rm or_postgresql-data
  fi
  
  # Delete any deployment volume so we get the latest
  docker volume rm or_deployment-data

  # Start the stack
  docker-compose -f temp/docker-compose.yml -p or up -d
  
  if [ $? != 0 ];then
    echo "Deployment failed to start the stack"
    exit 1
  fi
  
  echo "Waiting for up to 5mins for standard services to be healthy"
  count=0
  ok=false
  while [ \$ok != 'true' ] && [ \$count -lt 36 ]; do
    echo \"attempt...\$count\"
    sleep 5
    postgresOk=false
    keycloakOk=false
    managerOk=false
    proxyOk=false
    if [ ! -z "\$(docker ps -aq -f health=healthy -f name=or_postgresql_1)" ]; then
      postgresOk=true
    fi
    if [ ! -z "\$(docker ps -aq -f health=healthy -f name=or_keycloak_1)" ]; then
      keycloakOk=true
    fi
    if [ ! -z "\$(docker ps -aq -f health=healthy -f name=or_manager_1)" ]; then
      managerOk=true
    fi
    if [ ! -z "\$(docker ps -aq -f health=healthy -f name=or_proxy_1)" ]; then
      proxyOk=true
    fi
    
    if [ \$postgresOk == 'true' -a \$keycloakOk == 'true' -a \$managerOk == 'true' -a \$proxyOk == 'true' ]; then
      ok=true
    fi
    
    count=\$((count+1))    
  done
  
  if [ \$ok != 'true' ]; then
    echo "Not all containers are healthy"
    exit 1
  else
    docker image prune -f -a
    docker volume prune -f
  fi
EOF

if [ $? -ne 0 ]; then
  echo "Deployment failed or is unhealthy"
  exit 1
fi

echo "Testing manager web server https://$HOST..."
response=$(curl --output /dev/null --silent --head --write-out "%{http_code}" https://$HOST/manager/)
if [ $response -ne 200 ]; then
  echo "Response code = $response"
  exit 1
fi
