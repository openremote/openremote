#!/bin/bash

# Must be run from the repo root dir

# Function to be called before exiting to remove runner from AWS ssh-access security group
revoke_ssh () {
  if [ "$SSH_GRANTED" == 'true' ]; then
      if [ -n "$CIDR" ]; then
        "temp/aws/ssh_revoke.sh" "$CIDR" "github-da" "$AWS_ENABLED"
      fi
  fi
}

# Load the environment variables into this session
if [ -f "temp/env" ]; then
  echo "Loading environment variables: 'temp/env'"
  set -a
  . ./temp/env
  set +x

  echo "Environment variables loaded:"
  cat temp/env
fi

# Check host is defined
if [ -z "$OR_HOST" ]; then
 echo "Host is not set"
 exit 1
fi

# Load SSH environment variables into this session
if [ -f "ssh.env" ]; then
  echo "Loading SSH password environment variable: 'ssh.env'"
  set -a
  . ./ssh.env
  set +x
fi

# Copy CI/CD files into temp dir
echo "Copying CI/CD files into temp dir"
if [ "$IS_CUSTOM_PROJECT" == 'true' ]; then
  cp -r openremote/.ci_cd/host_init temp/
  cp -r openremote/.ci_cd/aws temp/
fi
if [ -d ".ci_cd/host_init" ]; then
  cp -r .ci_cd/host_init temp/
fi
if [ -d ".ci_cd/aws" ]; then
  cp -r .ci_cd/aws temp/
fi

# Determine compose file to use and copy to temp dir (do this here as all env variables are loaded)
if [ -z "$ENV_COMPOSE_FILE" ]; then
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
# Check docker compose file is present
if [ ! -f "temp/docker-compose.yml" ]; then
  echo "Couldn't determine docker compose file"
  exit 1
fi

# Set SSH/SCP command variables
sshCommandPrefix="ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
scpCommandPrefix="scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
if [ -n "$SSH_PORT" ]; then
  sshCommandPrefix="$sshCommandPrefix -p $SSH_PORT"
  scpCommandPrefix="$scpCommandPrefix -P $SSH_PORT"
fi
if [ -f "ssh.key" ]; then
  chmod 400 ssh.key
  sshCommandPrefix="$sshCommandPrefix -i ssh.key"
  scpCommandPrefix="$scpCommandPrefix -i ssh.key"
fi
hostStr="$OR_HOST"
if [ -n "$SSH_USER" ]; then
  hostStr="${SSH_USER}@$hostStr"
fi

# Check host is reachable (ping must be enabled)
if [ "$SKIP_HOST_PING" != 'true' ]; then
  echo "Attempting to ping host"
  ping -c1 -W1 -q $OR_HOST &>/dev/null
  if [ $? -ne 0 ]; then
    echo "Host is not reachable by PING"
    if [ "$SKIP_AWS_EC2_START" != 'true' ] && [ "$AWS_ENABLED" == 'true' ]; then
      "temp/aws/start_ec2_instance.sh" "$OR_HOST" "$AWS_ENABLED"
      if [ $? -ne 0 ]; then
        # Don't exit as it might just not be reachable by PING we'll fail later on
        echo "EC2 instance start failed"
      else
        echo "EC2 instance start succeeded"
      fi
    fi
  fi
fi

# Grant SSH access to this runner's public IP on AWS
if [ "$SKIP_SSH_WHITELIST" != 'true' ]; then
  echo "Attempting to add runner to AWS SSH whitelist"
  if [ "$AWS_ENABLED" == 'true' ]; then
    if [ -n "$CIDR" ]; then
      "temp/aws/ssh_whitelist.sh" "$CIDR" "github-da" "$AWS_ENABLED"
    fi
    if [ $? -eq 0 ]; then
      SSH_GRANTED=true
    fi
  else
    echo "AWS not enabled so cannot grant SSH access"
  fi
fi

# Determine host platform via ssh for deployment image building (can't export/import manifests)
PLATFORM=$($sshCommandPrefix $hostStr -- uname -m)
if [ "$?" != 0 -o -z "$PLATFORM" ]; then
  echo "Failed to determine host platform, most likely SSH credentials and/or settings are invalid"
  revoke_ssh
  exit 1
fi
if [ "$PLATFORM" == "x86_64" ]; then
  PLATFORM="amd64"
fi
PLATFORM="linux/$PLATFORM"


# Verify manager tag and create docker image tarballs as required
if [ "$MANAGER_TAG" != '#ref' ]; then
  docker manifest inspect openremote/manager:$MANAGER_TAG > /dev/null 2> /dev/null
  if [ $? -ne 0 ]; then
    echo "Specified manager tag does not exist in docker hub"
    revoke_ssh
    exit 1
  fi
else
  echo "Using commit SHA for manager docker tag: $MANAGER_REF"
  MANAGER_TAG="$MANAGER_REF"
  # Export manager docker image for host platform
  docker build -o type=docker,dest=- --build-arg GIT_REPO=$REPO_NAME --build-arg GIT_COMMIT=$MANAGER_REF --platform $PLATFORM -t openremote/manager:$MANAGER_REF $MANAGER_DOCKER_BUILD_PATH | gzip > temp/manager.tar.gz
  if [ $? -ne 0 ] || [ ! -f temp/manager.tar.gz ]; then
    echo "Failed to export manager image with tag: $MANAGER_REF"
    revoke_ssh
    exit 1
  fi
fi
if [ -n "$DEPLOYMENT_REF" ]; then
  # Export deployment docker image for host platform
  docker build -o type=docker,dest=- --build-arg GIT_REPO=$REPO_NAME --build-arg GIT_COMMIT=$DEPLOYMENT_REF --platform $PLATFORM -t openremote/deployment:$DEPLOYMENT_REF $DEPLOYMENT_DOCKER_BUILD_PATH | gzip > temp/deployment.tar.gz
  if [ $? -ne 0 ] || [ ! -f temp/deployment.tar.gz ]; then
    echo "Failed to export deployment image"
    revoke_ssh
    exit 1
  fi
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
  echo "Removing old temp deployment dir"
  rm -fr temp
  
  echo "Extracting temp dir"
  tar -xvzf temp.tar.gz
  chmod +x -R temp/
  
  set -a
  . ./temp/env
  set +a 

  # Login to AWS if credentials provided
  AWS_KEY=$AWS_KEY
  AWS_SECRET=$AWS_SECRET
  AWS_REGION=$AWS_REGION
  source temp/aws/login.sh

  if [ -f "temp/manager.tar.gz" ]; then
    echo "Loading manager docker image"
    docker load < temp/manager.tar.gz
  fi
  
  if [ -f "temp/deployment.tar.gz" ]; then
    echo "Loading deployment docker image"
    docker load < temp/deployment.tar.gz
  fi

  # Make sure we have correct keycloak, proxy and postgres images
  echo "Pulling requested service versions from docker hub"
  docker-compose -p or -f temp/docker-compose.yml pull --ignore-pull-failures

  if [ \$? -ne 0 ]; then
    echo "Deployment failed to pull docker images"
    exit 1
  fi

  # Attempt docker compose down
  echo "Stopping existing stack"
  docker-compose -f temp/docker-compose.yml -p or down 2> /dev/null

  if [ \$? -ne 0 ]; then
    echo "Deployment failed to stop the existing stack"
    exit 1
  fi

  # Run host init
  hostInitCmd=
  if [ "$HOST_INIT_SCRIPT" == 'NONE' -o "$HOST_INIT_SCRIPT" == 'none' ]; then
    echo "No host init requested"
  elif [ -n "$HOST_INIT_SCRIPT" ]; then
    if [ ! -f "temp/host_init/${HOST_INIT_SCRIPT}.sh" ]; then
      echo "HOST_INIT_SCRIPT (temp/host_init/${HOST_INIT_SCRIPT}.sh) does not exist"
      exit 1
    fi
    hostInitCmd="temp/host_init/${HOST_INIT_SCRIPT}.sh"
  elif [ -f "temp/host_init/$ENVIRONMENT.sh" ]; then
    hostInitCmd="temp/host_init/$ENVIRONMENT.sh"
  elif [ -f "temp/host_init/init.sh" ]; then
    hostInitCmd="temp/host_init/init.sh"
  fi
  if [ -n "$hostInitCmd" ]; then
    echo "Running host init script: '$hostInitCmd'"
    sudo $hostInitCmd
  else
    echo "No host init script"
  fi

  # Delete any deployment volume so we get the latest
  echo "Deleting existing deployment data volume"
  docker volume rm or_deployment-data 1>/dev/null



  # Mount EFS map if specified
  if [ "$AWS_ENABLED" == 'true' ] && [ -n "\$AWS_EFS_MAP" ]; then
    echo "Attempting to mount EFS map data"
    fileSystemId=\$(aws efs describe-file-systems --query "FileSystems[?Name==\'\$AWS_EFS_MAP\'].FileSystemId" --output text)
    if [ -z "\$fileSystemId" ]; then
      echo "Requested EFS volume named '$AWS_EFS_MAP' not found"
      exit 1
    fi
    mkdir -p /deployment.local/map
    echo "Adding EFS mount to /etc/fstab"
    echo "\$fileSystemId.efs.$AWS_REGION.amazonaws.com:/ /deployment.local/map nfs4 nfsvers=4.1 0 0" >> /etc/fstab
    echo "Attempting to mount EFS volume"
    mount -a -v
  fi

  # Copy any S3 deployment files
  if [ "$AWS_ENABLED" == 'true' ] && [ "\$SKIP_AWS_S3_FILECOPY" != 'true' ]; then
    echo "Looking for host specific deployment files on S3 at 'openremote-hosts/$OR_HOST/deployment_files'"
    aws s3 ls openremote-hosts/$OR_HOST/deployment_files &>/dev/null
    if [ \$? -ne 0 ]; then
      echo "No host specific deployment files found on S3"
    else
      echo "Files found; copying to /deployment.local/s3"
      mkdir -p /deployment.local/s3
      aws s3 cp s3://openremote-hosts/$OR_HOST/deployment_files/ /deployment.local/s3/ --recursive
    fi
  fi

  # Start the stack
  echo "Starting the stack"
  docker-compose -f temp/docker-compose.yml -p or up -d
  
  if [ \$? -ne 0 ]; then
    echo "Deployment failed to start the stack"
    exit 1
  fi
  
  echo "Waiting for up to 5mins for standard services to be healthy"
  count=0
  ok=false
  while [ \$ok != 'true' ] && [ \$count -lt 60 ]; do
    echo \"attempt...\$count\"
    sleep 5
    postgresOk=false
    keycloakOk=false
    managerOk=false
    proxyOk=false
    if [ -n "\$(docker ps -aq -f health=healthy -f name=or_postgresql_1)" ]; then
      postgresOk=true
    fi
    if [ -n "\$(docker ps -aq -f health=healthy -f name=or_keycloak_1)" ]; then
      keycloakOk=true
    fi
    if [ -n "\$(docker ps -aq -f health=healthy -f name=or_manager_1)" ]; then
      managerOk=true
    fi
    if [ -n "\$(docker ps -aq -f health=healthy -f name=or_proxy_1)" ]; then
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
  fi

  # Cleanup obsolete docker data
  docker image prune -f -a
  docker volume prune -f
  docker image inspect $(docker image ls -aq) > temp/image-info.txt
  docker inspect $(docker ps -aq) > temp/container-info.txt

  # Copy files to S3
EOF

if [ $? -ne 0 ]; then
  echo "Deployment failed or is unhealthy"
  revoke_ssh
  exit 1
fi

echo "Testing manager web server https://$OR_HOST..."
response=$(curl --output /dev/null --silent --head --write-out "%{http_code}" https://$OR_HOST/manager/)
if [ $response -ne 200 ]; then
  echo "Response code = $response"
  revoke_ssh
  exit 1
fi

revoke_ssh
