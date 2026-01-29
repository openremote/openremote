package org.openremote.test.mqtt

import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3AsyncClientView
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3ClientConfigView
import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientConnectionConfig
import io.netty.channel.socket.SocketChannel
import io.undertow.security.idm.X509CertificateCredential
import org.apache.activemq.artemis.spi.core.security.jaas.RolePrincipal
import org.apache.activemq.artemis.spi.core.security.jaas.UserPrincipal
import org.keycloak.KeycloakPrincipal
import org.openremote.agent.protocol.mqtt.MQTTLastWill
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.mqtt.MQTTHandler
import org.openremote.manager.provisioning.ProvisioningService
import org.openremote.manager.security.KeyStoreServiceImpl
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.security.RemotingConnectionPrincipal
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.security.ClientRole
import org.openremote.model.security.User
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.util.ValueUtil
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.provisioning.MTLSCertificateHelper
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.security.auth.Subject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import java.util.stream.Stream

import static org.openremote.model.util.MapAccess.getInteger
import static org.openremote.model.util.MapAccess.getString
import static org.openremote.manager.mqtt.MQTTBrokerService.*
import static org.openremote.model.value.MetaItemType.ACCESS_RESTRICTED_READ
import static org.openremote.model.value.ValueType.TEXT

class MqttBrokerTest extends Specification implements ManagerContainerTrait {

    def "Mqtt broker event test"() {
        given: "the container environment is started"
        List<SharedEvent> receivedEvents = new CopyOnWriteArrayList<>()
        List<SharedEvent> restrictedReceivedEvents = new CopyOnWriteArrayList<>()
        List<Object> receivedValues = new CopyOnWriteArrayList<>()
        MQTT_IOClient client = null
        MQTT_IOClient newClient = null
        def conditions = new PollingConditions(timeout: 15, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def defaultMQTTHandler = mqttBrokerService.getCustomHandlers().find {it instanceof DefaultMQTTHandler} as DefaultMQTTHandler
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
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTING
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
        }

        when: "the client disconnects"
        mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
        client.disconnect()

        then: "the client resources should be freed"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.DISCONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).isEmpty()
        }

        when: "a mqtt client connects with valid credentials"
        List<String> subFailures = new CopyOnWriteArrayList<>()
        mqttClientId = UniqueIdentifierGenerator.generateId()
        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, true, new UsernamePassword(username, password), null, null)
        client.setTopicSubscribeFailureConsumer {subFailures.add(it)}
        client.setRemoveConsumersOnSubscriptionFailure(true)
        client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
        }

        when: "a mqtt client subscribes to an asset in another realm"
        def topic = "${keycloakTestSetup.realmCity.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.thingId".toString()
        client.addMessageConsumer(topic, {msg -> })

        then: "No subscription should exist"
        conditions.eventually {
            assert subFailures.size() == 1
            assert subFailures[0] == topic
            assert client.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes with clientId missing"
        topic = "${keycloakTestSetup.realmBuilding.name}/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        client.addMessageConsumer(topic, {msg ->})

        then: "No subscription should exist"
        conditions.eventually {
            assert subFailures.size() == 2
            assert subFailures[1] == topic
            assert client.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes with different clientId"
        def newClientId = UniqueIdentifierGenerator.generateId()
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        client.addMessageConsumer(topic, {msg ->})

        then: "No subscription should exist"
        conditions.eventually {
            assert subFailures.size() == 3
            assert subFailures[2] == topic
            assert client.topicConsumerMap.get(topic) == null // Consumer added and removed on failure
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes to all attributes of an asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        Consumer<MQTTMessage<String>> eventConsumer = { msg ->
            def event = ValueUtil.parse(msg.payload as String, SharedEvent.class)
            receivedEvents.add(event.get())
        }
        client.addMessageConsumer(topic, eventConsumer)

        then: "A subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).consumers.size() == 1
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 1
        }

        when: "An attribute event occurs for a subscribed attribute"
        def attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 50)
        assetProcessingService.sendAttributeEvent(attributeEvent)

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
        assetProcessingService.sendAttributeEvent(attributeEvent)

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
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        when: "another asset attribute changed without any subscriptions"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", false)
        assetProcessingService.sendAttributeEvent(attributeEvent)

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
            assert client.topicConsumerMap.get(topic).consumers.size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 1
        }

        when: "that attribute changes"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 30)
        assetProcessingService.sendAttributeEvent(attributeEvent)

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
        assetProcessingService.sendAttributeEvent(attributeEvent)

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
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes to attributes for descendants of an asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId/$MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "a subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).consumers.size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 1
        }

        when: "a child asset of the subscription attribute event occurs"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 40)
        assetProcessingService.sendAttributeEvent(attributeEvent)

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
            assert client.topicConsumerMap.get(topic).consumers.size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 2
        }

        when: "a subscribed attribute changes"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 50)
        assetProcessingService.sendAttributeEvent(attributeEvent)

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
        assetProcessingService.sendAttributeEvent(attributeEvent)

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
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 1
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

        then: "the connection should exist and the previous subscription should be recreated"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert client.topicConsumerMap.size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 1
        }

        when: "a mqtt client subscribes to assets that are direct children of the realm"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ASSET_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD".toString()
        client.addMessageConsumer(topic, eventConsumer)

        then: "A subscription should exist"
        conditions.eventually {
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 2
        }

        when: "an asset is updated with a new attribute"
        def asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        assetStorageService.merge(asset1)

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
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 3
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

        then: "only two subscriptions should be left"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 2
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

        when: "the new client publishes to an attribute topic"
        receivedEvents.clear()
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

        when: "the new client tries to publish again with a specified timestamp"
        def specifiedTimestamp = (receivedEvents.get(0) as AttributeEvent).timestamp + 5
        receivedEvents.clear()
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_WRITE_TOPIC/motionSensor/${managerTestSetup.apartment1HallwayId}".toString()
        payload = "{\"value\": 170, \"timestamp\": ${specifiedTimestamp}}".toString()
        newClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "The value of the attribute should be updated and the first client should have received the event"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 170d
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            assert (receivedEvents.get(0) as AttributeEvent).id == managerTestSetup.apartment1HallwayId
            assert (receivedEvents.get(0) as AttributeEvent).name == "motionSensor"
            assert (receivedEvents.get(0) as AttributeEvent).value.orElse(0) == 170d
            assert (receivedEvents.get(0) as AttributeEvent).timestamp == specifiedTimestamp
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
        newClient.setTopicSubscribeFailureConsumer {subFailures.add(it)}
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
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1BathroomId".toString()
        newClient.addMessageConsumer(topic, {msg ->})

        then: "the subscription should fail but the consumer should still exist (as setRemoveConsumersOnSubscriptionFailure=false)"
        conditions.eventually {
            assert subFailures.size() == 4
            assert subFailures[3] == topic
            assert newClient.topicConsumerMap.get(topic) != null
            assert newClient.topicConsumerMap.get(topic).consumers.size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        when: "a restricted mqtt client subscribes to a linked asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        newClient.addMessageConsumer(topic, {msg ->})

        then: "a subscription should exist"
        conditions.eventually {
            assert newClient.topicConsumerMap.get(topic) != null
            assert newClient.topicConsumerMap.get(topic).consumers.size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
            assert defaultMQTTHandler.sessionSubscriptionConsumers.get(getConnectionIDString(connection)).size() == 1
        }

        when: "a user asset link is added for a connected restricted user"
        existingConnection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
        assetStorageService.storeUserAssetLinks(List.of(
                new UserAssetLink(keycloakTestSetup.realmBuilding.getName(),
                        keycloakTestSetup.serviceUser2.getId(),
                        managerTestSetup.apartment1BathroomId)))

        then: "the existing connection should be terminated and the client should reconnect and the previously failed subscription should now succeed"
        conditions.eventually {
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).size() == 1
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0] !== existingConnection
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        when: "a user asset link is removed for a connected restricted user"
        existingConnection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
        assetStorageService.deleteUserAssetLinks(List.of(
                new UserAssetLink(keycloakTestSetup.realmBuilding.getName(),
                        keycloakTestSetup.serviceUser2.getId(),
                        managerTestSetup.apartment1HallwayId)))

        then: "the existing connection should be terminated and the client should reconnect and the previously failed subscription should be tried again but also fail"
        conditions.eventually {
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).size() == 1
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0] !== existingConnection
        }

        when: "the restricted mqtt client removes all subscriptions"
        newClient.removeAllMessageConsumers() // Clear subscriptions as otherwise it won't hit the server again

        then: "no subscriptions should exist in the client"
        assert newClient.topicConsumerMap.isEmpty()

        Consumer<MQTTMessage<String>> restrictedEventConsumer = { msg ->
            def event = ValueUtil.parse(msg.payload as String, SharedEvent.class)
            restrictedReceivedEvents.add(event.get())
        }

        when: "the restricted mqtt client subscribes to a now unlinked asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        newClient.addMessageConsumer(topic, restrictedEventConsumer)

        then: "no subscription should exist"
        conditions.eventually {
            assert subFailures.size() == 6
            assert subFailures[5] == topic
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
            assert !defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        when: "the previously failed message consumer is removed"
        newClient.removeMessageConsumer(topic, restrictedEventConsumer)

        and: "a restricted mqtt client subscribes to the attribute wildcard topic"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD".toString()
        newClient.addMessageConsumer(topic, restrictedEventConsumer)

        then: "a subscription should exist"
        conditions.eventually {
            assert newClient.topicConsumerMap.get(topic) != null
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0]
            assert defaultMQTTHandler.sessionSubscriptionConsumers.containsKey(getConnectionIDString(connection))
        }

        when: "an unlinked asset attribute event occurs"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", 40)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the restricted client should not receive the event"
        conditions.eventually {
            assert restrictedReceivedEvents.size() == 0
        }

        when: "a linked asset attribute event occurs"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1BathroomId, RoomAsset.ROOM_NUMBER, 1)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the restricted client should not receive the event since the attribute is not restricted"
        conditions.eventually {
            assert restrictedReceivedEvents.size() == 0
        }

        when: "a linked asset attribute has its restricted meta configuration set to true"
        def bathroomAsset = assetStorageService.find(managerTestSetup.apartment1BathroomId)
        bathroomAsset.getAttributes().get(RoomAsset.ROOM_NUMBER).get().addOrReplaceMeta(new MetaItem<>(ACCESS_RESTRICTED_READ, true))
        assetStorageService.merge(bathroomAsset)

        then: "the restricted client should still be connected"
        conditions.eventually {
            assert newClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).size() == 1
        }

        then: "the restricted client should receive the triggered attribute event"
            conditions.eventually {
            assert restrictedReceivedEvents.size() == 1
        }
        restrictedReceivedEvents.clear()

        when: "another attribute event occurs on the restricted read attribute"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1BathroomId, RoomAsset.ROOM_NUMBER, 2)
        assetProcessingService.sendAttributeEvent(attributeEvent)

        then: "the restricted client should receive the triggered attribute event"
         conditions.eventually {
            assert restrictedReceivedEvents.size() == 1
            assert restrictedReceivedEvents.get(0) == attributeEvent
        }
        restrictedReceivedEvents.clear()

        when: "the restricted read meta item is removed"
        bathroomAsset = assetStorageService.find(managerTestSetup.apartment1BathroomId)
        bathroomAsset.getAttributes().addOrReplace(new Attribute<>(RoomAsset.ROOM_NUMBER, 2))
        assetStorageService.merge(bathroomAsset)

        then: "the restricted client should not receive the triggered attribute event"
            conditions.eventually {
            assert restrictedReceivedEvents.size() == 0
        }
        newClient.removeAllMessageConsumers()
        restrictedReceivedEvents.clear()

        when: "a restricted mqtt client subscribes to the asset wildcard topic"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ASSET_TOPIC/$MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD".toString()
        newClient.addMessageConsumer(topic, restrictedEventConsumer)

        then: "a subscription should exist"
        conditions.eventually {
            assert newClient.topicConsumerMap.get(topic) != null
        }

        when: "a linked asset event occurs"
        bathroomAsset = assetStorageService.find(managerTestSetup.apartment1BathroomId)
        bathroomAsset.getAttributes().get(RoomAsset.ROOM_NUMBER).get().setValue(2)
        assetStorageService.merge(bathroomAsset)

        then: "the restricted client should receive the triggered asset event"
        conditions.eventually {
            assert restrictedReceivedEvents.size() == 1
        }

        and: "the event should only contain attributes that are read restricted"
        conditions.eventually {
            assert restrictedReceivedEvents.size() == 1
            def event = restrictedReceivedEvents.get(0)
            assert event instanceof AssetEvent
            def assetEvent = event as AssetEvent
            assert !assetEvent.getAsset().hasAttribute(RoomAsset.ROOM_NUMBER)
        }
        restrictedReceivedEvents.clear()

        when: "the restricted read meta item is added again"
        bathroomAsset = assetStorageService.find(managerTestSetup.apartment1BathroomId)
        bathroomAsset.getAttributes().get(RoomAsset.ROOM_NUMBER).get().addOrReplaceMeta(new MetaItem<>(ACCESS_RESTRICTED_READ, true))
        assetStorageService.merge(bathroomAsset)

        then: "the restricted client should receive the triggered asset event"
        conditions.eventually {
            assert restrictedReceivedEvents.size() == 1
            def event = restrictedReceivedEvents.get(0)
            assert event instanceof AssetEvent
            def assetEvent = event as AssetEvent
            assert assetEvent.getAsset().getAttribute(RoomAsset.ROOM_NUMBER).isPresent()
            assert assetEvent.getAsset().getAttribute(RoomAsset.ROOM_NUMBER).get().getValue().orElse(null) == 2
        }
        restrictedReceivedEvents.clear()

        when: "the asset is unlinked"
        assetStorageService.deleteUserAssetLinks(List.of(
                new UserAssetLink(keycloakTestSetup.realmBuilding.getName(),
                        keycloakTestSetup.serviceUser2.getId(),
                        managerTestSetup.apartment1BathroomId)))


        then: "the existing connection should be terminated and the client should reconnect"
        conditions.eventually {
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id).size() == 1
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser2.id)[0] !== existingConnection
        }

        when: "the restricted mqtt client removes all subscriptions"
        newClient.removeAllMessageConsumers() // Clear subscriptions as otherwise it won't hit the server again

        then: "no subscriptions should exist in the client"
        assert newClient.topicConsumerMap.isEmpty()

        when: "the restricted mqtt client subscribes to the asset wildcard topic again"
        topic = "${keycloakTestSetup.realmBuilding.name}/$newClientId/$DefaultMQTTHandler.ASSET_TOPIC/$MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD".toString()
        newClient.addMessageConsumer(topic, restrictedEventConsumer)

        then: "a subscription should exist"
        conditions.eventually {
            assert newClient.topicConsumerMap.get(topic) != null
        }

        when: "an asset event occurs on the now unlinked asset"
        bathroomAsset = assetStorageService.find(managerTestSetup.apartment1BathroomId)
        bathroomAsset.getAttributes().get(RoomAsset.ROOM_NUMBER).get().setValue(2)
        assetStorageService.merge(bathroomAsset)

        then: "the restricted client should not receive the triggered asset event"
        conditions.eventually {
            assert restrictedReceivedEvents.size() == 0
        }
        restrictedReceivedEvents.clear()
        newClient.removeAllMessageConsumers()

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

    def "Mqtt broker mTLS test"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.1)
        MQTT_IOClient device1Client

        and: "temporary directories are created"
        def tempDir = new File(System.getProperty("java.io.tmpdir"), "openremote-mtls-test-keystores-" + System.currentTimeMillis())
        def tempManagerDir = new File(System.getProperty("java.io.tmpdir"), "openremote-tmp-" + System.currentTimeMillis())
        tempDir.mkdirs()
        tempManagerDir.mkdirs()
        getLOG().info("KeyStore TempDir: $tempDir.absoluteFile")
        getLOG().info("Manager TempDir: $tempManagerDir.absoluteFile")

        and: "test configuration is set up"
        def serverKeystorePath = new File(tempDir, "server-keystore.p12").absolutePath
        def serverTruststorePath = new File(tempDir, "server-truststore.p12").absolutePath
        def keystorePassword = "secret1"
        def provisionedAccountAliasName = "mtlsclient"
        def provisionedAccountKeyAlias = "$Constants.MASTER_REALM.$provisionedAccountAliasName"
        def ProvisionedAccountUserName = "mtlstest2"

        and: "mTLS certificate helper is initialized"
        def mtlsHelper = new MTLSCertificateHelper()

        and: "server and client certificates are generated"
        def (serverKeyPair, serverCert) = mtlsHelper.generateServerCertificate()
        def (clientKeyPair, clientCert) = mtlsHelper.generateClientCertificate(ProvisionedAccountUserName, Constants.MASTER_REALM)

        and: "server keystores are created and saved to disk"
        mtlsHelper.createAndSaveServerKeystores(
                serverKeystorePath,
                serverTruststorePath,
                keystorePassword,
                provisionedAccountKeyAlias,
                serverKeyPair,
                serverCert
        )

        and: "the container configuration is set up with MTLS environment variables"
        def config = defaultConfig()
        config.put(Constants.OR_MQTT_MTLS_SERVER_LISTEN_HOST, "localhost")
        config.put(Constants.OR_MQTT_MTLS_SERVER_LISTEN_PORT, "8884")
        config.put(Constants.OR_MQTT_MTLS_KEYSTORE_PATH, serverKeystorePath)
        config.put(Constants.OR_MQTT_MTLS_KEYSTORE_PASSWORD, keystorePassword)
        config.put(Constants.OR_MQTT_MTLS_TRUSTSTORE_PATH, serverTruststorePath)
        config.put(Constants.OR_MQTT_MTLS_TRUSTSTORE_PASSWORD, keystorePassword)
        config.put(Constants.OR_MQTT_MTLS_DISABLED, "false")
        config.put(KeyStoreServiceImpl.OR_KEYSTORE_PASSWORD, keystorePassword)
        config.put(PersistenceService.OR_STORAGE_DIR, tempManagerDir.getAbsolutePath())


        and: "the container starts"
        def container = startContainer(config, defaultServices())
        def keystoreService = container.getService(KeyStoreServiceImpl.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def provisioningService = container.getService(ProvisioningService.class)
        def mqttHost = "localhost"
        def mqttPort = 8884

        and: "the client certificate is added to the KeyStoreService"
        mtlsHelper.addClientCertificateToKeyStoreService(
                keystoreService,
                provisionedAccountKeyAlias,
                keystorePassword,
                clientKeyPair,
                clientCert
        )

        expect: "the keystores should contain the certificates"
        keystoreService.getKeyStore().containsAlias(provisionedAccountKeyAlias)
        keystoreService.getTrustStore().containsAlias(provisionedAccountKeyAlias)

        when: "A new service user with the corresponding certificate's fields is created"
        User serviceUser = new User()
                .setServiceAccount(true)
                .setEnabled(true)
                .setRealm("master")
                .setUsername(ProvisionedAccountUserName);
        serviceUser = identityService.getIdentityProvider().createUpdateUser("master", serviceUser, null, true);

        identityService.getIdentityProvider().updateUserClientRoles(
                Constants.MASTER_REALM,
                serviceUser.getId(),
                Constants.KEYCLOAK_CLIENT_ID,
                Stream.of(ClientRole.READ_ASSETS, ClientRole.WRITE_ASSETS, ClientRole.WRITE_ATTRIBUTES).map(ClientRole::getValue).toArray(String[]::new)
        );

        then: "the service user should exist"
        def fetchedUser = identityService.getIdentityProvider().getUserByUsername("master", "$User.SERVICE_ACCOUNT_PREFIX$ProvisionedAccountUserName")
        assert fetchedUser != null

        when: "an MQTT client is created with mTLS configuration"
        def device1UniqueId = "device1"
        def mqttDevice1ClientId = device1UniqueId
        device1Client = new MQTT_IOClient(
                mqttDevice1ClientId,
                mqttHost,
                mqttPort,
                true,
                false,
                null,
                null,
                null,
                keystoreService.getKeyManagerFactory(provisionedAccountKeyAlias),
                keystoreService.getTrustManagerFactory()
        )

        device1Client.connect()

        then: "the client should connect successfully using mTLS"
        conditions.eventually {
            assert device1Client.getConnectionStatus() == ConnectionStatus.CONNECTED
        }

        and: "the authenticated client should have the correct subject"
        conditions.eventually {
            Subject sub = mqttBrokerService.getConnectionFromClientID(mqttDevice1ClientId).getSubject()
            assert sub.getPrincipals().size() == 6
            assert sub.getPrincipals(RolePrincipal.class).size() == 3
            assert sub.getPrincipals(RolePrincipal.class).stream().map {rp -> rp.getName()}.toList().containsAll(Constants.READ_ASSETS_ROLE, Constants.WRITE_ASSETS_ROLE, Constants.WRITE_ATTRIBUTES_ROLE)
            assert sub.getPrincipals(UserPrincipal).size() == 1
            assert sub.getPrincipals(UserPrincipal)[0].getName() == "$User.SERVICE_ACCOUNT_PREFIX$ProvisionedAccountUserName"
            assert sub.getPrincipals(KeycloakPrincipal.class).size() == 1
            assert sub.getPrincipals(KeycloakPrincipal.class)[0].getName() == "$User.SERVICE_ACCOUNT_PREFIX$ProvisionedAccountUserName"
            assert sub.getPrincipals(RemotingConnectionPrincipal.class)[0].getRemotingConnection().getClientID() == mqttDevice1ClientId

            assert sub.getPrivateCredentials(X509CertificateCredential.class).size() == 1
            assert sub.getPrivateCredentials(X509CertificateCredential.class)[0].getCertificate().getSubjectX500Principal().getName().contains("OU=$Constants.MASTER_REALM")
            assert sub.getPrivateCredentials(X509CertificateCredential.class)[0].getCertificate().getSubjectX500Principal().getName().contains("CN=$ProvisionedAccountUserName")

            assert sub.getPublicCredentials().size() == 0
        }

        when: "A new keypair is created, without being signed by the issuer"
        def (invalidKeyPair, invalidCert) = mtlsHelper.generateSelfSignedCertificate("invaliduser", "master")

        and: "Added and saved to the KeyStoreService keystore"
        def invalidKeyAlias = Constants.MASTER_REALM+".invalidclient"
        mtlsHelper.addCertificateToKeyStoreService(
                keystoreService,
                invalidKeyAlias,
                keystorePassword,
                invalidKeyPair,
                invalidCert,
                false // Don't include root CA in chain for invalid cert
        )

        and: "A new MQTT client connects, using those new certificates"
        def invalidClientId = UniqueIdentifierGenerator.generateId("invaliddevice")
        MQTT_IOClient invalidClient = new MQTT_IOClient(
                invalidClientId,
                mqttHost,
                mqttPort,
                true,
                false,
                null,
                null,
                null,
                keystoreService.getKeyManagerFactory(null),
                keystoreService.getTrustManagerFactory()
        )
        invalidClient.connectTimeout = 100
        invalidClient.connect();

        then: "The connection should be rejected"

        conditions.eventually {
            assert invalidClient.getConnectionStatus() == ConnectionStatus.CONNECTING
            assert invalidClient != null
        }

        and: "disconnect invalid client if connected"
        invalidClient.disconnect()

        when: "A new keypair, properly signed, is created, but the service user for it is not created"
        def unprovisionedUsername = "unprovisioneduser"
        def (unprovisionedKeyPair, unprovisionedCert) = mtlsHelper.generateClientCertificate(
                unprovisionedUsername,
                Constants.MASTER_REALM,
                4 // serial offset
        )

        and: "It is added and saved to the KeyStoreService keystore"
        def unprovisionedKeyAlias = Constants.MASTER_REALM + "unprovisionedclient"
        mtlsHelper.addClientCertificateToKeyStoreService(
                keystoreService,
                unprovisionedKeyAlias,
                keystorePassword,
                unprovisionedKeyPair,
                unprovisionedCert
        )

        and: "A new MQTT client connects, using those correct yet unprovisioned certificates"
        def unprovisionedClientId = UniqueIdentifierGenerator.generateId("unprovisioneddevice")
        unprovisionedClientId = unprovisionedUsername;
        MQTT_IOClient unprovisionedClient = new MQTT_IOClient(
                unprovisionedClientId,
                mqttHost,
                mqttPort,
                true,
                false,
                null,
                null,
                null,
                keystoreService.getKeyManagerFactory(unprovisionedKeyAlias),
                keystoreService.getTrustManagerFactory()
        )

        unprovisionedClient.connect()

        then: "the connection should be created, but there should be no roles, but there is a ProvisioningPrincipal, that contains the correct certificate"
        conditions.eventually {
            assert unprovisionedClient.getConnectionStatus() == ConnectionStatus.CONNECTED
        }

        and: "the authenticated client should have a ProvisioningPrincipal with no user roles"
        conditions.eventually {
            Subject unprovisionedJaasSubject = mqttBrokerService.getConnectionFromClientID(unprovisionedClientId).getSubject()
            assert unprovisionedJaasSubject != null
            assert unprovisionedJaasSubject.getPrincipals().size() == 3
            assert unprovisionedJaasSubject.getPrivateCredentials().size() == 1
            // Should not have role principals since no service user exists
            assert unprovisionedJaasSubject.getPrincipals(RolePrincipal.class)[0].getName() == "anonymous"
            // Should not have a UserPrincipal or KeycloakPrincipal
            assert unprovisionedJaasSubject.getPrincipals(UserPrincipal)[0].getName() == "anonymous"
            assert unprovisionedJaasSubject.getPrincipals(KeycloakPrincipal.class).isEmpty()
            // Should have a ProvisioningPrincipal with the certificate
            assert unprovisionedJaasSubject.getPrivateCredentials(X509CertificateCredential.class).size() == 1
            def provisioningPrincipal = unprovisionedJaasSubject.getPrivateCredentials(X509CertificateCredential.class)[0]
            assert provisioningPrincipal.getCertificate() != null
            assert provisioningPrincipal.getCertificate().getSubjectX500Principal().getName().contains("CN=$unprovisionedUsername")
        }

        cleanup: "disconnect the client and cleanup temporary files"
        device1Client.disconnect()

        and: "disconnect unprovisioned client"
        unprovisionedClient.disconnect()

        and: "delete the created temporary directories"
        tempDir.deleteDir()
        tempManagerDir.deleteDir()
    }
}
