# Load Test
A load test that uses [Taurus](https://gettaurus.org/) to simulate:

* 1 x Realm (master)
* 1 x Auto provisioning config (WeatherAsset with a custom `calculated` attribute)
* 1 x Realm rule that performs a simple calculation to calculate the `calculated` attribute value of WeatherAssets based
on changes to `rainfall` and `temperature` attributes  
* N x auto provisioning MQTT devices connecting to the MQTT broker and:
  * Subscribing to the auto provisioning response topic (1-N devices in M minutes) 
  * Publishing their provisioning config requests (1-N devices in M minutes with initial delay of 10s)
  * Publishing two attribute values (temperature and rainfall)

# Prerequisites

* Bash shell
* Java 17+
* Taurus

# Usage

1. Run the `device_generator.sh` script to generate auto provisioning device X.509 certificates which are output to `tmp/devices.csv` 
2. Run the manager using the `load1` setup 
3. Run Taurus:
```
bzt mqtt_auto_provision.yml
```
