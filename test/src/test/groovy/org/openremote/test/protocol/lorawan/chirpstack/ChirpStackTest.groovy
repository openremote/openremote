/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test.protocol.lorawan.chirpstack

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig
import org.openremote.agent.protocol.lorawan.chirpstack.ChirpStackAgent
import org.openremote.agent.protocol.lorawan.chirpstack.ChirpStackProtocol
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
import java.util.concurrent.TimeUnit

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.*
import static org.openremote.setup.integration.model.asset.ChirpStackTestAsset.*

class ChirpStackTest extends Specification implements ManagerContainerTrait{

    static final String APPLICATION_ID = "807036d9-9d96-4305-82c9-92f681a11908"
    static final String CLIENT_ID = "chirpstackAgentClientId"
    static final String DEV_EUI_1 = "1111111111111111"
    static final String DEV_EUI_2 = "2222222222222222"
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
    }

    def cleanupSpec() {
        mqttClient?.disconnect()
        mqttBroker?.stopServer()
    }

    def "ChirpStack Integration Test"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "a ChirpStack agent is created"
        def agent = new ChirpStackAgent("ChirpStackAgent")
        agent.setRealm(MASTER_REALM)
        agent.setHost(HOST)
        agent.setMqttPort(mqttBrokerPort)
        agent.setClientId(CLIENT_ID)
        agent.setApplicationId(APPLICATION_ID)
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((ChirpStackProtocol)agentService.getProtocolInstance(agent.id)) != null
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
            ${DEV_EUI_1},${ASSET_NAME_1},ChirpStackTestAsset,${VENDOR_ID},${MODEL_ID},${FIRMWARE_VERSION}
            ${DEV_EUI_2},${ASSET_NAME_2},ChirpStackTestAsset,${VENDOR_ID},${MODEL_ID},${FIRMWARE_VERSION}
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
            fPort: UPLINK_PORT,
            object: [
                Temperature: TEMPERATURE_VALUE,
                Humidity: HUMIDITY_VALUE
            ]
        ]
        mqttClient.publishWith()
            .topic("application/${APPLICATION_ID}/device/${DEV_EUI_1}/event/up")
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
            .topicFilter("application/${APPLICATION_ID}/device/${DEV_EUI_1}/command/down")
            .callback { Mqtt3Publish publish ->
                downlinkMessages.add(new String(publish.payloadAsBytes))
            }.send()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(asset1.id, SWITCH, true))

        then: "a LoRaWAN downlink message should have been published"
        conditions.eventually {
            assert downlinkMessages.size() == 1
            def downlinkMessage = new JsonSlurper().parseText(downlinkMessages.get(0))
            assert downlinkMessage != null
            assert downlinkMessage.devEui == DEV_EUI_1
            assert downlinkMessage.fPort == DOWNLINK_PORT
            assert downlinkMessage.data == "DAE="
        }
    }
}
