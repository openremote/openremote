package org.openremote.test.mqtt

import com.google.common.collect.Lists
import io.moquette.BrokerConstants
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.mqtt.MqttBrokerService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeEvent
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
        def mqttBrokerServiceAttributeEventCalls = 0
        def mqttBrokerServiceAttributeValueCalls = 0
        def spyMqttBrokerService = Spy(MqttBrokerService) {
            sendAttributeEvent(_ as String, _ as AttributeEvent) >> {
                clientId, attributeEvent ->
                    mqttBrokerServiceAttributeEventCalls++
                    callRealMethod()
            }
            sendAttributeValue(_ as String, _ as AttributeEvent) >> {
                clientId, attributeEvent ->
                    mqttBrokerServiceAttributeValueCalls++
                    callRealMethod()
            }
        }

        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def services = Lists.newArrayList(defaultServices())
        services.replaceAll { it instanceof MqttBrokerService ? spyMqttBrokerService : it }
        def container = startContainer(defaultConfig(serverPort), services)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def mqttBrokerService = container.getService(MqttBrokerService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
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
            assert mqttBrokerService.mqttConnectionMap.size() == 0
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
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId) != null
        }

        when: "a mqtt client subscribes to an asset in another realm"
        def topic = "assets/" + managerDemoSetup.thingId
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
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).assetSubscriptions.size() == 0
        }

        when: "a mqtt client subscribes to an non existing asset"
        topic = "assets/dhajkdasjfgh"
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
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).assetSubscriptions.size() == 0
        }

        when: "a mqtt client subscribes to an asset"
        topic = "assets/" + managerDemoSetup.apartment1HallwayId
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
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).assetSubscriptions.size() > 0
        }

        when: "An asset attribute changed the client is subscribed on"
        def attributeEvent = new AttributeEvent(managerDemoSetup.apartment1HallwayId, "motionSensor", Values.create(50))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 1
        }

        when: "Another asset attribute changed the client is subscribed on"
        attributeEvent = new AttributeEvent(managerDemoSetup.apartment1HallwayId, "presenceDetected", Values.create(true))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "A second publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 2
        }

        when: "a mqtt client publishes to an asset attribute which is readonly"
        topic = "assets/" + managerDemoSetup.apartment1HallwayId
        def payload = Values.createObject().put("motionSensor", 70).toJson()
        remainingLength = 2 + topic.size() + payload.length()

        //PUBLISH
        client.write(0x30) // MQTT Control Packet type(10) with QoS level 0
                .write(remainingLength.byteValue()) // Remaining Length
                .write(0x00, topic.size().byteValue()) // Topic Length
                .write(topic) // Topic
                .write(payload) // content
                .flush()

        then: "The value of the attribute shouldn't be updated"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1HallwayId)
            assert asset.getAttribute("motionSensor").get().valueAsNumber.orElse(0) == 50d
        }

        when: "a mqtt client publishes to an asset attribute"
        topic = "assets/" + managerDemoSetup.apartment1HallwayId
        payload = Values.createObject().put("lights", false).toJson()
        remainingLength = 2 + topic.size() + payload.length()

        //PUBLISH
        client.write(0x30) // MQTT Control Packet type(10) with QoS level 0
                .write(remainingLength.byteValue()) // Remaining Length
                .write(0x00, topic.size().byteValue()) // Topic Length
                .write(topic) // Topic
                .write(payload) // content
                .flush()

        then: "The value of the attribute should be updated and another publish event should be sent"
        conditions.eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment1HallwayId)
            assert !asset.getAttribute("lights").get().valueAsBoolean.orElse(true)
            assert mqttBrokerServiceAttributeEventCalls == 3
        }

        when: "a mqtt client unsubscribes to an asset"
        topic = "assets/" + managerDemoSetup.apartment1HallwayId
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


        then: "No subscription should exist"
        conditions.eventually {
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).assetSubscriptions.size() == 0
        }

        when: "Another asset attribute changed without any subscriptions"
        attributeEvent = new AttributeEvent(managerDemoSetup.apartment1HallwayId, "presenceDetected", Values.create(false))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "No publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 3
        }

        when: "a mqtt client subscribes to an asset attribute"
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

        and: "that attribute changed"
        attributeEvent = new AttributeEvent(managerDemoSetup.apartment1HallwayId, "motionSensor", Values.create(30))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 4
        }

        when: "Another asset attribute changed without any subscriptions on that attribute"
        attributeEvent = new AttributeEvent(managerDemoSetup.apartment1HallwayId, "presenceDetected", Values.create(true))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "No publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 4
        }

        when: "a mqtt client unsubscribes to an asset attribute"
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


        then: "No subscription should exist"
        conditions.eventually {
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).assetAttributeSubscriptions.size() == 0
        }

        when: "a mqtt client subscribes to an asset attribute value"
        topic = "assets/" + managerDemoSetup.apartment1HallwayId + "/motionSensor/value"
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

        and: "that attribute changed"
        attributeEvent = new AttributeEvent(managerDemoSetup.apartment1HallwayId, "motionSensor", Values.create(40))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "A publish value message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeValueCalls == 1
        }

        when: "Another asset attribute changed without any subscriptions on that attribute"
        attributeEvent = new AttributeEvent(managerDemoSetup.apartment1HallwayId, "presenceDetected", Values.create(false))
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "No publish value message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeValueCalls == 1
        }

        when: "a mqtt client unsubscribes to an asset attribute value"
        topic = "assets/" + managerDemoSetup.apartment1HallwayId + "/motionSensor/value"
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


        then: "No subscription should exist"
        conditions.eventually {
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).assetAttributeValueSubscriptions.size() == 0
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
