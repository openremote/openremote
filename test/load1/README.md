# Load Tests
Various [JMeter](https://jmeter.apache.org/) load tests.

# Prerequisites

* Bash shell
* Java 17+
* JMeter with the following plugins:
  * Custom MQTT plugin from: https://github.com/richturner/mqtt-jmeter/releases
  * WebSocket Samplers by Peter Doornbosch
  * 

## Setup
This test requires the device client X.509 certificates to be available at './devices.csv', if more devices are required
then run the `device_generator.sh COUNT` script to generate a new file the COUNT value is the number of devices to
generate certificates for (default: 100).

## Deployment (`load1` Setup)
3 options for running the manager with the required setup:
* run the manager using the `load1` setup in an IDE
* Compile the `load1` setup jar into the manager image: `./gradlew -PSETUP_JAR=load1 clean installDist`
* Volume map the `load1` setup jar into the manager extensions folder


## Auto provisioning device test (`auto-provisioning.jmx`)
Simulates auto provisioning devices as follows:

* 1 x Realm (master)
* 1 x Auto provisioning config (WeatherAsset with a custom `calculated` attribute)
* 1 x Realm rule that performs a simple calculation to calculate the `calculated` attribute value of WeatherAssets based
  on changes to `rainfall` and `temperature` attributes
* N x auto provisioning MQTT devices connecting to the MQTT broker and:
    * Subscribing to the auto provisioning response topic (1-N devices in M minutes)
    * Publishing their provisioning config requests (1-N devices in M minutes with initial delay of 10s)
    * Publishing two attribute values (temperature and rainfall) every R seconds
    * Repeat the publish X times


## Console users test (`console-users.jmx`)
Simulates console users as follows:
* 1 x Realm (master)
* Get OAuth2 token from keycloak
* Make websocket connection
* Subscribe to attribute events for a given asset

