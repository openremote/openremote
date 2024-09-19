package org.openremote.test.benchmark

import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3AsyncClientView
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3ClientConfigView
import com.hivemq.client.mqtt.MqttClientConfig
import com.hivemq.client.mqtt.MqttClientConnectionConfig
import io.netty.channel.socket.SocketChannel
import org.openremote.agent.protocol.mqtt.MQTTLastWill
import org.openremote.agent.protocol.mqtt.MQTTMessage
import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.DefaultMQTTHandler
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.mqtt.MQTTHandler
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.auth.UsernamePassword
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.util.ValueUtil
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit
import java.util.function.Consumer

import static org.openremote.container.util.MapAccess.getInteger
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.mqtt.MQTTBrokerService.*
import static org.openremote.model.value.ValueType.TEXT

/**
 * This benchmark is intended to determine the throughput of AttributeEvents over MQTT which is a typical use case.
 * In this simple scenario an MQTT client has subscribed to attributes of an asset and also publishes attribute events
 * for this asset. The time taken for a response to arrive after a publish is a simple measure of the throughput of the
 * system; this can be used to monitor the performance change of the system over time on a given system specification.
 */
class AttributeEventBenchmarkTest extends Specification implements ManagerContainerTrait {

    def "Attribute processing benchmark"() {

        given: "the container environment is started"
        def eventCount = 1000
        def startTime = -1l
        def endTime = -1l
        List<SharedEvent> receivedEvents = []
        List<Object> receivedValues = []
        MQTT_IOClient client = null
        def conditions = new PollingConditions(timeout: 15, initialDelay: 0.1, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def clientEventService = container.getService(ClientEventService.class)
        def agentService = container.getService(AgentService.class)
        def mqttClientId = UniqueIdentifierGenerator.generateId()
        def username = keycloakTestSetup.realmBuilding.name + ":" + keycloakTestSetup.serviceUser.username // realm and OAuth client id
        def password = keycloakTestSetup.serviceUser.secret
        def mqttHost = getString(container.getConfig(), MQTT_SERVER_LISTEN_HOST, "0.0.0.0")
        def mqttPort = getInteger(container.getConfig(), MQTT_SERVER_LISTEN_PORT, 1883)

        when: "a mqtt client connects with valid credentials"
        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, true, new UsernamePassword(username, password), null, null)
        client.connect()

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            assert mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id).size() == 1
            def connection = mqttBrokerService.getUserConnections(keycloakTestSetup.serviceUser.id)[0]
            assert clientEventService.sessionKeyInfoMap.containsKey(getConnectionIDString(connection))
        }

        when: "a mqtt client subscribes to all attributes of an asset"
        def topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_TOPIC/$MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD/$managerTestSetup.apartment1HallwayId".toString()
        Consumer<MQTTMessage<String>> eventConsumer = { msg ->
            def event = ValueUtil.parse(msg.payload, SharedEvent.class)
            receivedEvents.add(event.get())
            if (receivedEvents.size() == eventCount) {
                endTime = System.currentTimeMillis()
            }
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

        when: "Many attribute events are sent by the mqtt client"
        startTime = System.currentTimeMillis()
        for (i in 1..eventCount) {
            topic = "${keycloakTestSetup.realmBuilding.name}/$mqttClientId/$DefaultMQTTHandler.ATTRIBUTE_VALUE_WRITE_TOPIC/motionSensor/${managerTestSetup.apartment1HallwayId}".toString()
            def payload = i.toString()
            client.sendMessage(new MQTTMessage<String>(topic, payload))
        }

        then: "all attribute updates should be received by the client"
        new PollingConditions(timeout: 300, initialDelay: 10, delay: 10).eventually {
            getLOG().info("Events processed = ${receivedEvents.size()}")
            assert noEventProcessedIn(assetProcessingService, 100)
        }

        cleanup: "output processing time"
        getLOG().info("Time taken to process $eventCount events = ${System.currentTimeMillis()-startTime}ms")
        if (client != null) {
            client.disconnect()
        }
    }
}
