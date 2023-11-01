package org.openremote.test.protocol.mqtt


import org.openremote.agent.protocol.mqtt.MQTT_IOClient
import org.openremote.container.Container
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.mqtt.TeltonikaMQTTHandler
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class TeltonikaMQTTProtocolTest extends Specification implements ManagerContainerTrait {
    @Shared public def conditions = new PollingConditions(timeout: 5, delay: 0.2)

    public static Container container
    public static AssetStorageService assetStorageService;
    public static AgentService agentService;
    public static MQTTBrokerService mqttBrokerService
    public static ManagerTestSetup managerTestSetup;
    public static KeycloakTestSetup keycloakTestSetup;
    public static ClientEventService clientEventService;
    public static String mqttHost;
    public static int mqttPort;
    public static String mqttClientId;
    public static String username;
    public static String password;
    public static MQTT_IOClient client;
    public static TeltonikaMQTTHandler handler;

    //TODO: I do not know how/if to link this to environment variable
    String TELTONIKA_DEVICE_RECEIVE_TOPIC = "data"
    String TELTONIKA_DEVICE_SEND_TOPIC = "commands";
    String TELTONIKA_DEVICE_TOKEN = "teltonika";
    String TELTONIKA_DEVICE_SEND_COMMAND_ATTRIBUTE_NAME = "sendToDevice";
    String TELTONIKA_DEVICE_RECEIVE_COMMAND_ATTRIBUTE_NAME = "response";
    //Real IMEI: https://www.imei.info/?imei=358491098808487
    def TELTONIKA_DEVICE_IMEI = "358491098808487";

    def setupSpec() {
        and: "the container is started"
        container = startContainer(defaultConfig(), defaultServices());
        assetStorageService = container.getService(AssetStorageService.class)
        agentService = container.getService(AgentService.class)
        mqttBrokerService = container.getService(MQTTBrokerService.class)
        managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        clientEventService = container.getService(ClientEventService.class)
        handler = mqttBrokerService.customHandlers.find {it instanceof TeltonikaMQTTHandler} as TeltonikaMQTTHandler

        mqttHost = mqttBrokerService.host
        mqttPort = mqttBrokerService.port

        mqttClientId = UniqueIdentifierGenerator.generateId()
        username = keycloakTestSetup.realmBuilding.name + ":" + keycloakTestSetup.serviceUser.username // realm and OAuth client id
        password = keycloakTestSetup.serviceUser.secret


        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, false,  null, null, null)
    }

    def setup(){
//        I think that it is required to provide usernamePassword to connect the client to the broker. Look at AbstractMQTT_IOClient:L100.
        client.removeAllMessageConsumers();
        client.disconnect();

        client = new MQTT_IOClient(mqttClientId, mqttHost, mqttPort, false, false, null, null, null)
    }

    def cleanupSpec(){
        getLOG().debug("cleanupSpec")
        container.stop();
        container = null;
//        this.container.stop()
        assetStorageService = null;
        agentService = null;
        mqttBrokerService = null;
        managerTestSetup = null;
        keycloakTestSetup = null;
        clientEventService = null;
        mqttHost = null;
        mqttPort = 1883;
    }
    def "the device connects to the MQTT broker"() {

        when: "the device connects to the MQTT broker"
        client.connect()


        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            def connection = mqttBrokerService.getConnectionFromClientID(mqttClientId)
            assert connection != null
        }


        cleanup: "disconnect client from broker"
        client.disconnect()
    }

    def "the device connects to the correct data topic" () {
        when: "client connects to the MQTT broker and to the correct data topic"

        String dataTopic = "${keycloakTestSetup.realmBuilding.name}/${mqttClientId}/${TELTONIKA_DEVICE_TOKEN}/${TELTONIKA_DEVICE_IMEI}/${TELTONIKA_DEVICE_RECEIVE_TOPIC}".toString();
        client.connect();
        client.addMessageConsumer(dataTopic, { msg -> getLOG().print(msg.toString())});

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            def connection = mqttBrokerService.getConnectionFromClientID(mqttClientId)
            assert connection != null
        }

        and: "There should be a subscription handled by TeltonikaMQTTHandler"
        conditions.eventually {
//            FOR SOME REASON, MQTTBrokerService.java:252 considers this connection internal,
//            so it returns void without going through with the subscription *??????????*
//            I think that client.cleanSession is what allows this to not be internal
            assert handler.connectionSubscriberInfoMap.containsKey(getTELTONIKA_DEVICE_IMEI());
        }

        cleanup: "disconnect client from broker"
        client.disconnect()
        client.removeAllMessageConsumers()
    }

    def "the device connects to the MQTT broker to a data topic without TELTONIKA_DEVICE_TOKEN"() {


        when: "the device connects to the MQTT broker to a data topic without TELTONIKA_DEVICE_TOKEN"

        def incorrectDataTopic1 = "${keycloakTestSetup.realmBuilding.name}/${mqttClientId}/${TELTONIKA_DEVICE_IMEI}/${TELTONIKA_DEVICE_RECEIVE_TOPIC}".toString();

        client.connect()
        client.addMessageConsumer(incorrectDataTopic1, { _ -> return });
        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            def connection = mqttBrokerService.getConnectionFromClientID(mqttClientId)
            assert connection != null
        }

        then: "A subscription should not exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(incorrectDataTopic1) == null // Consumer added and removed on failure
            assert !handler.connectionSubscriberInfoMap.containsKey(getTELTONIKA_DEVICE_IMEI());
        }

        cleanup: "disconnect client from broker"
        client.disconnect()
        client.removeAllMessageConsumers()

    }

    def "the device connects to the MQTT broker to a data topic without an IMEI"() {


        when: "the device connects to the MQTT broker to a data topic without an IMEI"

        def incorrectDataTopic2 = "${keycloakTestSetup.realmBuilding.name}/${mqttClientId}/${TELTONIKA_DEVICE_TOKEN}/${TELTONIKA_DEVICE_RECEIVE_TOPIC}".toString();
        client.connect();
        client.addMessageConsumer(incorrectDataTopic2, { _ -> return });

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            def connection = mqttBrokerService.getConnectionFromClientID(mqttClientId)
            assert connection != null
        }

        then: "A subscription should not exist"
        // This works because I am expecting either "data" or "command" on the 5th token, but the 5th token does not exist in the Topic
        conditions.eventually {
            assert !handler.connectionSubscriberInfoMap.containsKey(getTELTONIKA_DEVICE_IMEI());
            assert client.topicConsumerMap.get(incorrectDataTopic2) == null // Consumer added and removed on failure
        }
        cleanup: "disconnect client from broker"
        client.disconnect()
        client.removeAllMessageConsumers()
    }

    def "the device connects to the MQTT broker to a data topic without a RX or TX endpoint"() {

        when: "the device connects to the MQTT broker to a data topic without a RX or TX endpoint"

        def incorrectDataTopic3 = "${keycloakTestSetup.realmBuilding.name}/${mqttClientId}/${TELTONIKA_DEVICE_TOKEN}/${TELTONIKA_DEVICE_IMEI}".toString();

        client.connect();
        client.addMessageConsumer(incorrectDataTopic3, { _ -> return });

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            def connection = mqttBrokerService.getConnectionFromClientID(mqttClientId)
            assert connection != null
        }

        then: "A subscription should not exist"
        // This works because I am expecting either "data" or "command" on the 5th token, but the 5th token does not exist in the Topic
        conditions.eventually {
            assert client.topicConsumerMap.get(incorrectDataTopic3) == null // Consumer added and removed on failure
            assert !handler.connectionSubscriberInfoMap.containsKey(getTELTONIKA_DEVICE_IMEI());
        }

        cleanup: "disconnect client from broker"
        client.disconnect()
        client.removeAllMessageConsumers();
    }

    def "the device connects to the broker with a valid data topic"() {
        when: "the device connects to the MQTT broker to a data topic with a RX endpoint"

        def correctTopic1 = "${keycloakTestSetup.realmBuilding.name}/${mqttClientId}/${TELTONIKA_DEVICE_TOKEN}/${TELTONIKA_DEVICE_IMEI}/${TELTONIKA_DEVICE_RECEIVE_TOPIC}".toString();

        client.connect();
        client.addMessageConsumer(correctTopic1, { _ -> return });

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            def connection = mqttBrokerService.getConnectionFromClientID(mqttClientId)
            assert connection != null
        }

        then: "A subscription should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(correctTopic1) != null
            assert handler.connectionSubscriberInfoMap.containsKey(getTELTONIKA_DEVICE_IMEI());
        }

        cleanup: "disconnect client from broker"
        client.disconnect()
        client.removeAllMessageConsumers();
    }

    def "the device connects to the broker with valid data and command topics"() {
        when: "the device connects to the MQTT broker to a data topic with a valid data topic"

        def correctTopicData = "${keycloakTestSetup.realmBuilding.name}/${mqttClientId}/${TELTONIKA_DEVICE_TOKEN}/${TELTONIKA_DEVICE_IMEI}/${TELTONIKA_DEVICE_RECEIVE_TOPIC}".toString();
        def correctTopicCommands = "${keycloakTestSetup.realmBuilding.name}/${mqttClientId}/${TELTONIKA_DEVICE_TOKEN}/${TELTONIKA_DEVICE_IMEI}/${TELTONIKA_DEVICE_SEND_TOPIC}".toString();

        client.connect();
        client.addMessageConsumer(correctTopicData, { _ -> return });
        client.addMessageConsumer(correctTopicCommands, { _ -> return });

        then: "mqtt connection should exist"
        conditions.eventually {
            assert client.getConnectionStatus() == ConnectionStatus.CONNECTED
            def connection = mqttBrokerService.getConnectionFromClientID(mqttClientId)
            assert connection != null
        }

        then: "Two subscriptions should exist"
        conditions.eventually {
            assert client.topicConsumerMap.get(correctTopicData) != null
            assert client.topicConsumerMap.get(correctTopicCommands) != null
            assert handler.connectionSubscriberInfoMap.size() == 1;
            assert handler.connectionSubscriberInfoMap.containsKey(getTELTONIKA_DEVICE_IMEI());

        }

        cleanup: "disconnect client from broker"
        client.disconnect()
        client.removeAllMessageConsumers();
    }

}
//    def "Check Bidirectional communication between OpenRemote MQTT client and Device"(){
//        assert true;
//    }
//
//    def "Verify Correct Parsing of Teltonika Payload into OpenRemote Attributes"() {
//        assert true;
//    }
