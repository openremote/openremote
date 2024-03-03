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



### Deploying the `load1` Setup
The OpenRemote Manager instance under test must be running with `load1` setup and must be accessible
to the test runners. Three options for running the manager with the required setup:

* Run the manager using the `load1` setup in an IDE (Use `Load1 Setup` run configuration and `dev-proxy.yml`)
* Compile the `load1` setup jar directly into the manager image: `./gradlew -PSETUP_JAR=load1 clean installDist`
and package it into a custom docker image 
* Volume map the `load1` setup jar into the manager extensions folder: `./gradlew :setup:load1Jar` compiled
jar can then be found in `setup/build/libs`, this should be volume mapped into `/deployment/manager/extensions` of the 
manager container.

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
* TIME_BETWEEN_PUBLISHES - How long to wait between each time the 2 attributes are published in milliseconds (default: `10000`)

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



## Console users test (`console-users.yml`)
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
