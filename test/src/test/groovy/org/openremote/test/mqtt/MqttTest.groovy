package org.openremote.test.mqtt

import com.google.common.collect.Lists
import io.moquette.BrokerConstants
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.mqtt.MqttBrokerService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.value.Value
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.RawClient
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.*
import static org.openremote.manager.mqtt.KeycloakAuthenticator.MQTT_CLIENT_ID_SEPARATOR
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_HOST
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_PORT

class MqttTest extends Specification implements ManagerContainerTrait {
    def "Mqtt broker test"() {

        given: "the container environment is started"
        MqttBrokerService spyMqttBrokerService = Spy(MqttBrokerService)

        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def services = Lists.newArrayList(defaultServices())
        services.replaceAll{it instanceof MqttBrokerService ? spyMqttBrokerService : it}
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def mqttBrokerService = container.getService(MqttBrokerService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def mqttClientId = managerDemoSetup.realmBuildingTenant + MQTT_CLIENT_ID_SEPARATOR + UniqueIdentifierGenerator.generateId()
        def clientId = MqttBrokerService.MQTT_CLIENT_ID_PREFIX + UniqueIdentifierGenerator.generateId(managerDemoSetup.realmBuildingTenant)
        def clientSecret = UniqueIdentifierGenerator.generateId(managerDemoSetup.realmBuildingTenant)

        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST);
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT);

        expect: "the container should be running and initialised"
        conditions.eventually {
            assert container.isRunning()
        }

        when: "a mqtt client connects with wrong credentials"
        def payloadLength = "client1".size() + clientId.size() + clientSecret.size()
        def remainingLength = 16 + payloadLength
        def client = RawClient.connect(mqttHost, mqttPort).isConnected()
        // CONNECT
                .write(0x10) // MQTT Control Packet type(1)
                .write(remainingLength.byteValue()) // Remaining Length
                .write(0x00, 0x04) // Protocol Name Length
                .write("MQTT") // Protocol Name
                .write(0x04) // The value of the Protocol Level field for the version 3.1.1 of the protocol is 4 (0x04)

        // Connect Flags
        // User Name Flag(1)
        // Password Flag(1)
        // Will Retain(0)
        // Will QoS(00)
        // Will Flag(0)
        // Clean Session(1)
        // Reserved(0)
                .write(0xC2)
                .write(0x00, 0x00) // Keep Alive

        // Payload
                .write(0x00, "client1".size().byteValue()) // Client Identifier Length
                .write("client1") // Client Identifier
                .write(0x00, clientId.size().byteValue())
                .write(clientId)
                .write(0x00, clientSecret.size().byteValue())
                .write(clientSecret)
                .flush()

        then: "no connection should be made"
        conditions.eventually {
            assert mqttBrokerService.mqttConnector.connectionMap.size() == 0
        }

        when: "a mqtt client connects"
        payloadLength = mqttClientId.size() + clientId.size() + clientSecret.size()
        remainingLength = 16 + payloadLength
        client = RawClient.connect(mqttHost, mqttPort).isConnected()
        // CONNECT
                .write(0x10) // MQTT Control Packet type(1)
                .write(remainingLength.byteValue()) // Remaining Length
                .write(0x00, 0x04) // Protocol Name Length
                .write("MQTT") // Protocol Name
                .write(0x04) // The value of the Protocol Level field for the version 3.1.1 of the protocol is 4 (0x04)

        // Connect Flags
        // User Name Flag(1)
        // Password Flag(1)
        // Will Retain(0)
        // Will QoS(00)
        // Will Flag(0)
        // Clean Session(1)
        // Reserved(0)
                .write(0xC2)
                .write(0x00, 0x00) // Keep Alive

        // Payload
                .write(0x00, mqttClientId.size().byteValue()) // Client Identifier Length
                .write(mqttClientId) // Client Identifier
                .write(0x00, clientId.size().byteValue())
                .write(clientId)
                .write(0x00, clientSecret.size().byteValue())
                .write(clientSecret)
                .flush()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert mqttBrokerService.mqttConnector.getConnection(mqttClientId) != null
        }

        when: "a mqtt client subscribes to an asset in another realm"
        def topic = "assets/" + managerDemoSetup.thingId + "/" + managerDemoSetup.thingLightToggleAttributeName
        remainingLength = 4 + topic.size() + 1 //plus one for the QoS byte

        client
        // SUBSCRIBE
                .write(0x82) // MQTT Control Packet type(8) with QoS level 1
                .write(remainingLength.byteValue()) // Remaining Length
                .write(0x00, 0x10) // MessageId

        // Payload
                .write(0x00, topic.size().byteValue()) // Topic Length
                .write(topic) // Topic
                .write(0x01) // QoS level 1
                .flush()

        then: "No subscription should exist"
        conditions.eventually {
            assert mqttBrokerService.mqttConnector.getConnection(mqttClientId).attributeSubscriptions.size() == 0
        }

        when: "a mqtt client subscribes to an non existing asset"
        topic = "assets/dhajkdasjfgh/" + managerDemoSetup.thingLightToggleAttributeName
        remainingLength = 4 + topic.size() + 1 //plus one for the QoS byte

        client
        // SUBSCRIBE
                .write(0x82) // MQTT Control Packet type(8) with QoS level 1
                .write(remainingLength.byteValue()) // Remaining Length
                .write(0x00, 0x10) // MessageId

        // Payload
                .write(0x00, topic.size().byteValue()) // Topic Length
                .write(topic) // Topic
                .write(0x01) // QoS level 1
                .flush()

        then: "No subscription should exist"
        conditions.eventually {
            assert mqttBrokerService.mqttConnector.getConnection(mqttClientId).attributeSubscriptions.size() == 0
        }

        when: "a mqtt client subscribes to an asset"
        topic = "assets/" + managerDemoSetup.apartment1HallwayId + "/motionSensor"
        remainingLength = 4 + topic.size() + 1 //plus one for the QoS byte

        client
        // SUBSCRIBE
                .write(0x82) // MQTT Control Packet type(8) with QoS level 1
                .write(remainingLength.byteValue()) // Remaining Length
                .write(0x00, 0x10) // MessageId

        // Payload
                .write(0x00, topic.size().byteValue()) // Topic Length
                .write(topic) // Topic
                .write(0x01) // QoS level 1
                .flush()

        then: "A subscription should exist"
        conditions.eventually {
            assert mqttBrokerService.mqttConnector.getConnection(mqttClientId).attributeSubscriptions.size() > 0
        }

        when: "An asset changed the client is subscribed on"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerDemoSetup.apartment1HallwayId, "motionSensor", Values.create(50)))

        then: "A publish message should be sent"
        conditions.eventually {
            1 * mqttBrokerService.sendAssetAttributeUpdateMessage(_ as String, _ as AttributeRef, _ as Optional<Value>)
        }

        when: "a mqtt client unsubscribes to an asset"
        topic = "assets/" + managerDemoSetup.apartment1HallwayId + "/motionSensor"
        remainingLength = 4 + topic.size()

        client
        // UNSUBSCRIBE
                .write(0xA2) // MQTT Control Packet type(10) with QoS level 1
                .write(remainingLength.byteValue()) // Remaining Length
                .write(0x00, 0x10) // MessageId

        // Payload
                .write(0x00, topic.size().byteValue()) // Topic Length
                .write(topic) // Topic
                .flush()

        then: "NO subscription should exist"
        conditions.eventually {
            assert mqttBrokerService.mqttConnector.getConnection(mqttClientId).attributeSubscriptions.size() == 0
        }
    }
}
