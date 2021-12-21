package org.openremote.test.mqtt


import io.moquette.BrokerConstants
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTHandler
import org.openremote.manager.mqtt.MqttBrokerService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer

import static org.openremote.container.util.MapAccess.getInteger
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_HOST
import static org.openremote.manager.mqtt.MqttBrokerService.MQTT_SERVER_LISTEN_PORT
import static org.openremote.model.value.ValueType.TEXT

class MqttBrokerTest extends Specification implements ManagerContainerTrait {

    def "Mqtt broker event test"() {
        given: "the container environment is started"
        List<SharedEvent> receivedEvents = []
        List<Object> receivedValues = []

        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def mqttBrokerService = container.getService(MqttBrokerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def clientEventService = container.getService(ClientEventService.class)
        def agentService = container.getService(AgentService.class)
        def mqttClientId = UniqueIdentifierGenerator.generateId()
        def username = keycloakTestSetup.tenantBuilding.realm + ":" + keycloakTestSetup.serviceUser.username // realm and OAuth client id
        def password = keycloakTestSetup.serviceUser.secret

        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, BrokerConstants.HOST)
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, BrokerConstants.PORT)

        when: "a mqtt client connects with invalid credentials"
        def wrongUsername = "master:" + keycloakTestSetup.serviceUser.username
        MQTT_IOClient client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, true, new UsernamePassword(wrongUsername, password), null)
        client.connect()

        then: "the client connection status should be in error"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.WAITING
        }

        when: "a mqtt client connects with valid credentials"
        client.disconnect()
        mqttClientId = UniqueIdentifierGenerator.generateId()
        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, true, new UsernamePassword(username, password), null)
        client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttClientId) != null
        }

        when: "a mqtt client subscribes to an asset in another realm"
        def topic = "${keycloakTestSetup.tenantCity.realm}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.thingId".toString()
        client.addMessageConsumer(topic, {msg -> })

        then: "No subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
        }

        when: "a mqtt client subscribes with clientId missing"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        client.addMessageConsumer(topic, {msg ->})

        then: "No subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
        }

        when: "a mqtt client subscribes with different clientId"
        def newClientId = UniqueIdentifierGenerator.generateId()
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        client.addMessageConsumer(topic, {msg ->})

        then: "No subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            assert mqttBrokerService.clientIdConnectionMap.get(mqttClientId) != null
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
        }

        when: "a mqtt client subscribes to all attributes of an asset"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        Consumer<MQTTMessage<String>> eventConsumer = { msg ->
            def event = ValueUtil.parse(msg.payload, SharedEvent.class)
            receivedEvents.add(event.get())
        }
        client.addMessageConsumer(topic, eventConsumer)

        then: "A subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
            assert mqttBrokerService.clientIdConnectionMap.get(mqttClientId) != null
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttClientId).size() == 1
        }

        when: "An attribute event occurs for a subscribed attribute"
        def attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 50)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).assetId == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).attributeName == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 50
        }
        receivedEvents.clear()

        when: "Another asset attribute changed the client is subscribed on"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", true)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "A second publish event message should be sent"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).assetId == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).attributeName == "presenceDetected"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(false) == true
        }
        receivedEvents.clear()

        when: "a mqtt client publishes to an asset attribute which is readonly"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_WRITE_TOPIC".toString()
        def payload = ValueUtil.asJSON(new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 70)).get()
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "The value of the attribute should be updated and the client should have received the event"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            def asset = assetStorageService.find(managerTestSetup.apartment1HallwayId)
            assert asset.getAttribute("motionSensor").get().value.orElse(0) == 70d
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).assetId == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).attributeName == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 70d
        }
        receivedEvents.clear()

        when: "a mqtt client publishes to an asset attribute which is not readonly"
        payload = ValueUtil.asJSON(new AttributeEvent(managerTestSetup.apartment1HallwayId, "lights", false)).get()
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the attribute should be updated and the client should have received the event"
        conditions.eventually {
            def asset = assetStorageService.find(managerTestSetup.apartment1HallwayId)
            assert !asset.getAttribute("lights").get().value.orElse(true)
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).assetId == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).attributeName == "lights"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(true) == false
        }
        receivedEvents.clear()

        when: "a mqtt client publishes to an asset attribute value which is not readonly"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/lights/$managerTestSetup.apartment1HallwayId".toString()
        payload = ValueUtil.asJSON(true).orElse(null)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the attribute should be updated and the client should have received the event"
        conditions.eventually {
            def asset = assetStorageService.find(managerTestSetup.apartment1HallwayId)
            assert asset.getAttribute("lights").get().value.orElse(false)
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).assetId == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).attributeName == "lights"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(false) == true
        }
        receivedEvents.clear()

        when: "a mqtt client unsubscribes from an asset"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        client.removeMessageConsumer(topic, eventConsumer)

        then: "No subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
        }

        when: "another asset attribute changed without any subscriptions"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", false)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "No publish event message should be sent"
        new PollingConditions(initialDelay: 1, delay: 1, timeout: 10).eventually {
            assert receivedEvents.size() == 0
        }

        when: "a mqtt client subscribes to a specific asset attribute"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/motionSensor/$managerTestSetup.apartment1HallwayId".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "the subscription should be created"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttClientId).size() == 1
        }

        when: "that attribute changes"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 30)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "a publish event message should be sent to the client"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).assetId == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).attributeName == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 30
        }
        receivedEvents.clear()

        when: "another asset attribute changed without any subscriptions on that attribute"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", true)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "no publish event message should be sent"
        new PollingConditions(initialDelay: 1, delay: 1, timeout: 10).eventually {
            assert receivedEvents.size() == 0
        }

        when: "a mqtt client unsubscribes from an asset attribute"
        client.removeMessageConsumer(topic, eventConsumer)

        then: "No subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
        }

        when: "a mqtt client subscribes to attributes for descendants of an asset"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId/$MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "a subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttClientId).size() == 1
        }

        when: "a child asset of the subscription attribute event occurs"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 40)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "a publish event message should be sent"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).assetId == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).attributeName == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 40
        }
        receivedEvents.clear()

        when: "a mqtt client subscribes to an asset attribute value"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_TOPIC/motionSensor/$managerTestSetup.apartment1HallwayId".toString()
        Consumer<MQTTMessage<String>> valueConsumer = { msg ->
            receivedValues.add(msg.payload)
        }
        client.addMessageConsumer(topic, valueConsumer)

        then: "the subscription should be created"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttClientId).size() == 2
        }

        when: "a subscribed attribute changes"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 50)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "A publish value message should be sent to both subscriptions"
        conditions.eventually {
            assert receivedValues.size() == 1
            assert receivedValues.get(0) == "50"
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).assetId == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).attributeName == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 50
        }
        receivedEvents.clear()
        receivedValues.clear()

        when: "a subscribed attribute changes"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", false)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "only the wildcard subscription should have received a publish"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).assetId == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).attributeName == "presenceDetected"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(true) == false
            assert receivedValues.size() == 0
        }

        when: "the client unsubscribes from the attribute value topic"
        client.removeMessageConsumer(topic, valueConsumer)

        then: "only one subscription should remain"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
            assert client.topicConsumerMap.size() == 1
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttClientId).size() == 1
        }

        when: "a client disconnects"
        client.disconnect()

        then: "the client status should be disconnected"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.DISCONNECTED
            assert mqttBrokerService.clientIdConnectionMap.size() == 0
        }

        when: "the client reconnects"
        receivedEvents.clear()
        client.connect()

        then: "the connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.clientIdConnectionMap.get(mqttClientId) != null
        }

        when: "a mqtt client subscribes to assets that are direct children of the realm"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$mqttClientId/$DefaultMQTTHandler.ASSET_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "A subscription should exist"
        conditions.eventually {
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttClientId).size() == 1
        }

        when: "an asset is updated with a new attribute"
        def asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        asset1 = assetStorageService.merge(asset1)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            assert (receivedEvents.get(0) as AssetEvent).assetId == managerTestSetup.smartBuildingId
            assert (receivedEvents.get(0) as AssetEvent).asset.attributes.get("temp") != null
            assert (receivedEvents.get(0) as AssetEvent).asset.attributes.get("temp").flatMap(){it.getValue()}.orElse(null) == "hello world"
        }
        receivedEvents.clear()

        when: "the client subscribes to descendant assets of a specific asset"
        topic = "${keycloakTestSetup.tenantBuilding.realm}/$mqttClientId/$DefaultMQTTHandler.ASSET_TOPIC/$managerTestSetup.apartment1Id/$MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "the subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttClientId).size() == 2
        }

        when: "an asset is added as a descendant to the subscribed asset"
        def childAsset = new ThingAsset("child")
                .setParentId(managerTestSetup.apartment1LivingroomId)
                .setRealm(managerTestSetup.realmBuildingTenant);
        childAsset = assetStorageService.merge(childAsset);

        then: "another event should be sent"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            assert (receivedEvents.get(0) as AssetEvent).assetId == childAsset.id
            assert (receivedEvents.get(0) as AssetEvent).assetName == childAsset.name
        }
        receivedEvents.clear()

        when: "the mqtt client unsubscribes from the multilevel topic"
        client.removeMessageConsumer(topic, eventConsumer)

        then: "only one subscription should be left"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(mqttClientId)
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(mqttClientId).size() == 1
        }

        when: "the descendant asset is modified"
        childAsset.addAttributes(new Attribute<>("temp2", TEXT))
        childAsset = assetStorageService.merge(childAsset)

        then: "A publish event message should not be sent"
        new PollingConditions(initialDelay: 1, delay: 1, timeout: 10).eventually {
            assert receivedEvents.size() == 0
        }

        when: "MQTT client disconnects"
        client.disconnect()

        then: "no connection should be left"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.DISCONNECTED
            assert mqttBrokerService.clientIdConnectionMap.size() == 0
        }
    }
}
