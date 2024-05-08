## OpenRemote MQTT Client (Arduino/ESP)
The OpenRemote MQTT Client is a wrapper library for the [PubSubClient](https://github.com/knolleary/pubsubclient) and contains a simple API for communicating with the OpenRemote MQTT Gateway API. 

The OpenRemotePubSub library is primarily supported for the [Arduino Framework](https://docs.platformio.org/en/stable/frameworks/arduino.html). It is worth noting that the library is open-source and can easily be modified to be compatible with other platforms and or frameworks.

***
##### Basic Setup (ESP32 - Arduino Framework)

 Both the [OpenRemotePubSub]() library and the [PubSubClient]() need to be added as an dependency to your project.

1. Define the required objects - WifiClient, PubSubClient, OpenRemotePubSub.

```
WiFiClient wifiClient;
PubSubClient client(wifiClient);
OpenRemotePubSub openRemote(clientId, client);
```

2. Connect the WifiClient
```
WiFi.begin(ssid, password);
```
3. Set the MQTT Client Server
```
openRemote.client.setServer(mqtt_server, mqtt_port);
```

4. Connect to the MQTT Server
```
if (openRemote.client.connect(clientId, username, mqttpass))
{
  // Serial.println("Connected to MQTT broker");
}
```

5. Add the OpenRemote Client Loop
```
openRemote.client.loop();
```

This should provide the base for setting up the openRemote client. See the [OpenRemote MQTT Client (Arduino/ESP) Docs]() page for all the API specification.