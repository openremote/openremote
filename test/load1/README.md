# Load Tests
[Blazemeter Taurus](https://gettaurus.org/) load testing utilising [JMeter](https://jmeter.apache.org/).

## Prerequisites
* Custom Taurus docker image - available in docker hub (https://github.com/openremote/jmeter-taurus)
* Bash shell
* See wiki for toolchain requirements (if running the manager locally)


## Setup
To run the tests you will need the Manager instance being tested to be deployed with the
`load1` setup; the setup does the following:
* 1 x Auto provisioning config in the master realm (WeatherAsset with a custom `calculated` attribute)
* 1 x Realm rule in the master realm that performs a simple calculation to calculate the `calculated` attribute value of
  the WeatherAssets based on changes to `rainfall` and `temperature` attributes
* N x Regular users in the master realm with normal read/write permissions username format `user+i` (OR_SETUP_REGULAR_USERS: default = 0)
* N x Service users in the master realm with normal read/write permissions username format `serviceuser+i` (OR_SETUP_SERVICE_USERS: default = 0)
* N x Pre-provisioned devices created from the auto provisioning config with asset name format `device+i` (OR_SETUP_DEVICES: default = 0) can optionally
skip every N'th device using (OR_SETUP_DEVICES_SKIP_COUNT: default = 10); this allows these N'th devices to go through the full auto provisioning process
when they connect during load testing thus simulating a more real world situation where a large percentage of devices would have provisioned slowly over 
time as they are first powered up 

## Deployment 1
### The `load1` Setup
The OpenRemote Manager instance under test must be running with `load1` setup and must be accessible
to the test runners. Three options for running the manager with the required setup:

* Run the manager using the `load1` setup in an IDE (Use `Load1 Setup` run configuration and `dev-proxy.yml`)
* Compile the `load1` setup jar directly into the manager image: `./gradlew -PSETUP_JAR=load1 clean installDist`
and package it into a custom docker image 
* Volume map the `load1` setup jar into the manager extensions folder: `./gradlew :setup:load1Jar` compiled
jar can then be found in `setup/build/libs`, this should be volume mapped into `/deployment/manager/extensions` of the 
manager container.

Some commands to deploy to a remote server `server1`, run from repo root dir:
```shell
./gradlew :setup:load1Jar
cd test
mkdir -p build/deployment1/manager/extensions build/deployment2 build/deployment3
cp ../setup/build/libs/openremote-load1-setup-0.0.0.jar build/deployment1/manager/extensions
cp ../profile/test-load1.yml build/deployment1
cp load1/console-users.jmx load1/console-users.yml build/deployment2
cp load1/auto-provisioning.jmx load1/auto-provisioning.yml build/deployment3
cd build
```

## Deployment 2
### Console users test (`console-users.yml`)
Simulates console devices registering with the Manager and subscribing to attribute events over websocket as follows:

* Get OAuth2 token from keycloak
* Make websocket connection
* Subscribe to attribute events
* Listen for attribute events in a continuous loop until `DURATION` seconds elapse

#### Test variables
* MANAGER_HOSTNAME - The hostname/IP address of the OpenRemote Manager under test (default: `localhost`)
* THREAD_COUNT - Number of threads/users/devices (default: `1000`)
* RAMP_RATE - Number of thread/users/devices to add / second (default: `50`)
* DURATION - How long each thread should run for in seconds (default: `300`)

#### Run the test
Add test variable overrides as required by copy/pasting the `-o settings.env.XXX` line:
```bash
mkdir -p test/load1/results; \
MSYS_NO_PATHCONV=1 docker run --rm -it \
-v $PWD/test/load1:/bzt-configs \
-v $PWD/test/load1/results:/tmp/artifacts openremote/jmeter-taurus \
-o settings.env.MANAGER_HOSTNAME=192.168.1.123 \
console-users.yml
```

## Deployment 3
### Auto provisioning device test (`auto-provisioning.yml`)
Simulates auto provisioning devices connecting to the Manager via MQTT as follows:

* Subscribing to the auto provisioning response topic
* Publish provisioning request and verify the success response
* Publishing two attribute values (temperature and rainfall) every TIME_BETWEEN_PUBLISHES milliseconds in a continuous loop until
`DURATION` seconds elapse

#### Device X.509 certificates (for auto provisioning)
The test requires device client X.509 certificates to be available at './devices.csv'; the file
contains 10,000 pre-generated device certificates but if more devices are required then use the `device_generator.sh COUNT`
script to generate a new file the COUNT value is the number of devices to generate certificates for (default: 100).
***NOTE: THiS WILL TAKE SOME TIME TO RUN***

#### Test variables
* MANAGER_HOSTNAME - The hostname/IP address of the OpenRemote Manager under test (default: `localhost`)
* THREAD_COUNT - Number of threads/users/devices (default: `1000`)
* RAMP_RATE - Number of thread/users/devices to add / second (default: `50`)
* DURATION - How long each thread should run for in seconds (default: `300`)
* MILLIS_BETWEEN_PUBLISHES - How long to wait between each time the 2 attributes are published in milliseconds (default: `10000`)

#### Run the test
Add test variable overrides as required by copy/pasting the `-o settings.env.XXX` line:
```bash
mkdir -p test/load1/results; \
MSYS_NO_PATHCONV=1 docker run --rm -it \
-v $PWD/test/load1:/bzt-configs \
-v $PWD/test/load1/results:/tmp/artifacts openremote/jmeter-taurus \
-o settings.env.MANAGER_HOSTNAME=192.168.1.123 \
auto-provisioning.yml
```

**NOTE: A t4g.medium instance can only handle ~4000 devices; a t4g.large instance ~9000 devices; so anything more
requires a larger instance or splitting into multiple deployments.**

### Auto provisioning device settle test (`auto-provisioning-settle-test.yml`)
Simulates auto provisioning devices connecting to the Manager via MQTT similar to the above auto provisioning test but
each device will keep retrying auto provisioning until a successful response is received with the intention to find the
time required for the system to settle down after a full restart. The test will only stop when all devices have
provisioned successfully and start publishing attribute events.

#### Test variables
* MANAGER_HOSTNAME - The hostname/IP address of the OpenRemote Manager under test (default: `localhost`)
* THREAD_COUNT - Number of threads/users/devices (default: `1000`)
* RAMP_RATE - Number of thread/users/devices to add / second (default: `50`)
* MILLIS_BETWEEN_PUBLISHES - How long to wait between each time the 2 attributes are published in milliseconds (default: `10000`)
* RESTART_DELAY_MILLIS - Max amount of time to wait when an error occurs before the device will re-connect (a random value between 0 and this value will be used)

#### Run the test
Add test variable overrides as required by copy/pasting the `-o settings.env.XXX` line:
```bash
mkdir -p test/load1/results; \
MSYS_NO_PATHCONV=1 docker run --rm -it \
-v $PWD/test/load1:/bzt-configs \
-v $PWD/test/load1/results:/tmp/artifacts openremote/jmeter-taurus \
-o settings.env.MANAGER_HOSTNAME=192.168.1.123 \
auto-provisioning-settle-test.yml
```

## Full test script `run.sh`
The `run.sh` script can be used to orchestrate deploying the 3 different deployments via `ssh` see the file header for
usage e.g.:
```bash
./run.sh test1.example.com test2.example.com test3.example.com
```
With options to not redeploy `deployment1`, skip `deployment2` and use the settle test for `deployment3`:
```bash
DEPLOYMENT1_DO_NOTHING=true DEPLOYMENT2_THREAD_COUNT=0 DEPLOYMENT3_THREAD_COUNT=10000 DEPLOYMENT3_RAMP_RATE=500 DEPLOYMENT3_USE_SETTLE_TEST=true ./run.sh test1.example.com unused test3.example.com
```
