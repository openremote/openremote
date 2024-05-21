package org.openremote.test.mqtt

import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
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
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.query.AssetQuery
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

        //region Test: Connect to MQTT Broker
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

        //region Test: Create Asset
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

        //region Test: Create Asset with Response
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

        //region Test: Update Asset Data
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


        //region Test: Update Asset Data with Response
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

        
        //region Test: Delete Asset
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

        //region Test: Delete Asset with Response
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


        //region Test: Retrieve Asset Data
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


        //region Test: Update Asset Attribute
        when: "a mqtt client publishes an update message for a specific asset and attribute"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${managerTestSetup.apartment1HallwayId}/attributes/motionSensor/update".toString()
        payload = "60"
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the attribute should be updated accordingly"
        conditions.eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 60d
        }
        //endregion

        //region Test: Update Asset Attribute with Response
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
        
        //region Test: Retrieve Attribute Value
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

        //region Test: Update Multiple Asset Attributes
        //endregion

        cleanup: "disconnect the clients"
        if (client != null) {
            client.disconnect()
        }
        if (newClient != null) {
            newClient.disconnect()
        }
    }
}