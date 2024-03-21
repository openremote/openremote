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
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.util.ValueUtil
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getInteger
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.mqtt.MQTTBrokerService.*

class MqttGatewayHandlerTest extends Specification implements ManagerContainerTrait {

    def "Mqtt gateway handler test"() {
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

        when: "a mqtt client publishes a message to the create asset topic"
        def responseIdentifier = UniqueIdentifierGenerator.generateId()
        //<realm>/<clientId>/operations/assets/<responseIdentifier>create
        def topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/$responseIdentifier/$GatewayMQTTHandler.CREATE_TOPIC".toString()
        def assetName = "gatewayHandlerTestCreationAsset"
        def testAsset = new ThingAsset(assetName)
        def payload = ValueUtil.asJSON(testAsset).get()
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "The asset should be created"
        conditions.eventually {
            assert assetStorageService.find(new AssetQuery().names(assetName)) != null
        }

        when: "a mqtt client publishes to an asset attribute"
        //<realm>/<clientId>/operations/assets/<assetId>/attributes/<attributeName>/update
        topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$GatewayMQTTHandler.OPERATIONS_TOPIC/assets/${managerTestSetup.apartment1HallwayId}/attributes/motionSensor/update".toString()
        payload = "70"
        client.sendMessage(new MQTTMessage<String>(topic, payload))

        then: "The value of the attribute should be updated"
        new PollingConditions(initialDelay: 1, timeout: 10, delay: 1).eventually {
            assert assetStorageService.find(managerTestSetup.apartment1HallwayId).getAttribute("motionSensor").get().value.orElse(0) == 70d
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