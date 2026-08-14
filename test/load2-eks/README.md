# Load Tests

Scripts and configuration files to setup an OpenRemote stack running in an EKS cluster appropriate for load testing.
This includes configuring the cluster to use a more powerful machine
and adapting the values files to configure the memory usage on the different pods.  
For the manager, JVM parameters are used to make use of the extra memory available to the container.  
There are different "profiles" available depending on the power required and the tests to be run:
- large: minimal set-up useful to test memory leaks and pressure condition
- xlarge-minimal: doubles memory allocation for manager compared to large profile (4Gi vs 2Gi), with room for further increases
- xlarge: bigger setup using all capacity on 4 CPU/16 GB cluster
- 2xlarge: bigger setup using all capacity on 8 CPU/32 GB cluster
- 4xlarge: bigger setup using all capacity on 16 CPU/64 GB cluster
- 8xlarge: bigger setup using all capacity on 32 CPU/128 GB cluster

The profile is selected by setting the OR_PROFILE environment variable in the `eks-common.sh` script.   

This folder contains a different setup than load1 and includes different test scenarios.  

The setup is controlled by the following parameters:
- OR_SETUP_USERS: the number of accounts to create
- OR_SETUP_ASSETS: the number of light assets to create in each account
- OR_SETUP_ASSETS_WITH_LOCATIONS: number of assets (per account) that will be assigned a location
- OR_SETUP_LOCATION_CLUSTERS: number of clusters to divide location-enabled assets into
- OR_SETUP_LOCATION_MAIN_CENTER: main center point in "lon,lat" format (e.g. "4.470175,51.923464")
- OR_SETUP_LOCATION_CENTER_DISTANCE: distance in meters between cluster centers
- OR_SETUP_LOCATION_ASSET_RADIUS: max radius in meters for random asset locations around each cluster center

The setup creates:
- OR_SETUP_USERS standard user account
- In each account, 1 Building asset
- As child of each Building asset, OR_SETUP_ASSETS Light assets
- OR_SETUP_ASSETS_WITH_LOCATIONS of those Light assets will have a location set.  
All locations are distributed in OR_SETUP_LOCATION_CLUSTERS clusters.  
The clusters' centers are arranged in a hex grid pattern around OR_SETUP_LOCATION_MAIN_CENTER.  
Within each cluster, the locations are random within a OR_SETUP_LOCATION_CENTER_DISTANCE radius from the cluster center.
- OR_SETUP_USERS restricted service users, linked to the corresponding Building asset,
with appropriate permission to push attribute values

You build and push the custom manager image in a similar way than for load1, by running
```
./gradlew -PSETUP_JAR=load2 clean installDist

export AWS_DEVELOPERS_ACCOUNT_ID="dev-account-id"
aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin $AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com
docker buildx build --push --platform linux/amd64,linux/arm64 -t $AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/openremote/manager:load2 manager/build/install/manager/
```

You might want to update the stack without re-creating the whole cluster, as re-creating the cluster takes time and
requires re-generating a new certificate (on which we have some limits).  
To do so, keep the proxy running and uninstall the other charts
```shell
helm uninstall manager postgresql keycloak
```
then use the `eks-deploy-load.sh` script to properly re-deploy those charts.

Once deployed, running the load tests can be done using the same clients as for load testing a VM,
but using the scenarios present under `scenarios` in this folder instead.  
See `load1` folder for the tools, scripts and documentation.

Two scenarios are provided:
#### connect-and-publish

Runs for a given duration, publishing attribute value changes over MQTT once connected.  
For each connection, it publishes ASSETS_COUNT values at the same time (50ms interval), then pauses for MILLIS_BETWEEN_PUBLISHES.  
If a publish fails because the connection is lost and `RECONNECT_ATTEMPTS` is greater than 0, the thread returns to the connect step and retries only while its original per-thread runtime has not yet elapsed. If `RECONNECT_ATTEMPTS` is 0, the thread does not retry and its load is lost.  
At the end of publish loop, it waits for DISCONNECT_DELAY before disconnecting from MQTT.

The parameters are:  
MANAGER_HOSTNAME: Hostname of the manager to be tested. Default is localhost.  
THREAD_COUNT: Number of parallel accounts that will connect and publish in parallel. Default is 500.  
ASSETS_COUNT: Number of Light assets for which to publish an attribute during each iteration. Default is 5.  
RAMP_RATE: Number of thread to add per second during ramp-up. Default is 20.  
DURATION: Total duration to run the test for, in seconds. Default is 300.  
MILLIS_BETWEEN_PUBLISHES: Delay between each publishing iteration. Default is 1000.  
RECONNECT_ATTEMPTS: Reconnect control. Default is 0. With the current HiveMQ-based xmeter client, 0 disables automatic reconnect and any positive value enables it using the client's default reconnect configuration. The scenario also uses this setting to control JMeter-level retry behavior: 0 means no reconnect retry loop, any positive value means failed publish loops return to the connect step and retry only until the original per-thread runtime expires, even if connect attempts keep timing out.  
DISCONNECT_DELAY: Delay before closing the MQTT connection after publishing loop, in seconds. Default is 0.

#### connect-settle-test

Connects up to THREAD_COUNT accounts to the system.  
Once connected, it does 1 single publish every MILLIS_BETWEEN_PUBLISHES (defaults to 300s).  
Goal is to measure the time it takes for the system to allow that many connections and stabilise.

The parameters are:  
MANAGER_HOSTNAME: Hostname of the manager to be tested. Default is localhost.  
THREAD_COUNT: Number of parallel accounts that will connect and publish in parallel. Default is 1000.  
RAMP_RATE: Number of thread to add per second during ramp-up. Default is 50.  
MILLIS_BETWEEN_PUBLISHES: Delay between each publishing iteration. Default is 30000.  

## Multi-proxy test

As part of [Test using multiple HAProxy replicas under EKS · Issue #2568 · openremote/openremote](https://github.com/openremote/openremote/issues/2568),
a setup terminating TLS at the AWS NLB level and using ACM to manage certificates, while still using HAProxy pod(s) has been implemented.  
This can be installed using the `eks-setup-load-acm.sh` script.  
This is really for testing purposes and clarification on how we deploy our stack is required to move further.
