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
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.mqtt.MQTTResponseMessage
import org.openremote.model.mqtt.MQTTSuccessResponse
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
        List<MQTTResponseMessage> receivedResponses = []
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


        //region Client connection test
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


        //region Asset creation test
        when: "a mqtt client publishes a create asset message"
        def responseIdentifier = UniqueIdentifierGenerator.generateId()
        def topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/$responseIdentifier/$GatewayMQTTHandler.CREATE_TOPIC".toString()
        def assetName = "gatewayHandlerTestCreationAsset"
        def testAsset = new ThingAsset(assetName)
        def payload = ValueUtil.asJSON(testAsset).get()
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the asset should be created with the specified name"
        conditions.eventually {
            assert assetStorageService.find(new AssetQuery().names(assetName)) != null
        }
        //endregion


        //region Asset creation test with response
        when: "a mqtt client publishes a create asset message and subscribes to the response topic"
        responseIdentifier = UniqueIdentifierGenerator.generateId()
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/$responseIdentifier/$GatewayMQTTHandler.CREATE_TOPIC".toString()
        def responseTopic = topic + "/response"
        assetName = "gatewayHandlerTestCreationAssetResponse"
        testAsset = new ThingAsset(assetName)
        payload = ValueUtil.asJSON(testAsset).get()


        Consumer<MQTTMessage<String>> messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, MQTTResponseMessage.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the thing asset should be created and the asset data should be received on the response topic"
        conditions.eventually {
            assert assetStorageService.find(new AssetQuery().names(assetName)) != null
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof MQTTSuccessResponse
            def response = receivedResponses.get(0) as MQTTSuccessResponse
            def asset = response.getData(ThingAsset.class)
            assert asset != null
            assert asset.getName() == assetName
        }
        receivedResponses.clear()
        client.removeAllMessageConsumers()
        //endregion

        //region asset get test with response
        when: "a mqtt client publishes a get asset message and subscribes to the response topic"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/$managerTestSetup.smartBuildingId/get".toString()
        responseTopic = topic + "/response"
        payload = ValueUtil.asJSON(testAsset).get()

        messageConsumer = { MQTTMessage<String> msg ->
            receivedResponses.add(ValueUtil.parse(msg.payload, MQTTResponseMessage.class).orElse(null))
        }
        client.addMessageConsumer(responseTopic, messageConsumer)
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the specified asset should be received on the response topic"
        conditions.eventually {
            assert receivedResponses.size() == 1
            assert receivedResponses.get(0) instanceof MQTTSuccessResponse
            def response = receivedResponses.get(0) as MQTTSuccessResponse
            def asset = response.getData(BuildingAsset.class)
            assert asset != null
            assert asset.getId() == managerTestSetup.smartBuildingId
        }


        //region Asset attribute update test
        when: "a mqtt client publishes an update to an asset attribute"
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${managerTestSetup.apartment1HallwayId}/attributes/motionSensor/update".toString()
        payload = "70"
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "the value of the attribute should be updated accordingly"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 70d
        }
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