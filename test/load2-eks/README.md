# Load Tests

Scripts and configuration files to setup an OpenRemote stack running in an EKS cluster appropriate for load testing.  
This includes configuring the cluster to use a more powerful machine
and adapting the values files to configure the memory usage on the different pods.  
For the manager, JVM parameters are used to make use of the extra memory available to the container.  

This folder contains a different setup than load1 and includes different test scenarios.  

The setup is controlled by 2 parameters:
- OR_SETUP_USERS: the number of accounts to create
- OR_SETUP_ASSETS: the number of light assets to create in each account

The setup creates:
- OR_SETUP_USERS standard user account
- In each account, 1 Building asset
- As child of each Building asset, OR_SETUP_ASSETS Light assets
- OR_SETUP_USERS restrictred service users, linked to the corresponding Building asset,
with appropriate permission to push attribute values

You build and push the custom manager image in a similar way than for load1, by running
```
./gradlew -PSETUP_JAR=load2 clean installDist

export AWS_DEVELOPERS_ACCOUNT_ID="dev-account-id"
aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin $AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com
docker buildx build --push --platform linux/amd64,linux/arm64 -t $AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/openremote/manager:load2 manager/build/install/manager/
```

Once deployed, running the load tests can be done using the same clients as for load testing a VM,
but using the scenarios present in under `scenarios` in this folder instead.  
See `load1` folder for the tools, scripts and documentation.

Two scenarios are provided:
#### connect-and-publish

Runs for a given duration, publishing attribute value changes over MQTT once connected.  
For each connection, it publishes ASSETS_COUNT values at the same time (50ms interval), then pauses for MILLIS_BETWEEN_PUBLISHES.

The parameters are:  
MANAGER_HOSTNAME: Hostname of the manager to be tested  
THREAD_COUNT: Number of parallel accounts that will connect and publish in parallel  
ASSETS_COUNT: Number of Light assets for which to publish an attribute during each iteration  
RAMP_RATE: Number of thead to add per second during ramp-up  
DURATION: Total duration to run the test for  
MILLIS_BETWEEN_PUBLISHES: Delay between each publishing iteration  

#### connect-settle-test

Connects up to THREAD_COUNT accounts to the system.  
Once connected, it does 1 single publish every MILLIS_BETWEEN_PUBLISHES (defaults to 300s).  
Goal is to measure the time it takes for the system to allow that many connections and stabilise.

The parameters are:  
MANAGER_HOSTNAME: Hostname of the manager to be tested  
THREAD_COUNT: Number of parallel accounts that will connect publish in parallel  
RAMP_RATE: Number of thead to add per second during ramp-up  
MILLIS_BETWEEN_PUBLISHES: Delay between each publishing iteration  
