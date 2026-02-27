package org.openremote.test.mqtt

import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.GatewayMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.GatewayV2Asset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.mqtt.MQTTErrorResponse
import org.openremote.model.mqtt.MQTTGatewayEventMessage
import org.openremote.model.query.AssetQuery
import org.openremote.model.security.User
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.util.ValueUtil
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer

import static org.openremote.container.util.MapAccess.getInteger
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.mqtt.MQTTBrokerService.*
import static org.openremote.model.value.ValueType.TEXT

class MqttGatewayHandlerTest extends Specification implements ManagerContainerTrait {

    // mqtt topic abbreviations
    def assetsTopic = GatewayMQTTHandler.ASSETS_TOPIC
    def attributesTopic = GatewayMQTTHandler.ATTRIBUTES_TOPIC
    def attributesValueTopic = GatewayMQTTHandler.ATTRIBUTES_VALUE_TOPIC
    def createTopic = GatewayMQTTHandler.CREATE_TOPIC
    def updateTopic = GatewayMQTTHandler.UPDATE_TOPIC
    def deleteTopic = GatewayMQTTHandler.DELETE_TOPIC
    def getValueTopic = GatewayMQTTHandler.GET_VALUE_TOPIC
    def getTopic = GatewayMQTTHandler.GET_TOPIC
    def responseTopic = GatewayMQTTHandler.RESPONSE_TOPIC
    def eventsTopic = GatewayMQTTHandler.EVENTS_TOPIC
    def operationsTopic = GatewayMQTTHandler.OPERATIONS_TOPIC
    def gatewayTopic = GatewayMQTTHandler.GATEWAY_TOPIC
    def gatewayEventsTopic = GatewayMQTTHandler.GATEWAY_EVENTS_TOPIC
    def pendingTopic = GatewayMQTTHandler.GATEWAY_PENDING_TOPIC
    def acknowledgeTopic = GatewayMQTTHandler.GATEWAY_ACK_TOPIC


    @SuppressWarnings("GroovyAccessibility")
    def "Mqtt gateway handler test"() {
        given: "the container environment is started"
        List<Object> receivedResponses = [] // request responses
        List<Object> receivedEvents = [] // events from subscriptions
        List<Object> receivedValues = [] // values from attribute value subscriptions
        List<MQTTGatewayEventMessage> receivedPendingGatewayEvents = []
        MQTT_IOClient client = null
        MQTT_IOClient gatewayClient = null
        def conditions = new PollingConditions(timeout: 15, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def clientEventService = container.getService(ClientEventService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def username = keycloakTestSetup.realmBuilding.name + ":" + keycloakTestSetup.serviceUser.username // realm and OAuth client id
        def password = keycloakTestSetup.serviceUser.secret
        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, "0.0.0.0")
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, 1883)


        when: "a mqtt client connects with valid credentials"
        def mqttClientId = UniqueIdentifierGenerator.generateId()
        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, true, new UsernamePassword(username, password), null, null)
        client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
        }

        when: "a mqtt client publishes an asset create operation and subscribes to the response"
        def responseIdentifier = UniqueIdentifierGenerator.generateId()
        def topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/$responseIdentifier/$createTopic".toString()
        def responseTopic = topic + "/response"
        def testAsset = new ThingAsset("gatewayHandlerTestAsset")
        def payload = ValueUtil.asJSON(testAsset).get()

        Consumer<MQTTMessage<String>> messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be created"
        conditions.eventually {
            assert assetStorageService.find(new AssetQuery().names(testAsset.getName())) != null
        }

        and: "a response should be received"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof AssetEvent
            def event = receivedResponses.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.CREATE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAsset.getName()
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()

        when: "a mqtt client publishes an asset create operation and the asset already exists"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/$responseIdentifier/$createTopic".toString()
        responseTopic = topic + "/response"
        testAsset = assetStorageService.find(new AssetQuery().names(testAsset.getName()))
        payload = ValueUtil.asJSON(testAsset).get()

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, MQTTErrorResponse.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "an error response should be received"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof MQTTErrorResponse
            def errorResponse = receivedResponses.get(0) as MQTTErrorResponse
            assert errorResponse.getError() == MQTTErrorResponse.Error.CONFLICT
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()


        when: "a mqtt client publishes an asset update operation and subscribes to the response"
        def testAssetId = assetStorageService.find(new AssetQuery().names(testAsset.getName())).getId()
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/${testAssetId}/${updateTopic}".toString()
        responseTopic = topic + "/response"
        testAsset.setName(testAsset.getName() + "Updated")
        payload = ValueUtil.asJSON(testAsset).get()

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be updated and the response should be received"
        conditions.eventually {
            assert assetStorageService.find(testAssetId).getName() == testAsset.getName()
        }

        and: "a response should be received"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof AssetEvent
            def event = receivedResponses.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.UPDATE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAsset.getName()
        }

        receivedResponses.clear()
        client.removeAllMessageConsumers()

        when: "a mqtt client publishes an asset delete operation and subscribes to the response"
        testAssetId = assetStorageService.find(new AssetQuery().names(testAsset.getName())).getId()
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/${testAssetId}/${deleteTopic}".toString()
        responseTopic = topic + "/response"
        payload = ""

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be deleted"
        conditions.eventually {
            assert assetStorageService.find(testAssetId) == null
        }

        and: "a response should be received"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof AssetEvent
            def event = receivedResponses.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.DELETE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAsset.getName()
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()

        when: "a mqtt client publishes a asset get operation and subscribes to the response topic"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/$managerTestSetup.smartBuildingId/$getTopic".toString()
        responseTopic = topic + "/response"
        payload = ""

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, BuildingAsset.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be received on the response topic"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof BuildingAsset
            def asset = receivedResponses.get(0) as BuildingAsset
            assert asset != null
            assert asset.getId() == managerTestSetup.smartBuildingId
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()

        when: "a mqtt client publishes an attribute update operation and subscribes to the response"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/${managerTestSetup.apartment1HallwayId}/${attributesTopic}/motionSensor/${updateTopic}".toString()
        responseTopic = topic + "/response"
        payload = "70"

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the attribute should be updated"
        conditions.eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 70d
        }

        and: "a response should be received"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof AttributeEvent
            def event = receivedResponses.get(0) as AttributeEvent
            assert event.getName() == "motionSensor"
            assert event.value.get() == 70 || "70"
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()

        when: "a mqtt client publishes an attribute get value operation and subscribes to the response"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/${managerTestSetup.apartment1HallwayId}/${attributesTopic}/motionSensor/${getValueTopic}".toString()
        responseTopic = topic + "/response"
        payload = ""

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, Double.class).orElse(null))
        }

        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the attribute should be received on the response topic"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) == 70d
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()

        when: "a mqtt client publishes a multiple attribute update operation"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/${managerTestSetup.apartment1HallwayId}/${attributesTopic}/${updateTopic}"
        payload = "{\"motionSensor\": 80, \"presenceDetected\": \"true\"}"
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the values of the attributes should be updated"
        conditions.eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 80d
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("presenceDetected").get().value.orElse(false) == true
        }

        when: "a mqtt client subscribes to all asset events"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)

        then: "then a subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
        }

        when: "a mqtt client unsubscribes from all asset events"
        client.removeMessageConsumer(topic, messageConsumer)

        then: "then the subscription should be removed"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
        }

        when: "a mqtt client subscribes to all asset events of the realm and an asset event is triggered"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        testAsset = new ThingAsset("gatewayHandlerTestAsset")
        testAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        testAsset.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(testAsset)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.CREATE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAsset.getName()
        }
        receivedEvents.clear()
        client.removeAllMessageConsumers()


        when: "a mqtt client subscribes to all asset events of the realm's direct children and a child is updated"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        def asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        assetStorageService.merge(asset1)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.UPDATE
            def asset = event.getAsset()
            assert asset.getName() == asset1.getName()
        }
        receivedEvents.clear()

        when: "a non-direct child asset is created"
        ThingAsset asset1Child = new ThingAsset("gatewayHandlerTestAsset1Child")
        asset1Child.setRealm(keycloakTestSetup.realmBuilding.name)
        asset1Child.setParent(asset1)
        asset1Child.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(asset1Child)

        then: "then the asset event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()

        when: "a mqtt client subscribes to all asset events of a asset and the asset is updated"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/${managerTestSetup.apartment1HallwayId}".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        def apartment1Hallway = assetStorageService.find(managerTestSetup.apartment1HallwayId)
        apartment1Hallway.setNotes("Updated notes")
        assetStorageService.merge(apartment1Hallway)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.UPDATE
            def asset = event.getAsset()
            assert asset.getName() == apartment1Hallway.getName()
            assert asset.getNotes().get() == "Updated notes"
        }
        receivedEvents.clear()

        when: "another asset is updated"
        asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.setNotes("Updated notes")
        assetStorageService.merge(asset1)

        then: "then the asset event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()

        when: "a mqtt client subscribes to all asset events of an asset's descendants and a descendant asset is created"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/${managerTestSetup.apartment1Id}/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        ThingAsset childAsset = new ThingAsset("gatewayHandlerTestChildAsset")
        childAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        childAsset.setParentId(managerTestSetup.apartment1Id)
        childAsset.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(childAsset)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.CREATE
            def asset = event.getAsset()
            assert asset.getName() == childAsset.getName()
        }
        receivedEvents.clear()

        when: "a non-descendant asset is updated"
        asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.setNotes("Updated notes")
        assetStorageService.merge(asset1)

        then: "then the asset event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()

        when: "a mqtt client subscribes to all asset events of an asset's direct children and a direct child asset is updated"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/${managerTestSetup.apartment1Id}/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        childAsset.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        assetStorageService.merge(childAsset)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.UPDATE
            def asset = event.getAsset()
            assert asset.getName() == childAsset.getName()
        }
        receivedEvents.clear()

        when: "a non-direct descendant asset is created"
        def childChildAsset = new ThingAsset("gatewayHandlerTestChildChildAsset")
        childChildAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        childChildAsset.setParent(childAsset)
        childChildAsset.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(childChildAsset) // non-direct descendant

        then: "then the asset event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()


        when: "a mqtt client subscribes to all attribute events of the realm"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/+/${attributesTopic}/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true"))

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "presenceDetected"
            assert event.value.get() == true
        }
        receivedEvents.clear()
        client.removeAllMessageConsumers()


        when: "a mqtt client subscribes to all attribute events of the realm's direct children"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/+/${attributesTopic}/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.smartBuildingId, "notes", "hello world"))

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "notes"
            assert event.value.get() == "hello world"
        }
        receivedEvents.clear()

        when: "an attribute is updated of an asset that is not a direct descendant"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "notes", "hello world"))

        then: "then the attribute event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()


        when: "a mqtt client subscribes to all attribute events with a specific attribute name and an attribute is updated"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/+/${attributesTopic}/motionSensor/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80"))


        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "motionSensor"
            assert event.value.get() == 80
        }
        receivedEvents.clear()

        when: "an attribute with a different name is updated"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true"))

        then: "then the attribute event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()

        when: "a mqtt client subscribes to all attribute events with a specific name of the realm's direct children and an attribute is updated"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/+/${attributesTopic}/notes/+".toString()

        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.smartBuildingId, "notes", "hello world"))

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "notes"
            assert event.value.get() == "hello world"
        }
        receivedEvents.clear()

        when: "an attribute is updated of an asset that is not a direct descendant"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "notes", "hello world"))

        then: "then the attribute event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }

        when: "an attribute is updated with a different name"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80"))

        then: "then the attribute event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()

        when: "a mqtt client subscribes to all attribute events of a specific asset and an attribute is updated"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/${managerTestSetup.apartment1HallwayId}/${attributesTopic}".toString()

        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true"))

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "presenceDetected"
            assert event.value.get() == true
        }
        receivedEvents.clear()

        when: "an attribute is updated of a different asset"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.smartBuildingId, "notes", "hello world"))

        then: "then the attribute event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()


        when: "a mqtt client subscribes to all attribute events of an asset's descendants with a specific attribute name and the attribute is updated"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/${managerTestSetup.smartBuildingId}/${attributesTopic}/motionSensor/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80"))

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "motionSensor"
            assert event.value.get() == 80
        }
        receivedEvents.clear()

        when: "a different attribute is updated"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.smartBuildingId, "notes", "hello world"))

        then: "then the attribute event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()

        when: "a mqtt client subscribes to all attribute events of an asset's direct children with a specific attribute name and an attribute is updated"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/${managerTestSetup.apartment1Id}/${attributesTopic}/motionSensor/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)

        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80"))

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "motionSensor"
            assert event.value.get() == 80
        }
        receivedEvents.clear()

        when: "a non-direct child is updated with the same attribute name"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.smartBuildingId, "motionSensor", "80"))

        then: "then the attribute event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()

        when: "a mqtt client subscribes to all attribute events of the realm with the value only prefix"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/+/${attributesValueTopic}/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedValues.add(ValueUtil.parse(msg.payload, Object.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true"))

        then: "then only the value of the attribute event should be received"
        conditions.eventually {
            assert receivedValues.size() == 1
            assert receivedValues.get(0) == true
        }
        receivedValues.clear()
        client.removeAllMessageConsumers()

        when: "a mqtt client subscribes to all attribute events of the realm's direct children with the value only prefix"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/+/${attributesValueTopic}/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedValues.add(ValueUtil.parse(msg.payload, Object.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.smartBuildingId, "notes", "hello world"))

        then: "then the value of the attribute event should be received"
        conditions.eventually {
            assert receivedValues.size() == 1
            assert receivedValues.get(0) == "hello world"
        }
        receivedValues.clear()
        client.removeAllMessageConsumers()

        when: "an attribute is updated of an asset that is not a direct descendant with the value only prefix"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "notes", "hello world"))

        then: "then the value of the attribute event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedValues.size() == 0
        }
        client.removeAllMessageConsumers()

        when: "a mqtt client subscribes to all attribute events with a specific attribute name and an attribute is updated with the value only prefix"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$eventsTopic/$assetsTopic/+/${attributesValueTopic}/motionSensor/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedValues.add(ValueUtil.parse(msg.payload, Object.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80"))

        then: "then the value of the attribute event should be received"
        conditions.eventually {
            assert receivedValues.size() == 1
            assert receivedValues.get(0) == 80
        }
        receivedValues.clear()
        client.removeAllMessageConsumers()

        when: "an attribute with a different name is updated with the value only prefix"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true"))

        then: "then the value of the attribute event should not be received"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert receivedValues.size() == 0
        }
        client.removeAllMessageConsumers()


        // create the gateway asset so we can test the gateway v2 asset service user connection/behavior
        when: "a gateway v2 asset is created"
        def tempGatewayAsset = new GatewayV2Asset("gatewayV2Asset")
        tempGatewayAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        tempGatewayAsset.setId(UniqueIdentifierGenerator.generateId())
        def gatewayAsset = assetStorageService.merge(tempGatewayAsset)

        then: "the gateway v2 asset should exist"
        conditions.eventually {
            assert gatewayAsset != null
            assert gatewayAsset.getClientId().isPresent()
            assert gatewayAsset.getClientSecret().isPresent()
        }

        and: "a service user is created for the gateway asset"
        conditions.eventually {
            assert keycloakTestSetup.keycloakProvider.getUserByUsername(keycloakTestSetup.realmBuilding.name,
                    User.SERVICE_ACCOUNT_PREFIX + gatewayAsset.getClientId().get()) != null
        }

        def gatewayClientId = gatewayAsset.getClientId().get()
        def gatewayMqttUsername = keycloakTestSetup.realmBuilding.name + ":" + gatewayClientId // realm:clientId
        def gatewayServiceUsername = User.SERVICE_ACCOUNT_PREFIX + gatewayClientId // service-account-clientId
        def gatewayClientSecret = gatewayAsset.getClientSecret().get() // password
        def gatewayUser = keycloakTestSetup.keycloakProvider.getUserByUsername(keycloakTestSetup.realmBuilding.name, gatewayServiceUsername)

        when: "a mqtt client connects to the mqtt broker with gateway service user"
        gatewayClient = new MQTT_IOClient(gatewayClientId, mqttHost, mqttPort, false, true,
                new UsernamePassword(gatewayMqttUsername, gatewayClientSecret), null, null)
        gatewayClient.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert gatewayClient.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(gatewayUser.getId()).size() == 1
        }


        when: "a gateway service user publishes a create asset message with a parent that is not its descendant or itself"
        responseIdentifier = UniqueIdentifierGenerator.generateId()
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$operationsTopic/$assetsTopic/$responseIdentifier/${createTopic}"
        testAsset = new ThingAsset("mqttGatewayHandlerTestAsset")
        testAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        testAsset.setId(UniqueIdentifierGenerator.generateId())
        testAsset.setParentId(managerTestSetup.smartBuildingId)
        payload = ValueUtil.asJSON(testAsset).get()
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should not be created"
        conditions.eventually {
            def asset = (ThingAsset) assetStorageService.find(new AssetQuery().names(testAsset.getName()))
            assert asset == null
        }


        when: "a gateway service user publishes a create asset message without a parentId"
        responseIdentifier = UniqueIdentifierGenerator.generateId()
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$operationsTopic/$assetsTopic/$responseIdentifier/${createTopic}"
        testAsset = new ThingAsset("mqttGatewayHandlerTestAsset")
        testAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        testAsset.setId(UniqueIdentifierGenerator.generateId())
        testAsset.setParentId(null)
        payload = ValueUtil.asJSON(testAsset).get()
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be created"
        conditions.eventually {
            def asset = (ThingAsset) assetStorageService.find(new AssetQuery().names(testAsset.getName()))
            assert asset != null
            asset.getName() == testAsset.getName()
        }

        and: "the asset should be a direct child of the gateway asset"
        conditions.eventually {
            def asset = (ThingAsset) assetStorageService.find(new AssetQuery().names(testAsset.getName()))
            assert asset.getParentId() == gatewayAsset.getId()
        }


        when: "a gateway service user subscribes to pending gateway attribute events"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$gatewayTopic/$gatewayEventsTopic/${pendingTopic}".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedPendingGatewayEvents.add(ValueUtil.parse(msg.payload, MQTTGatewayEventMessage.class).orElse(null))
        }
        gatewayClient.addMessageConsumer(topic, messageConsumer)

        then: "the subscription should exist"
        conditions.eventually {
            assert gatewayClient.topicConsumerMap.get(topic) != null
            assert gatewayClient.topicConsumerMap.get(topic).size() == 1
        }

        when: "a pending attribute event is published"
        testAsset = (ThingAsset) assetStorageService.find(new AssetQuery().names(testAsset.getName()))
        assetProcessingService.sendAttributeEvent(new AttributeEvent(testAsset.getId(), "notes", "hello gateway"))

        then: "the pending attribute event should be received"
        conditions.eventually {
            assert receivedPendingGatewayEvents.size() == 1
            def gatewayEventMessage = receivedPendingGatewayEvents.get(0)
            assert gatewayEventMessage instanceof MQTTGatewayEventMessage
            assert gatewayEventMessage.getEvent().getName() == "notes"
            assert gatewayEventMessage.getEvent().getValue().get() == "hello gateway"
        }

        and: "the attribute event should not have been processed yet"
        conditions.eventually {
            def asset = assetStorageService.find(testAsset.getId())
            assert asset.getAttribute("notes").get().value.orElse(null) != "hello gateway"
        }

        when: "a pending attribute event is acknowledged with the incorrect ackId"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$gatewayTopic/$gatewayEventsTopic/${acknowledgeTopic}".toString()
        def ackId = UniqueIdentifierGenerator.generateId()
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, ackId))

        then: "the pending attribute event should not be processed"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            def asset = assetStorageService.find(testAsset.getId())
            assert asset.getAttribute("notes").get().value.orElse(null) != "hello gateway"
        }

        when: "a pending attribute event is acknowledged"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$gatewayTopic/$gatewayEventsTopic/${acknowledgeTopic}".toString()
        ackId = receivedPendingGatewayEvents.get(0).getAckId()
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, ackId))

        then: "the pending attribute event should be processed"
        conditions.eventually {
            def asset = assetStorageService.find(testAsset.getId())
            assert asset.getAttribute("notes").get().value.orElse(null) == "hello gateway"
        }
        receivedPendingGatewayEvents.clear()

        when: "a gateway service publishes an attribute update operation"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$operationsTopic/$assetsTopic/${testAsset.getId()}/${attributesTopic}/notes/${updateTopic}"
        payload = "hello gateway updated"
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the attribute should be updated"
        conditions.eventually {
            def asset = assetStorageService.find(testAsset.getId())
            assert asset.getAttribute("notes").get().value.orElse(null) == "hello gateway updated"
        }

        and: "the attribute event should not be received as a pending event"
        conditions.eventually {
            assert receivedPendingGatewayEvents.size() == 0
        }
        gatewayClient.removeAllMessageConsumers()

        when: "a gateway service user publishes a update attribute operation for an asset that is not a descendant"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$operationsTopic/$assetsTopic/${managerTestSetup.smartBuildingId}/${attributesTopic}/notes/${updateTopic}"
        payload = "hello gateway"
        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }
        gatewayClient.addMessageConsumer(topic + "/response", messageConsumer)
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the attribute should not be updated"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            def asset = assetStorageService.find(managerTestSetup.smartBuildingId)
            assert asset.getAttribute("notes").get().value.orElse(null) != "hello gateway"
        }

        when: "a gateway service user publishes a get attribute operation for an asset that is not a descendant and subscribes to the response"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$operationsTopic/$assetsTopic/${managerTestSetup.smartBuildingId}/${attributesTopic}/notes/${getValueTopic}"
        payload = ""
        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, String.class).orElse(null))
        }
        gatewayClient.addMessageConsumer(topic + "/response", messageConsumer)
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the attribute value should not be received on the response topic"
        conditions.eventually {
            assert receivedResponses.size() == 0
        }
        receivedResponses.clear()
        gatewayClient.removeAllMessageConsumers()


        when: "a non-gateway service user publishes an asset update operation for a gateways descendant asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/${testAsset.getId()}/${updateTopic}"
        def originalName = testAsset.getName()
        testAsset.setName(testAsset.getName() + "Updated")
        payload = ValueUtil.asJSON(testAsset).get()

        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should not be updated"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            def updatedAsset = (ThingAsset) assetStorageService.find(new AssetQuery().names(testAsset.getName()))
            assert updatedAsset == null // we should not be able to find the asset since the non-gateway service user tried to update it
            def asset = (ThingAsset) assetStorageService.find(new AssetQuery().names(originalName))
            assert asset != null
            assert asset.getName() == originalName
        }
        testAsset.setName(originalName) // reset the name

        when: "a non-gateway service user publishes an asset update that changes the parentId of a non-gateway descendant to a gateway asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/${testAsset.getId()}/${updateTopic}"
        asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.setParentId(gatewayAsset.getId())
        payload = ValueUtil.asJSON(testAsset).get()
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset parent should not be updated"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            def asset = assetStorageService.find(asset1.getId())
            assert asset.getParentId() != gatewayAsset.getId()
        }

        when: "a non-gateway service user publishes an asset delete operation for a gateways descendant asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$operationsTopic/$assetsTopic/${testAsset.getId()}/${deleteTopic}"
        payload = ""

        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should not be deleted"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            def asset = assetStorageService.find(new AssetQuery().names(testAsset.getName()))
            assert asset != null
            assert asset.getName() == testAsset.getName()
        }

        when: "a gateway service user publishes a update asset operation"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$operationsTopic/$assetsTopic/${testAsset.getId()}/${updateTopic}"
        testAsset.setName(testAsset.getName() + "Updated")
        payload = ValueUtil.asJSON(testAsset).get()
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be updated"
        conditions.eventually {
            def asset = assetStorageService.find(testAsset.getId())
            assert asset.getName() == testAsset.getName()
        }

        when: "a gateway service user publishes an asset update asset that moves the asset out of the gateway's hierarchy"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$operationsTopic/$assetsTopic/${testAsset.getId()}/${updateTopic}"
        testAsset.setParentId(managerTestSetup.apartment1HallwayId)
        payload = ValueUtil.asJSON(testAsset).get()
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset parent should not be updated"
        conditions.eventually {
            def asset = assetStorageService.find(testAsset.getId())
            assert asset.getParentId() != managerTestSetup.apartment1HallwayId
            assert asset.getParentId() == gatewayAsset.getId()
        }

        when: "a gateway service user publishes an asset update that moves a non-gateway asset to the gateways hierarchy"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$operationsTopic/$assetsTopic/${managerTestSetup.apartment1HallwayId}/${updateTopic}"
        asset1 = assetStorageService.find(managerTestSetup.apartment1HallwayId)
        asset1.setParentId(gatewayAsset.getId())
        payload = ValueUtil.asJSON(asset1).get()
        gatewayClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset parent should not be updated"
        conditions.eventually {
            def asset = assetStorageService.find(managerTestSetup.apartment1HallwayId)
            assert asset.getParentId() != gatewayAsset.getId()
        }


        when: "a gateway service user publishes a delete asset operation"
        topic = "${keycloakTestSetup.realmBuilding.name}/$gatewayClientId/$operationsTopic/$assetsTopic/${testAsset.getId()}/${deleteTopic}"
        payload = ""

        gatewayClient.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be deleted"
        conditions.eventually {
            def storedAsset = assetStorageService.find(testAsset.getId())
            assert storedAsset == null
        }

        cleanup: "disconnect the clients"
        if (client != null) {
            client.disconnect()
        }
        if (gatewayClient != null) {
            gatewayClient.disconnect()
        }
    }
}