package org.openremote.test.mqtt

import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.agent.AgentService
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
import org.openremote.model.query.AssetQuery
import org.openremote.model.security.User
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

    @SuppressWarnings("GroovyAccessibility")
    def "Mqtt gateway handler test"() {
        given: "the container environment is started"
        List<Object> receivedResponses = []
        List<Object> receivedEvents = []
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

        //region Test: Connect to MQTT broker
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
        //endregion

        //region Test: Pub create asset
        when: "a mqtt client publishes a create message for with a valid thing asset template"
        def responseIdentifier = UniqueIdentifierGenerator.generateId()
        def topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/$responseIdentifier/$GatewayMQTTHandler.CREATE_TOPIC".toString()
        def testAssetOne = new ThingAsset("gatewayHandlerTestAssetOne")
        def payload = ValueUtil.asJSON(testAssetOne).get()
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be created with the specified name"
        conditions.eventually {
            assert assetStorageService.find(new AssetQuery().names(testAssetOne.getName())) != null
        }
        //endregion

        //region Test: Pub asset and sub response
        when: "a mqtt client publishes a create asset message and subscribes to corresponding response topic"
        responseIdentifier = UniqueIdentifierGenerator.generateId()
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/$responseIdentifier/$GatewayMQTTHandler.CREATE_TOPIC".toString()
        def responseTopic = topic + "/response"
        def testAssetTwo = new ThingAsset("gatewayHandlerTestAssetTwo")
        payload = ValueUtil.asJSON(testAssetTwo).get()

        Consumer<MQTTMessage<String>> messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the thing asset should be created and the asset event should be received on the response topic"
        conditions.eventually {
            assert assetStorageService.find(new AssetQuery().names(testAssetTwo.getName())) != null
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof AssetEvent
            def event = receivedResponses.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.CREATE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAssetTwo.getName()
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Pub update asset
        when: "a mqtt client publishes an update message for a specific asset"
        def testAssetId = assetStorageService.find(new AssetQuery().names(testAssetOne.getName())).getId();
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${testAssetId}/update".toString()
        testAssetOne.setName(testAssetOne.getName() + "Updated")
        payload = ValueUtil.asJSON(testAssetOne).get()
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be updated with the specified name"
        conditions.eventually {
            assert assetStorageService.find(testAssetId).getName() == testAssetOne.getName()
        }
        //endregion


        //region Test: Pub update asset and sub response
        when: "a mqtt client publishes an update message for a specific asset and subscribes to the corresponding response topic"
        testAssetId = assetStorageService.find(new AssetQuery().names(testAssetTwo.getName())).getId();
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${testAssetId}/update".toString()
        responseTopic = topic + "/response"
        testAssetTwo.setName(testAssetTwo.getName() + "Updated")
        payload = ValueUtil.asJSON(testAssetTwo).get()

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be updated and the response should be received"
        conditions.eventually {
            assert assetStorageService.find(testAssetId).getName() == testAssetTwo.getName()
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof AssetEvent
            def event = receivedResponses.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.UPDATE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAssetTwo.getName()
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()
        //endregion


        //region Test: Pub delete asset
        when: "a mqtt client publishes a delete message for a specific asset (assetOne)"
        testAssetId = assetStorageService.find(new AssetQuery().names(testAssetOne.getName())).getId();
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${testAssetId}/delete".toString()
        payload = ""
        client.sendMessage(new MQTTMessage<String>(topic, payload))
        then: "the asset should be deleted"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert assetStorageService.find(testAssetId) == null
        }
        //endregion

        //region Test: Pub delete asset and sub response
        when: "a mqtt client publishes a delete message for a specific asset and subscribes to the corresponding response topic (assetTwo)"
        testAssetId = assetStorageService.find(new AssetQuery().names(testAssetTwo.getName())).getId();
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${testAssetId}/delete".toString()
        responseTopic = topic + "/response"
        payload = ""

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be deleted and the response should be received"
        conditions.eventually {
            assert assetStorageService.find(testAssetId) == null
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof AssetEvent
            def event = receivedResponses.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.DELETE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAssetTwo.getName()
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()
        //endregion


        //region Test: Pub get asset and sub response
        when: "a mqtt client publishes a get asset message for a specific asset and subscribes to the corresponding response topic"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/$managerTestSetup.smartBuildingId/get".toString()
        responseTopic = topic + "/response"
        payload = ""

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, BuildingAsset.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the specified asset should be received on the response topic"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof BuildingAsset
            def asset = receivedResponses.get(0) as BuildingAsset
            assert asset != null
            assert asset.getId() == managerTestSetup.smartBuildingId
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()
        //endregion


        //region Test: Pub update attribute
        when: "a mqtt client publishes an update message for a specific asset and attribute"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${managerTestSetup.apartment1HallwayId}/attributes/motionSensor/update".toString()
        payload = "60"
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the attribute should be updated accordingly"
        conditions.eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 60d
        }
        //endregion

        //region Test: Pub update attribute and sub response
        when: "a mqtt client publishes an update message for a specific asset and attribute and subscribes to the corresponding response topic"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${managerTestSetup.apartment1HallwayId}/attributes/motionSensor/update".toString()
        responseTopic = topic + "/response"
        payload = "70"

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the specified attribute should be updated and the response should be received"
        conditions.eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 70d
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof AttributeEvent
            def event = receivedResponses.get(0) as AttributeEvent
            assert event.getName() == "motionSensor"
            assert event.value.get() == "70"
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Pub get value attribute and sub response
        when: "a mqtt client publishes a get attribute value message for a specific asset and attribute and subscribes to the corresponding response topic"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${managerTestSetup.apartment1HallwayId}/attributes/motionSensor/get-value".toString()
        responseTopic = topic + "/response"
        payload = ""

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, Double.class).orElse(null))
        }

        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the specified attribute should be received on the response topic"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) == 70d
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Pub update multiple attributes
        when: "a mqtt client publishes an update message for multiple attributes of a specific asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${managerTestSetup.apartment1HallwayId}/attributes/update"
        payload = "{\"motionSensor\": 80, \"presenceDetected\": \"true\"}"
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the values of the attributes should be updated accordingly"
        conditions.eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 80d
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("presenceDetected").get().value.orElse(false) == true
        }
        //endregion


        //region Test: Sub events topic
        when: "a mqtt client subscribes to an events topic"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)

        then: "then a subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) != null
            assert client.topicConsumerMap.get(topic).size() == 1
        }
        //endregion

        //region Test: Unsub events topic
        when: "a mqtt client unsubscribes from an events topic"
        client.removeMessageConsumer(topic, messageConsumer)

        then: "then the subscription should be removed"
        conditions.eventually {
            assert client.topicConsumerMap.get(topic) == null
        }
        //endregion

        //region Test: Sub all asset events of realm
        when: "a mqtt client subscribes to all asset events of the realm"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)
        ThingAsset testAssetThree = new ThingAsset("gatewayHandlerTestAssetThree")
        testAssetThree.setRealm(keycloakTestSetup.realmBuilding.name)
        testAssetThree.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(testAssetThree)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.CREATE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAssetThree.getName()
        }
        receivedEvents.clear()
        //endregion

        //region Test: Receive update on asset events sub
        when: "a mqtt client is subscribed to all asset events of the realm and an asset is updated"
        testAssetThree.setName("gatewayHandlerTestAssetThreeUpdated")
        assetStorageService.merge(testAssetThree)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.UPDATE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAssetThree.getName()
        }
        receivedEvents.clear()
        //endregion

        //region Test: Receive delete on asset events sub
        when: "a mqtt client is subscribed to all asset events of the realm and an asset is deleted"
        List<String> assetIdsToRemove = new ArrayList<>()
        assetIdsToRemove.add(testAssetThree.getId())
        assetStorageService.delete(assetIdsToRemove)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.DELETE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAssetThree.getName()
        }
        receivedEvents.clear()
        client.removeAllMessageConsumers();
        //endregion

        //region Test: Sub all asset events realm's direct children
        when: "a mqtt client subscribes to all asset events of the realm's direct children"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        testAssetThree = new ThingAsset("gatewayHandlerTestAssetThree")
        testAssetThree.setRealm(keycloakTestSetup.realmBuilding.name)
        testAssetThree.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(testAssetThree) // direct descendant

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.CREATE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAssetThree.getName()
        }
        receivedEvents.clear()
        //endregion

        //region Test: Don't receive non-direct events on direct children asset sub
        when: "a mqtt client subscribes to all asset events of the realm's direct descendants and an asset is created that is not a direct descendant"
        ThingAsset testAssetThreeChild = new ThingAsset("gatewayHandlerTestAssetThreeChild")
        testAssetThreeChild.setRealm(keycloakTestSetup.realmBuilding.name)
        testAssetThreeChild.setParent(testAssetThree)
        testAssetThreeChild.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(testAssetThreeChild)

        then: "then the asset event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        receivedEvents.clear()
        //endregion

        //region Test: Receive update on direct children asset sub
        when: "a mqtt client is subscribed to all asset events of the realm's direct descendants and a direct descendant asset is updated"
        testAssetThree.setName("gatewayHandlerTestAssetThreeUpdated")
        assetStorageService.merge(testAssetThree)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.UPDATE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAssetThree.getName()
        }
        receivedEvents.clear()
        assetIdsToRemove.clear()
        assetIdsToRemove.add(testAssetThreeChild.getId())
        assetStorageService.delete(assetIdsToRemove) // cleanup, delete child asset
        //endregion

        //region Test: Receive delete on direct children asset sub
        when: "a mqtt client is subscribed to all asset events of the realm's direct descendants and a direct descendant asset is deleted"
        assetIdsToRemove.clear()
        assetIdsToRemove.add(testAssetThree.getId())
        assetStorageService.delete(assetIdsToRemove)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.DELETE
            def asset = event.getAsset()
            assert asset instanceof ThingAsset
            assert asset.getName() == testAssetThree.getName()
        }
        receivedEvents.clear()
        client.removeAllMessageConsumers();
        //endregion

        //region Test: Sub all asset events of specific asset
        when: "a mqtt client subscribes to all asset events of a specific asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/${managerTestSetup.apartment1HallwayId}".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        var apartment1Hallway = assetStorageService.find(managerTestSetup.apartment1HallwayId)
        var name = apartment1Hallway.getName()
        apartment1Hallway.setNotes("Updated notes")
        assetStorageService.merge(apartment1Hallway)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.UPDATE
            def asset = event.getAsset()
            assert asset.getName() == name
            assert asset.getNotes().get() == "Updated notes"
        }
        receivedEvents.clear()
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Sub all asset events of asset's descendants
        when: "a mqtt client subscribes to all asset events of a specific asset's descendants"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/${managerTestSetup.apartment1Id}/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        var apartment1 = assetStorageService.find(managerTestSetup.apartment1Id)
        ThingAsset childAsset = new ThingAsset("gatewayHandlerTestChildAsset")
        childAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        childAsset.setParent(apartment1)
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
        //endregion


        //region Test: Receive create on asset descendants events sub
        when: "a mqtt client is subscribed to all asset events of a specific asset's descendants and a descendant asset is created"
        ThingAsset childChildAsset = new ThingAsset("gatewayHandlerTestChildChildAsset")
        childChildAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        childChildAsset.setParent(childAsset)
        childChildAsset.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(childChildAsset)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.CREATE
            def asset = event.getAsset()
            assert asset.getName() == childChildAsset.getName()
        }
        receivedEvents.clear()
        //endregion

        //region Test: Receive update on asset descendants events sub
        when: "a mqtt client is subscribed to all asset events of a specific asset's descendants and a descendant asset is updated"
        childChildAsset.setName("gatewayHandlerTestChildChildAssetUpdated")
        assetStorageService.merge(childChildAsset)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.UPDATE
            def asset = event.getAsset()
            assert asset.getName() == childChildAsset.getName()
        }
        receivedEvents.clear()
        //endregion

        //region Test: Receive delete on asset descendants events sub
        when: "a mqtt client is subscribed to all asset events of a specific asset's descendants and a descendant asset is deleted"
        assetIdsToRemove.clear()
        assetIdsToRemove.add(childAsset.getId())
        assetIdsToRemove.add(childChildAsset.getId())
        assetStorageService.delete(assetIdsToRemove)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 2 // two events, one for each asset
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.DELETE
            assert receivedEvents.get(1) instanceof AssetEvent
            event = receivedEvents.get(1) as AssetEvent
            assert event.cause == AssetEvent.Cause.DELETE
        }
        receivedEvents.clear()
        client.removeAllMessageConsumers();
        //endregion

        //region Test: Sub all asset events of asset's direct children
        when: "a mqtt client subscribes to all asset events of a specific asset's direct children"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/${managerTestSetup.apartment1Id}/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AssetEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        apartment1 = assetStorageService.find(managerTestSetup.apartment1Id)
        childAsset = new ThingAsset("gatewayHandlerTestChildAsset")
        childAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        childAsset.setParent(apartment1)
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
        //endregion

        //region Test: Don't receive non-direct attribute on direct children events sub
        when: "a mqtt client subscribed to all asset events of a specific asset's direct children and a non-direct descendant asset is created"
        childChildAsset = new ThingAsset("gatewayHandlerTestChildChildAsset")
        childChildAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        childChildAsset.setParent(childAsset)
        childChildAsset.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(childChildAsset) // non-direct descendant

        assetIdsToRemove.clear()
        assetIdsToRemove.add(childChildAsset.getId())
        assetStorageService.delete(assetIdsToRemove) // another event, delete the child child asset (should not be received)

        then: "then the asset event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        receivedEvents.clear()
        //endregion

        //region Test: Receive update on asset direct children events sub
        when: "a mqtt client is subscribed to all asset events of a specific asset's direct children and a direct child asset is updated"
        childAsset.setName("gatewayHandlerTestChildAssetUpdated")
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
        //endregion

        //region Test: Receive delete on asset direct children events sub
        when: "a mqtt client is subscribed to all asset events of a specific asset's direct children and a direct child asset is deleted"
        assetIdsToRemove.clear()
        assetIdsToRemove.add(childAsset.getId())
        assetStorageService.delete(assetIdsToRemove)

        then: "then the asset event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AssetEvent
            def event = receivedEvents.get(0) as AssetEvent
            assert event.cause == AssetEvent.Cause.DELETE
            def asset = event.getAsset()
            assert asset.getName() == childAsset.getName()
        }
        receivedEvents.clear()
        client.removeAllMessageConsumers();
        //endregion

        //region Test: Sub all attribute events of realm
        when: "a mqtt client subscribes to all attribute events of the realm"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/+/attributes/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        def attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "presenceDetected"
            assert event.value.get() == true
        }
        receivedEvents.clear()
        //endregion

        //region Test: Don't receive asset events on attribute sub
        when: "a mqtt client is subscribed to all attribute events of the realm and an asset is created"
        testAssetThree = new ThingAsset("gatewayHandlerTestAssetThree")
        testAssetThree.setRealm(keycloakTestSetup.realmBuilding.name)
        testAssetThree.setId(UniqueIdentifierGenerator.generateId())
        assetStorageService.merge(testAssetThree)

        then: "then the attribute event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        assetIdsToRemove.clear()
        assetIdsToRemove.add(testAssetThree.getId())
        assetStorageService.delete(assetIdsToRemove) // cleanup, delete asset
        receivedEvents.clear()
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Sub all attribute events of realm's direct children
        when: "a mqtt client subscribes to all attribute events of the realm's direct children"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/+/attributes/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        def asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        assetStorageService.merge(asset1)

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "temp"
            assert event.value.get() == "hello world"
        }
        receivedEvents.clear()
        //endregion

        //region Test: Don't receive non-direct children attribute on direct children events sub
        when: "a mqtt client subscribed to all attribute events of the realm's direct children and an attribute is created that is not a direct descendant"
        def asset2 = assetStorageService.find(managerTestSetup.apartment1HallwayId)
        asset2.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        assetStorageService.merge(asset2)

        then: "then the attribute event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Sub all attribute events with attribute name
        when: "a mqtt client subscribes to all attribute events with a specific attribute name"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/+/attributes/motionSensor/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "motionSensor"
            assert event.value.get() == 80
        }
        receivedEvents.clear()
        //endregion

        //region Test: Don't receive attribute with a different name on event sub with attribute name
        when: "a mqtt client subscribed to all attribute events with a specific name and an attribute is updated with a different name"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Sub all attribute events of realm's direct children with attribute name
        when: "a mqtt client subscribes to all attribute events with a specific name of the realm's direct children"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/+/attributes/temp/+".toString()

        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)

        asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        assetStorageService.merge(asset1)

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "temp"
            assert event.value.get() == "hello world"
        }
        receivedEvents.clear()
        //endregion

        //region Test: Don't receive non-direct children attribute on direct children events sub with attribute name
        when: "a mqtt client subscribed to all attribute events with a specific name of the realm's direct children and an attribute is created that is not a direct descendant"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Sub all attribute events of asset
        when: "a mqtt client subscribes to all attribute events of a specific asset"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/${managerTestSetup.apartment1HallwayId}/attributes".toString()

        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)

        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "presenceDetected"
            assert event.value.get() == true
        }
        receivedEvents.clear()
        //endregion

        //region Test: Don't receive attribute of different asset on asset events sub
        when: "a mqtt client is subscribed to all attribute events of a specific asset and an attribute of a different asset is updated"
        asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        assetStorageService.merge(asset1)

        then: "then the attribute event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Sub all attribute events of asset's descendants with attribute name
        when: "a mqtt client subscribes to all attribute events of a specific asset's descendants with a specific attribute name"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/${managerTestSetup.smartBuildingId}/attributes/motionSensor/#".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)

        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should be received"

        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "motionSensor"
            assert event.value.get() == 80
        }
        receivedEvents.clear()
        //endregion

        //region Test: Don't receive attribute of different asset on asset descendants sub with attribute name
        when: "a mqtt client is subscribed to all attribute events of a specific asset's descendants with a specific attribute name and an attribute of a non-descendant asset is updated"
        asset1 = assetStorageService.find(managerTestSetup.smartBuildingId)
        asset1.addAttributes(new Attribute<>("temp", TEXT, "hello world"))
        assetStorageService.merge(asset1)

        then: "then the attribute event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        //endregion


        //region Test: Don't receive attribute of different attribute on asset descendants sub with attribute name
        when: "a mqtt client is subscribed to all attribute events of a specific asset's descendants with a specific attribute name and an attribute of a descendant asset is updated with a different name"
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "presenceDetected", "true")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Sub all attribute events of asset's direct children with attribute name
        when: "a mqtt client subscribes to all attribute events of a specific asset's direct children with a specific attribute name"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/${managerTestSetup.apartment1Id}/attributes/motionSensor/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }
        client.addMessageConsumer(topic, messageConsumer)

        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should be received"
        conditions.eventually {
            assert receivedEvents.size() == 1
            assert receivedEvents.get(0) instanceof AttributeEvent
            def event = receivedEvents.get(0) as AttributeEvent
            assert event.getName() == "motionSensor"
            assert event.value.get() == 80
        }
        receivedEvents.clear()
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Don't receive attribute of different asset children on asset direct children attribute events sub with attribute name
        when: "a mqtt client is subscribed to all attribute events of a specific asset's direct children with a specific attribute name and an attribute of a non-direct child asset is updated"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/${managerTestSetup.smartBuildingId}/attributes/motionSensor/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()
        //endregion

        //region Test: Don't receive attribute of different attribute on asset direct children attribute events sub with attribute name
        when: "a mqtt client is subscribed to all attribute events of a specific asset's direct children with a specific attribute name and an attribute of a direct child asset is updated with a different name"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.EVENTS_TOPIC/assets/${managerTestSetup.apartment1Id}/attributes/presenceDetected/+".toString()
        messageConsumer = { MQTTMessage<String> msg ->
            receivedEvents.add(ValueUtil.parse(msg.payload, AttributeEvent.class).orElse(null))
        }

        client.addMessageConsumer(topic, messageConsumer)
        attributeEvent = new AttributeEvent(managerTestSetup.apartment1HallwayId, "motionSensor", "80")
        ((SimulatorProtocol) agentService.getProtocolInstance(managerTestSetup.apartment1ServiceAgentId)).updateSensor(attributeEvent)

        then: "then the attribute event should not be received"
        conditions.eventually {
            assert receivedEvents.size() == 0
        }
        client.removeAllMessageConsumers()
        //endregion


        // create the gateway asset so we can test the gateway v2 asset service user connection/behavior
        when: "a gateway v2 asset is created a service user should be created"
        def tempGatewayAsset = new GatewayV2Asset("gatewayV2Asset")
        tempGatewayAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        tempGatewayAsset.setId(UniqueIdentifierGenerator.generateId())
        def gatewayAsset = assetStorageService.merge(tempGatewayAsset)

        then: "then the gateway v2 asset should be created with a service user"
        conditions.eventually {
            assert gatewayAsset != null
            assert gatewayAsset.getClientId().isPresent()
            assert gatewayAsset.getClientSecret().isPresent()
            // assert the service user exists in keycloak
            assert keycloakTestSetup.keycloakProvider.getUserByUsername(keycloakTestSetup.realmBuilding.name,
                    User.SERVICE_ACCOUNT_PREFIX + gatewayAsset.getClientId().get()) != null

        }

        //region Test: Connect to MQTT Broker with Gateway V2 Asset Service User
        def gatewayClientId = gatewayAsset.getClientId().get()
        def gatewayMqttUsername = keycloakTestSetup.realmBuilding.name + ":" + gatewayClientId // realm:clientId
        def gatewayServiceUsername = User.SERVICE_ACCOUNT_PREFIX + gatewayClientId // service-account-clientId
        def gatewayClientSecret = gatewayAsset.getClientSecret().get() // password
        def gatewayUser = keycloakTestSetup.keycloakProvider.getUserByUsername(keycloakTestSetup.realmBuilding.name, gatewayServiceUsername)

        when: "a mqtt client connects to the mqtt broker with gateway service user"
        client.disconnect(); // disconnect the previous client
        client = new MQTT_IOClient(gatewayClientId, mqttHost, mqttPort, false, true,
                new UsernamePassword(gatewayMqttUsername, gatewayClientSecret), null, null)
        client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(gatewayUser.getId()).size() == 1
            def connection = mqttBrokerService.getUserConnections(gatewayUser.getId())[0]
            assert clientEventService.sessionKeyInfoMap.containsKey(getConnectionIDString(connection))
        }
        //endregion

        //TODO: Gateway V2 Asset Service User Tests, filters and operations should be 'relative to the gateway' and not 'relative to the realm


        cleanup: "disconnect the clients"
        if (client != null) {
            client.disconnect()
        }
        if (newClient != null) {
            newClient.disconnect()
        }
    }
}