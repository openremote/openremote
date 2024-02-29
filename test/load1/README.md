# Load Tests
[Blazemeter Taurus](https://gettaurus.org/) load testing utilising [JMeter](https://jmeter.apache.org/).

## Prerequisites
* Custom Taurus docker image - available in docker hub (https://github.com/openremote/jmeter-taurus)
* Bash shell
* See wiki for toolchain requirements (if running the manager locally)

## Setup
To run the tests you will need the Manager instance being tested to be deployed with the
`load` setup; the steps to do this are explained below. The test suite consists of:

### Target - OpenRemote Manager to test (must be running `load1` setup)
The OpenRemote Manager instance under test must be running with `load1` setup and must be accessible
to the test runners. Three options for running the manager with the required setup:
* run the manager using the `load1` setup in an IDE (Use `Load1 Setup` run configuration and `dev-proxy.yml`)
* Compile the `load1` setup jar directly into the manager image: `./gradlew -PSETUP_JAR=load1 clean installDist`
and package it into a docker image 
* Volume map the `load1` setup jar into the manager extensions folder: `./gradlew :setup:load1Jar` compiled
jar can then be found in `setup/build/libs`, this should be volume mapped into `/deployment/manager/extensions` of the 
manager container.

### Auto provisioning device test (`auto-provisioning.yml`)
This Simulates auto provisioning devices as follows:

* 1 x Realm (master)
* 1 x Auto provisioning config (WeatherAsset with a custom `calculated` attribute)
* 1 x Realm rule that performs a simple calculation to calculate the `calculated` attribute value of WeatherAssets based
  on changes to `rainfall` and `temperature` attributes
* N x auto provisioning MQTT devices connecting to the MQTT broker and:
    * Subscribing to the auto provisioning response topic (1-N devices in M minutes)
    * Publishing their provisioning config requests (1-N devices in M minutes with initial delay of 10s)
    * Publishing two attribute values (temperature and rainfall) every R seconds
    * Repeat the publish X times

#### Device X.509 certificates (for auto provisioning)
The `auto-provisioning` test requires device client X.509 certificates to be available at './devices.csv'; the file
contains 10,000 pre-generated device certificates but if more devices are required then use the `device_generator.sh COUNT`
script to generate a new file the COUNT value is the number of devices to generate certificates for (default: 100).

## Console users test (`console-users.jmx`)
Simulates console users as follows:
* 1 x Realm (master)
* Get OAuth2 token from keycloak
* Make websocket connection
* Subscribe to attribute events for a given asset
