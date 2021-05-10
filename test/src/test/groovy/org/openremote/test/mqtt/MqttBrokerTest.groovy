package org.openremote.test.mqtt

import com.google.common.collect.Lists
import io.moquette.BrokerConstants
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.mqtt.MqttBrokerService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.test.setup.ManagerTestSetup
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
import static org.openremote.manager.mqtt.MqttBrokerService.MULTI_LEVEL_WILDCARD
import static org.openremote.manager.mqtt.MqttBrokerService.TOPIC_SEPARATOR
import static org.openremote.model.value.ValueType.TEXT

class MqttBrokerTest extends Specification implements ManagerContainerTrait {
    def "Mqtt broker attribute event test"() {

        given: "the container environment is started"
        def mqttBrokerServiceAttributeEventCalls = 0
        def mqttBrokerServiceAttributeValueCalls = 0
        def spyMqttBrokerService = Spy(MqttBrokerService) {
            sendAttributeEvent(_ as String, _ as String, _ as AttributeEvent) >> {
                clientId, topic, attributeEvent ->
                    mqttBrokerServiceAttributeEventCalls++
                    callRealMethod()
            }
            sendAttributeValue(_ as String, _ as String, _ as AttributeEvent) >> {
                clientId, topic, attributeEvent ->
                    mqttBrokerServiceAttributeValueCalls++
                    callRealMethod()
            }
        }

        def conditions = new PollingConditions(timeout: 20, delay: 0.2)
        def services = Lists.newArrayList(defaultServices())
        services.replaceAll { it instanceof MqttBrokerService ? spyMqttBrokerService : it }
        def container = startContainer(defaultConfig(), services)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def mqttBrokerService = container.getService(MqttBrokerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def mqttClientId = managerTestSetup.realmBuildingTenant + MQTT_CLIENT_ID_SEPARATOR + UniqueIdentifierGenerator.generateId()
        def clientId = MqttBrokerService.MQTT_CLIENT_ID_PREFIX + UniqueIdentifierGenerator.generateId(managerTestSetup.realmBuildingTenant)
        def clientSecret = UniqueIdentifierGenerator.generateId(managerTestSetup.realmBuildingTenant)

        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST);
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT);

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
                .write(0x00, managerTestSetup.realmBuildingTenant.size().byteValue()) // Client Identifier Length
                .write(managerTestSetup.realmBuildingTenant) // Client Identifier
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
        def topic = "attribute/" + managerTestSetup.thingId
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
        topic = "attribute/dhajkdasjfgh"
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
        topic = "attribute/" + managerTestSetup.apartment1HallwayId
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
        def attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 50)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 1
        }

        when: "Another asset attribute changed the client is subscribed on"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", true)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "A second publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 2
        }

        when: "a mqtt client publishes to an asset attribute which is readonly"
        topic = "attribute/" + managerTestSetup.apartment1HallwayId
        def payload = Values.asJSON(Values.createJsonObject().put("motionSensor", 70)).orElse(null)
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
            def asset = assetStorageService.find(managerTestSetup.apartment1HallwayId)
            assert asset.getAttribute("motionSensor").get().value.orElse(0) == 50d
        }

        when: "a mqtt client publishes to an asset attribute"
        topic = "attribute/" + managerTestSetup.apartment1HallwayId
        payload = Values.asJSON(Values.createJsonObject().put("lights", false)).orElse(null)
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
            def asset = assetStorageService.find(managerTestSetup.apartment1HallwayId)
            assert !asset.getAttribute("lights").get().value.orElse(true)
            assert mqttBrokerServiceAttributeEventCalls == 3
        }

        when: "a mqtt client unsubscribes to an asset"
        topic = "attribute/" + managerTestSetup.apartment1HallwayId
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
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", false)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "No publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 3
        }

        when: "a mqtt client subscribes to an asset attribute"
        topic = "attribute/" + managerTestSetup.apartment1HallwayId + "/motionSensor"
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

        then: "the subscription should be created"
        conditions.eventually {
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).assetSubscriptions.containsKey(topic)
        }

        when: "that attribute changed"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 30)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 4
        }

        when: "Another asset attribute changed without any subscriptions on that attribute"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", true)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "No publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 4
        }

        when: "a mqtt client unsubscribes to an asset attribute"
        topic = "attribute/" + managerTestSetup.apartment1HallwayId + "/motionSensor"
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

        when: "a mqtt client subscribes with multilevel on asset"
        topic = "attribute/" + managerTestSetup.apartment1Id + TOPIC_SEPARATOR + MULTI_LEVEL_WILDCARD
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

        when: "An child asset attribute changed the client is subscribed on"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 40)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeEventCalls == 5
        }

        when: "a mqtt client subscribes to an asset attribute value"
        topic = "attribute/" + managerTestSetup.apartment1HallwayId + "/motionSensor/value"
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

        then: "the subscription should be created"
        conditions.eventually {
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).attributeValueSubscriptions.containsKey(topic)
        }

        when: "that attribute changed"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 40)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "A publish value message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeValueCalls == 1
        }

        when: "Another asset attribute changed without any subscriptions on that attribute"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", false)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "No publish value message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAttributeValueCalls == 1
        }

        when: "a mqtt client unsubscribes to an asset attribute value"
        topic = "attribute/" + managerTestSetup.apartment1HallwayId + "/motionSensor/value"
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
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).attributeValueSubscriptions.size() == 0
        }

        when: " a client disconnects"
        client
        // DISCONNECT
                .write(0xE0) // MQTT Control Packet type(14) with QoS level 0
                .write(0.byteValue()) // Remaining Length
                .flush()

        then: "no connection should be left"
        conditions.eventually {
            assert mqttBrokerService.mqttConnectionMap.size() == 0
        }
    }

    def "Mqtt broker asset event test"() {

        given: "the container environment is started"
        def mqttBrokerServiceAssetEventCalls = 0
        def spyMqttBrokerService = Spy(MqttBrokerService) {
            sendAssetEvent(_ as String, _ as String, _ as AssetEvent) >> {
                clientId, topic, attributeEvent ->
                    mqttBrokerServiceAssetEventCalls++
                    callRealMethod()
            }
        }

        def conditions = new PollingConditions(timeout: 20, delay: 0.2)
        def services = Lists.newArrayList(defaultServices())
        services.replaceAll { it instanceof MqttBrokerService ? spyMqttBrokerService : it }
        def container = startContainer(defaultConfig(), services)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def mqttBrokerService = container.getService(MqttBrokerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def mqttClientId = managerTestSetup.realmBuildingTenant + MQTT_CLIENT_ID_SEPARATOR + UniqueIdentifierGenerator.generateId()
        def clientId = MqttBrokerService.MQTT_CLIENT_ID_PREFIX + UniqueIdentifierGenerator.generateId(managerTestSetup.realmBuildingTenant)
        def clientSecret = UniqueIdentifierGenerator.generateId(managerTestSetup.realmBuildingTenant)

        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST);
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT);

        when: "a mqtt client connects"
        def payloadLength = mqttClientId.size() + clientId.size() + clientSecret.size()
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
        def topic = "asset/" + managerTestSetup.thingId
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
        topic = "asset/dhajkdasjfgh"
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
        topic = "asset/" + managerTestSetup.apartment1HallwayId
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

        when: "An asset is updated with a new attribute"
        def asset1 = assetStorageService.find(managerTestSetup.apartment1HallwayId)
        asset1.addAttributes(new Attribute<>("temp", TEXT))
        asset1 = assetStorageService.merge(asset1)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAssetEventCalls == 1
        }

        when: "a mqtt client subscribes for child assets of an asset"
        topic = "asset/" + managerTestSetup.apartment1Id +  TOPIC_SEPARATOR + MULTI_LEVEL_WILDCARD
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

        and: "a second subscription is in place"
        conditions.eventually {
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).assetSubscriptions.size() == 2
        }

        and: "a asset is added as child to the subcribed asset"
        def childAsset = new ThingAsset("child")
                .setParentId(managerTestSetup.apartment1Id)
                .setRealm(managerTestSetup.realmBuildingTenant);
        childAsset = assetStorageService.merge(childAsset);

        then: "another event should be sent"
        conditions.eventually {
            assert mqttBrokerServiceAssetEventCalls == 2
        }

        when: "a mqtt client unsubscribes to an asset"
        topic = "asset/" + managerTestSetup.apartment1HallwayId
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


        then: "Only one subscription should be left"
        conditions.eventually {
            assert mqttBrokerService.mqttConnectionMap.get(mqttClientId).assetSubscriptions.size() == 1
        }

        when: "MQTT client disconnects"
        client
        // DISCONNECT
                .write(0xE0) // MQTT Control Packet type(14) with QoS level 0
                .write(0.byteValue()) // Remaining Length
                .flush()

        then: "no connection should be left"
        conditions.eventually {
            assert mqttBrokerService.mqttConnectionMap.size() == 0
        }
    }
}
