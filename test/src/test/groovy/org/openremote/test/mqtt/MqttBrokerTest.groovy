package org.openremote.test.mqtt

import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3AsyncClientView
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3ClientConfigView
import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientConnectionConfig
import io.netty.channel.socket.SocketChannel
import org.openremote.agent.protocol.mqtt.MQTTLastWill
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer

import static org.openremote.container.util.MapAccess.getInteger
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.mqtt.MQTTBrokerService.MQTT_SERVER_LISTEN_HOST
import static org.openremote.manager.mqtt.MQTTBrokerService.MQTT_SERVER_LISTEN_PORT
import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString
import static org.openremote.model.value.ValueType.TEXT

class MqttBrokerTest extends Specification implements ManagerContainerTrait {

    def "Mqtt broker event test"() {
        given: "the container environment is started"
        List<SharedEvent> receivedEvents = []
        List<Object> receivedValues = []
        MQTT_IOClient client = null
        MQTT_IOClient newClient = null
        def conditions = new PollingConditions(timeout: 15, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def clientEventService = container.getService(ClientEventService.class)
        def agentService = container.getService(AgentService.class)
        def mqttClientId = UniqueIdentifierGenerator.generateId()
        def username = keycloakTestSetup.realmBuilding.name + ":" + keycloakTestSetup.serviceUser.username // realm and OAuth client id
        def password = keycloakTestSetup.serviceUser.secret

        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, "0.0.0.0")
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, 1883)

        when: "a mqtt client connects with invalid credentials"
        def wrongUsername = "master:" + keycloakTestSetup.serviceUser.username
        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, true, new UsernamePassword(wrongUsername, password), null, null)
        client.connect()

        then: "the client connection status should be in error"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.WAITING
        }

        when: "a mqtt client connects with valid credentials"
        client.disconnect()
        mqttClientId = UniqueIdentifierGenerator.generateId()
        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, true, new UsernamePassword(username, password), null, null)
        client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.sessionKeyInfoMap.containsKey(getConnectionIDString(connection))
        }

        when: "the client disconnects"
        def currentConnection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
        client.disconnect()

        then: "the client resources should be freed"
        conditions.eventually {
            assert clientEventService.sessionKeyInfoMap.isEmpty()
            assert !clientEventService.sessionKeyInfoMap.containsKey(getConnectionIDString(currentConnection))
        }

        when: "a mqtt client connects with valid credentials"
        mqttClientId = UniqueIdentifierGenerator.generateId()
        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, true, new UsernamePassword(username, password), null, null)
        client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.sessionKeyInfoMap.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes to an asset in another realm"
        def topic = "${keycloakTestSetup.realmCity.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.thingId".toString()
        client.addMessageConsumer(topic, {msg -> })

        then: "No subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes with clientId missing"
        topic = "${keycloakTestSetup.realmBuilding.name}/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        client.addMessageConsumer(topic, {msg ->})

        then: "No subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes with different clientId"
        def newClientId = UniqueIdentifierGenerator.generateId()
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        client.addMessageConsumer(topic, {msg ->})

        then: "No subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes to all attributes of an asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        Consumer<MQTTMessage<String>> eventConsumer = { msg ->
            def event = ValueUtil.parse(msg.payload, SharedEvent.class)
            receivedEvents.add(event.get())
        }
        client.addMessageConsumer(topic, eventConsumer)

        then: "A subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 1
        }

        when: "An attribute event occurs for a subscribed attribute"
        def attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 50)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "motionSensor"
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
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "presenceDetected"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(false) == true
        }
        receivedEvents.clear()

        when: "a mqtt client publishes to an asset attribute"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/motionSensor/${managerTestSetup.apartment1HallwayId}".toString()
        def payload = "70"
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "The value of the attribute should be updated and the client should have received the event"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 70d
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 70d
        }
        receivedEvents.clear()

        when: "a mqtt client publishes to an asset attribute"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/lights/${managerTestSetup.apartment1HallwayId}".toString()
        payload = Boolean.FALSE.toString()
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the attribute should be updated and the client should have received the event"
        conditions.eventually {
            assert !assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("lights").get().value.orElse(true)
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "lights"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(true) == false
        }
        receivedEvents.clear()

        when: "a mqtt client publishes to an asset attribute value"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/lights/$managerTestSetup.apartment1HallwayId".toString()
        payload = ValueUtil.asJSON(true).orElse(null)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the attribute should be updated and the client should have received the event"
        conditions.eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("lights").get().value.orElse(false)
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "lights"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(false) == true
        }
        receivedEvents.clear()

        when: "a mqtt client unsubscribes from an asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        client.removeMessageConsumer(topic, eventConsumer)

        then: "No subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
        }

        when: "another asset attribute changed without any subscriptions"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", false)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "No publish event message should be sent"
        new PollingConditions(initialDelay: 1, delay: 1, timeout: 10).eventually {
            assert receivedEvents.size() == 0
        }

        when: "a mqtt client subscribes to a specific asset attribute"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/motionSensor/$managerTestSetup.apartment1HallwayId".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "the subscription should be created"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 1
        }

        when: "that attribute changes"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 30)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "a publish event message should be sent to the client"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "motionSensor"
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
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes to attributes for descendants of an asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId/$MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "a subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 1
        }

        when: "a child asset of the subscription attribute event occurs"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 40)
        ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "a publish event message should be sent"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 40
        }
        receivedEvents.clear()

        when: "a mqtt client subscribes to an asset attribute value"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_TOPIC/motionSensor/$managerTestSetup.apartment1HallwayId".toString()
        Consumer<MQTTMessage<String>> valueConsumer = { msg ->
            receivedValues.add(msg.payload)
        }
        client.addMessageConsumer(topic, valueConsumer)

        then: "the subscription should be created"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 2
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
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "motionSensor"
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
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "presenceDetected"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(true) == false
            assert receivedValues.size() == 0
        }

        when: "the client unsubscribes from the attribute value topic"
        client.removeMessageConsumer(topic, valueConsumer)

        then: "only one subscription should remain"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
            assert client.topicConsumerMap.size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 1
        }

        when: "a client disconnects"
        client.disconnect()

        then: "the client status should be disconnected"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.DISCONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 0
        }

        when: "the client reconnects"
        receivedEvents.clear()
        client.connect()

        then: "the connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
        }

        when: "a mqtt client subscribes to assets that are direct children of the realm"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ASSET_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "A subscription should exist"
        conditions.eventually {
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 1
        }

        when: "an asset is updated with a new attribute"
        def asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        asset1 = assetStorageService.merge(asset1)

        then: "A publish event message should be sent"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            assert (receivedEvents.get(0) as AssetEvent).id == managerTestSetup.smartBuildingId
            assert (receivedEvents.get(0) as AssetEvent).asset.attributes.get("temp") != null
            assert (receivedEvents.get(0) as AssetEvent).asset.attributes.get("temp").flatMap(){it.getValue()}.orElse(null) == "hello world"
        }
        receivedEvents.clear()

        when: "the client subscribes to descendant assets of a specific asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ASSET_TOPIC/$managerTestSetup.apartment1Id/$MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "the subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 2
        }

        when: "an asset is added as a descendant to the subscribed asset"
        def childAsset = new ThingAsset("child")
                .setParentId(managerTestSetup.apartment1LivingroomId)
                .setRealm(managerTestSetup.realmBuildingName)
        childAsset = assetStorageService.merge(childAsset)

        then: "another event should be sent"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            assert (receivedEvents.get(0) as AssetEvent).id == childAsset.id
            assert (receivedEvents.get(0) as AssetEvent).assetName == childAsset.name
        }
        receivedEvents.clear()

        when: "the mqtt client unsubscribes from the multilevel topic"
        client.removeMessageConsumer(topic, eventConsumer)

        then: "only one subscription should be left"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 1
        }

        when: "the descendant asset is modified"
        childAsset.addAttributes(new Attribute<>("temp2", TEXT))
        childAsset = assetStorageService.merge(childAsset)

        then: "A publish event message should not be sent"
        new PollingConditions(initialDelay: 1, delay: 1, timeout: 10).eventually {
            assert receivedEvents.size() == 0
        }

        when: "another client connects with a last will configured to write to an attribute"
        newClientId = "newClient"
        def username2 = keycloakTestSetup.realmBuilding.name + ":" + keycloakTestSetup.serviceUser2.username // realm and OAuth client id
        def password2 = keycloakTestSetup.serviceUser2.secret
        newClient = new MQTT_IOClient(newClientId, mqttHost, mqttPort, false, true, new UsernamePassword(username2, password2), null, new MQTTLastWill("${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/motionSensor/$managerTestSetup.apartment1HallwayId".toString(), "1000", false))
        newClient.connect()

        then: "the client should be connected"
        conditions.eventually {
            assert newClient.getConnectionStatus() == ConnectionStatus.CONNECTED
        }

        when: "the existing client subscribes to attribute events in the hallway"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        client.addMessageConsumer(topic, eventConsumer)

        and: "the new client publishes to a an attribute topic"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/motionSensor/${managerTestSetup.apartment1HallwayId}".toString()
        payload = "170"
        newClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "The value of the attribute should be updated and the first client should have received the event"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 170d
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 170d
        }
        receivedEvents.clear()

        and: "the new client gets abruptly disconnected"
        def existingConnection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
//        ((NioSocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)device1Client.client).clientConfig).delegate).connectionConfig.get()).channel).config().setOption(ChannelOption.SO_LINGER, 0I)
        ((SocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)newClient.client).clientConfig).delegate).connectionConfig.get()).channel).close()

        then: "the client should reconnect"
        conditions.eventually {
            assert !mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).isEmpty()
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0] !== existingConnection
        }

        and: "The last will message should have updated the value of the attribute and the first client should have received the event"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 1000d
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 1000d
        }
        receivedEvents.clear()

        when: "the new client tries to publish again"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/motionSensor/${managerTestSetup.apartment1HallwayId}".toString()
        payload = "170"
        newClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "The value of the attribute should be updated and the first client should have received the event"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 170d
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 170d
        }
        receivedEvents.clear()

        when: "the new client disconnects"
        newClient.disconnect()

        then: "the status should show as disconnected"
        conditions.eventually {
            assert newClient.getConnectionStatus() == ConnectionStatus.DISCONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).isEmpty()
        }

        when: "the new client reconnects using a last will topic for an unauthorised asset/attribute"
        def asset = assetStorageService.find(managerTestSetup.apartment2LivingroomId)
        def currentCO2Level = asset.getAttribute("co2Level").flatMap{it.getValue()}.orElse(0d)
        newClient = new MQTT_IOClient(newClientId, mqttHost, mqttPort, false, true, new UsernamePassword(username2, password2), null, new MQTTLastWill("${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/co2Level/$managerTestSetup.apartment2LivingroomId".toString(), "1000", false))
        newClient.connect()

        then: "the client should be connected"
        conditions.eventually {
            assert newClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).size() == 1
        }

        when: "the new client gets abruptly disconnected"
        existingConnection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
//        ((NioSocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)device1Client.client).clientConfig).delegate).connectionConfig.get()).channel).config().setOption(ChannelOption.SO_LINGER, 0I)
        ((SocketChannel)((MqttClientConnectionConfig)((MqttClientConfig)((Mqtt3ClientConfigView)((Mqtt3AsyncClientView)newClient.client).clientConfig).delegate).connectionConfig.get()).channel).close()

        then: "the client should reconnect"
        conditions.eventually {
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).size() == 1
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0] !== existingConnection
        }

        then: "The last will message should not have updated the value of the attribute"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            def asst = assetStorageService.find(managerTestSetup.apartment2LivingroomId)
            assert asst.getAttribute("co2Level").get().value.orElse(-100) == currentCO2Level
        }

        when: "a restricted mqtt client subscribes to an unlinked asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1KitchenId".toString()
        newClient.addMessageConsumer(topic, {msg ->})

        then: "no subscription should exist"
        conditions.eventually {
            assert newClient.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
        }

        when: "a restricted mqtt client subscribes to a linked asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        newClient.addMessageConsumer(topic, {msg ->})

        then: "a subscription should exist"
        conditions.eventually {
            assert newClient.topicConsumerMap.get(topic) != null
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
            assert clientEventService.eventSubscriptions.sessionSubscriptionIdMap.get(getConnectionIDString(connection)).size() == 1
        }

        when: "a user asset link is added for a connected restricted user"
        existingConnection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
        assetStorageService.storeUserAssetLinks(List.of(
                new UserAssetLink(keycloakTestSetup.realmBuilding.getName(),
                        keycloakTestSetup.serviceUser2.getId(),
                        managerTestSetup.apartment1BathroomId)))

        then: "the existing connection should not have been terminated"
        new PollingConditions(initialDelay: 2, timeout: 10, delay: 1).eventually {
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).size() == 1
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0].is(existingConnection)
        }

        when: "a user asset link is removed for a connected restricted user"
        existingConnection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
        assetStorageService.deleteUserAssetLinks(List.of(
                new UserAssetLink(keycloakTestSetup.realmBuilding.getName(),
                        keycloakTestSetup.serviceUser2.getId(),
                        managerTestSetup.apartment1HallwayId)))

        then: "the existing connection should be terminated and the client should reconnect"
        conditions.eventually {
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).size() == 1
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0] !== existingConnection
        }

        when: "the restricted mqtt client removes all subscriptions"
        newClient.removeAllMessageConsumers() // Clear subscriptions as otherwise it won't hit the server again

        then: "no subscriptions should exist in the client"
        assert newClient.topicConsumerMap.isEmpty()

        when: "the restricted mqtt client subscribes to a now unlinked asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        newClient.addMessageConsumer(topic, {msg ->})

        then: "no subscription should exist"
        conditions.eventually {
            assert newClient.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
            assert !clientEventService.eventSubscriptions.sessionSubscriptionIdMap.containsKey(getConnectionIDString(connection))
        }

        // TODO: Further MQTT tests
//        when: "an attribute event occurs on an attribute that the restricted user can access"
//
//        then: "they should receive the event"
//
//        when: "an attribute event occurs on an attribute that the restricted user cannot access"
//
//        then: "they should not receive the event"
//
//        when: "an asset event occurs on an asset that the restricted user can access"
//
//        then: "they should receive the event but it should only contain restricted attributes"

        when: "both MQTT clients disconnect"
        client.disconnect()
        newClient.disconnect()

        then: "no connections should be left"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.DISCONNECTED
            assert newClient.getConnectionStatus() == ConnectionStatus.DISCONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).isEmpty()
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).isEmpty()
        }

        cleanup: "disconnect the clients"
        if (client != null) {
            client.disconnect()
        }
        if (newClient != null) {
            newClient.disconnect()
        }
    }
}
