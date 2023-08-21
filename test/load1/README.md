# Load Test
A load test that uses [Taurus](https://gettaurus.org/) to simulate:

* 1 x Realm (master)
* 1 x Auto provisioning config (WeatherAsset with a custom `calculated` attribute)
* 1 x Realm rule that performs a simple calculation to calculate the `calculated` attribute value of WeatherAssets based
on changes to `rainfall` and `temperature` attributes  
* N x auto provisioning MQTT devices connecting to the MQTT broker and:
  * Subscribing to the auto provisioning response topic (1-N devices in M minutes) 
  * Publishing their provisioning config requests (1-N devices in M minutes with initial delay of 10s)
  * Subscribing to the attributes of the asset
  * Publishing two attribute values (temperature and rainfall) every R seconds
  * Checking for 3 attribute events to be received (temperature, rainfall and calculated)
  * Repeat the publish and checking indefinitely

# Prerequisites

* Bash shell
* Java 17+
* Taurus

# Usage

1. Run the `device_generator.sh COUNT` script to generate auto provisioning device X.509 certificates which are output to `tmp/devices.csv`
the COUNT value is the number of devices to generate certificates for (default: 100)
3. Either run the manager using the `load1` setup or compile the `load1` setup jar (`./gradlew clean :setup:load1Jar`) manager with the `load1` setup included  
4. Run Taurus:
```
bzt mqtt_auto_provision.yml
```
