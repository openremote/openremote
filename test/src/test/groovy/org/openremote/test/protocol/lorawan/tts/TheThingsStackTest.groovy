package org.openremote.test.protocol.lorawan.tts

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig
import org.openremote.agent.protocol.lorawan.tts.TheThingsStackAgent
import org.openremote.agent.protocol.lorawan.tts.TheThingsStackProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.asset.AssetTreeNode
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.AgentResource
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.file.FileInfo
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static org.openremote.agent.protocol.lorawan.tts.TheThingsStackProtocol.THE_THINGS_STACK_TEST
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.*
import static org.openremote.setup.integration.model.asset.TheThingsStackTestAsset.*

class TheThingsStackTest extends Specification implements ManagerContainerTrait{

    static final String TENANT_ID = "ttn"
    static final String API_KEY = "NNSXS.1234"
    static final String APPLICATION_ID = "test-app-id"
    static final String CLIENT_ID = "ttsAgentClientId"
    static final String DEV_EUI_1 = "1111111111111111"
    static final String DEV_EUI_2 = "2222222222222222"
    static final String DEVIDE_ID_1 = "device_id_1"
    static final String DEVIDE_ID_2 = "device_id_2"
    static final String ASSET_NAME_1 = "Test Asset 1"
    static final String ASSET_NAME_2 = "Test Asset 2"
    static final String VENDOR_ID  = "dragino"
    static final String MODEL_ID = "lht65"
    static final String FIRMWARE_VERSION = "1.8"
    static final String HOST = "localhost"
    static final Integer UPLINK_PORT = 2
    static final Integer DOWNLINK_PORT = 4
    static final Double TEMPERATURE_VALUE = 20.4
    static final Double HUMIDITY_VALUE = 56.4

    @Shared
    int ttsServerPort

    @Shared
    HttpServer ttsServer

    @Shared
    int mqttBrokerPort

    @Shared
    Server mqttBroker

    @Shared
    Mqtt3AsyncClient mqttClient

    def setupSpec() {
        mqttBrokerPort = findEphemeralPort()
        def props = new Properties()
        props.setProperty('port', mqttBrokerPort.toString())
        def config = new MemoryConfig(props)
        mqttBroker = new Server()
        mqttBroker.startServer(config)

        mqttClient = MqttClient.builder()
            .useMqttVersion3()
            .identifier("testClientId")
            .serverHost(HOST)
            .serverPort(mqttBrokerPort)
            .buildAsync()
        mqttClient.connect().get(2, TimeUnit.SECONDS)

        ttsServerPort = findEphemeralPort()
        ttsServer = HttpServer.create(new InetSocketAddress(ttsServerPort), 0)
        ttsServer.createContext("/api/v3/applications/${APPLICATION_ID}/devices", new HttpHandler() {
            @Override
            void handle(HttpExchange exchange) throws IOException {
                String responseJson = """
                {
                    "end_devices": [
                        {
                            "ids": {
                                "dev_eui": "${DEV_EUI_1}",
                                "device_id": "${DEVIDE_ID_1}"
                            }
                        },
                        {
                            "ids": {
                                "dev_eui": "${DEV_EUI_2}",
                                "device_id": "${DEVIDE_ID_2}"
                            }
                        }
                    ]
                }
                """
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, responseJson.getBytes().length)
                exchange.responseBody.withCloseable {
                    it.write(responseJson.getBytes())
                }
            }
        })

        ttsServer.setExecutor(Executors.newSingleThreadExecutor())
        ttsServer.start()
    }

    def cleanupSpec() {
        ttsServer?.stop(0)
        mqttClient?.disconnect()
        mqttBroker?.stopServer()
    }

    def "TheThingsStack Integration Test"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        container.getConfig().put(THE_THINGS_STACK_TEST, true.toString())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "a TheThingsStack agent is created"
        def agent = new TheThingsStackAgent("TheThingsStackAgent")
        agent.setRealm(MASTER_REALM)
        agent.setHost(HOST)
        agent.setPort(ttsServerPort)
        agent.setMqttPort(mqttBrokerPort)
        agent.setClientId(CLIENT_ID)
        agent.setApplicationId(APPLICATION_ID)
        agent.setTenantId(TENANT_ID)
        agent.setApiKey(API_KEY)
        def assetTypeMap = new ValueType.StringMap()
        def key = "${VENDOR_ID}::${MODEL_ID}::${FIRMWARE_VERSION}".toString()
        assetTypeMap.putAll([(key): "TheThingsStackTestAsset"])
        agent.setAssetTypeMap(assetTypeMap)
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((TheThingsStackProtocol)agentService.getProtocolInstance(agent.id)) != null
        }

        and: "the connection status should be CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.id)
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "an authenticated admin user"
        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the agent resource"
        def agentResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AgentResource.class)

        and: "CSV import is executed"
        def csvContent = """\
            ${DEV_EUI_1},${ASSET_NAME_1},${VENDOR_ID},${MODEL_ID},${FIRMWARE_VERSION}
            ${DEV_EUI_2},${ASSET_NAME_2},${VENDOR_ID},${MODEL_ID},${FIRMWARE_VERSION}
        """.stripIndent()
        def fileInfo = new FileInfo("devices.csv", csvContent, false)
        AssetTreeNode[] assets = agentResource.doProtocolAssetImport(null, agent.getId(), null, fileInfo)

        then: "new assets should have been imported"
        assert assets != null
        assert assets.length == 2
        def node1 = assets.find {it.asset.getAttribute(DEV_EUI).map {DEV_EUI_1.equalsIgnoreCase(it.value.get())}.orElse(false)}
        def node2 = assets.find {it.asset.getAttribute(DEV_EUI).map {DEV_EUI_2.equalsIgnoreCase(it.value.get())}.orElse(false)}
        assert node1 != null
        assert node2 != null

        when: "the device sends a LoRaWAN uplink message"
        def asset1 = node1.asset
        def json = [
            uplink_message: [
                f_port: UPLINK_PORT,
                decoded_payload: [
                    Temperature: TEMPERATURE_VALUE,
                    Humidity: HUMIDITY_VALUE
                ]
            ]
        ]
        mqttClient.publishWith()
            .topic("v3/${APPLICATION_ID}@${TENANT_ID}/devices/${DEVIDE_ID_1}/up")
            .payload(JsonOutput.toJson(json).bytes)
            .send()

        then: "asset attribute values should be updated"
        conditions.eventually {
            def asset = assetStorageService.find(asset1.id)
            assert asset.getAttribute(TEMPERATURE).flatMap {it.value}.map {it == TEMPERATURE_VALUE}.orElse(false)
            assert asset.getAttribute(RELATIVE_HUMIDITY).flatMap {it.value}.map {it == HUMIDITY_VALUE}.orElse(false)
        }

        when: "an asset attribute value is written"
        def downlinkMessages = new CopyOnWriteArrayList<String>()
        mqttClient.subscribeWith()
            .topicFilter("v3/${APPLICATION_ID}@${TENANT_ID}/devices/${DEVIDE_ID_1}/down/push")
            .callback { Mqtt3Publish publish ->
                downlinkMessages.add(new String(publish.payloadAsBytes))
            }.send()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(asset1.id, SWITCH, true))

        then: "a LoRaWAN downlink message should have been published"
        conditions.eventually {
            assert downlinkMessages.size() == 1
            def downlinkMessage = new JsonSlurper().parseText(downlinkMessages.get(0))
            assert downlinkMessage != null
            assert downlinkMessage.downlinks[0].f_port == DOWNLINK_PORT
            assert downlinkMessage.downlinks[0].frm_payload == "DAE="
        }
    }
}

