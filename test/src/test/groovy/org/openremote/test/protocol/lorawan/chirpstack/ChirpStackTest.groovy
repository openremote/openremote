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

import io.chirpstack.api.DeviceListItem
import io.chirpstack.api.DeviceProfile
import io.chirpstack.api.DeviceProfileServiceGrpc
import io.chirpstack.api.DeviceServiceGrpc
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.chirpstack.api.GetDeviceProfileRequest
import io.chirpstack.api.GetDeviceProfileResponse
import io.chirpstack.api.ListDevicesRequest
import io.chirpstack.api.ListDevicesResponse
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import io.grpc.Status;
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
import org.openremote.model.query.AssetQuery
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

import static org.openremote.agent.protocol.lorawan.LoRaWANConstants.ATTRIBUTE_NAME_DEV_EUI
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.util.MapAccess.getString
import static org.openremote.model.Constants.*
import static org.openremote.setup.integration.model.asset.ChirpStackTestAsset.*

class ChirpStackTest extends Specification implements ManagerContainerTrait {

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
    static final String PROFILE_ID = "4711"
    static final String API_KEY = "123456789ABCDEFG"

    @Shared
    int mqttBrokerPort

    @Shared
    Server mqttBroker

    @Shared
    Path tempDataDir

    @Shared
    int grpcPort

    @Shared
    ChirpStackGrpcServer grpcServer

    @Shared
    Mqtt3AsyncClient mqttClient

    def setupSpec() {
        tempDataDir = File.createTempDir('moquette_data', '').toPath()
        mqttBrokerPort = findEphemeralPort()
        def props = new Properties()
        props.setProperty('port', mqttBrokerPort.toString())
        // Moquette assumes linux paths so need to convert windows paths
        props.setProperty('persistent_store', tempDataDir.resolve('moquette_store.mapdb').toString().replace('\\', '/'))
        def config = new MemoryConfig(props)
        mqttBroker = new Server()
        mqttBroker.startServer(config)

        grpcPort = findEphemeralPort()
        grpcServer = new ChirpStackGrpcServer(grpcPort)
        grpcServer.start()

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
        grpcServer?.stop()
        tempDataDir?.deleteDir()
    }

    def "ChirpStack CSV Import Test"() {
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
        agent.setMqttHost(HOST)
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
        conditions.eventually {
            assert assets != null
            assert assets.length == 2
            def node1 = assets.find { it.asset.getAttribute(DEV_EUI).map { DEV_EUI_1.equalsIgnoreCase(it.value.get()) }.orElse(false) }
            def node2 = assets.find { it.asset.getAttribute(DEV_EUI).map { DEV_EUI_2.equalsIgnoreCase(it.value.get()) }.orElse(false) }
            assert node1 != null
            assert node2 != null
            def asset1 = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_1.toUpperCase()))
            def asset2 = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_2.toUpperCase()))
            assert asset1 != null
            assert asset2 != null
        }

        when: "the device sends a LoRaWAN uplink message"
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
            def asset = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_1.toUpperCase()))
            assert asset.getAttribute(TEMPERATURE).flatMap {it.value}.map {it == TEMPERATURE_VALUE}.orElse(false)
            assert asset.getAttribute(RELATIVE_HUMIDITY).flatMap {it.value}.map {it == HUMIDITY_VALUE}.orElse(false)
        }

        when: "an asset attribute value is written"
        def asset1 = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_1.toUpperCase()))
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

    def "ChirpStack Auto Discovery Test"() {
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
        agent.setMqttHost(HOST)
        agent.setMqttPort(mqttBrokerPort)
        agent.setHost(HOST)
        agent.setPort(grpcPort)
        agent.setClientId(CLIENT_ID)
        agent.setApplicationId(APPLICATION_ID)
        agent.setApiKey(API_KEY)
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

        AssetTreeNode[] assets = agentResource.doProtocolAssetDiscovery(null, agent.getId(), null)

        then: "new assets should have been discovered"
        conditions.eventually {
            assert assets != null
            assert assets.length == 2
            def node1 = assets.find { it.asset.getAttribute(DEV_EUI).map { DEV_EUI_1.equalsIgnoreCase(it.value.get()) }.orElse(false) }
            def node2 = assets.find { it.asset.getAttribute(DEV_EUI).map { DEV_EUI_2.equalsIgnoreCase(it.value.get()) }.orElse(false) }
            assert node1 != null
            assert node2 != null
            def asset1 = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_1.toUpperCase()))
            def asset2 = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_2.toUpperCase()))
            assert asset1 != null
            assert asset2 != null
        }

        when: "the device sends a LoRaWAN uplink message"
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
            def asset = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_1.toUpperCase()))
            assert asset.getAttribute(TEMPERATURE).flatMap {it.value}.map {it == TEMPERATURE_VALUE}.orElse(false)
            assert asset.getAttribute(RELATIVE_HUMIDITY).flatMap {it.value}.map {it == HUMIDITY_VALUE}.orElse(false)
        }

        when: "an asset attribute value is written"
        def asset1 = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_1.toUpperCase()))
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

    static class ChirpStackGrpcServer {
        private final int port
        private io.grpc.Server server

        ChirpStackGrpcServer(int port) {
            this.port = port
        }

        void start() {
            server = ServerBuilder.forPort(port)
                .addService(new DeviceService())
                .addService(new DeviceProfileService())
                .build()
                .start()
            println "Fake gRPC server started on port $port"
        }

        void stop() {
            if (server != null) {
                server.shutdownNow()
                println "Fake gRPC server stopped"
            }
        }

        private static class DeviceService extends DeviceServiceGrpc.DeviceServiceImplBase {
            @Override
            void list(ListDevicesRequest request, StreamObserver<ListDevicesResponse> responseObserver) {
                println "Fake DeviceService is called"
                DeviceListItem device1 = DeviceListItem.newBuilder()
                    .setDevEui(DEV_EUI_1)
                    .setName(ASSET_NAME_1)
                    .setDeviceProfileId(PROFILE_ID)
                    .build()
                DeviceListItem device2 = DeviceListItem.newBuilder()
                    .setDevEui(DEV_EUI_2)
                    .setName(ASSET_NAME_2)
                    .setDeviceProfileId(PROFILE_ID)
                    .build()
                ListDevicesResponse response = ListDevicesResponse.newBuilder()
                    .addResult(device1)
                    .addResult(device2)
                    .setTotalCount(2)
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            }
        }

        private static class DeviceProfileService extends DeviceProfileServiceGrpc.DeviceProfileServiceImplBase {
            @Override
            void get(GetDeviceProfileRequest request, StreamObserver<GetDeviceProfileResponse> responseObserver) {
                println "Fake DeviceProfileService is called"
                if (PROFILE_ID != request.id) {
                    responseObserver.onError(
                        Status.NOT_FOUND
                            .withDescription("Device profile not found: ${request.id}")
                            .asRuntimeException()
                    );
                    return;
                }

                DeviceProfile profile = DeviceProfile.newBuilder()
                    .setName("TestProfile")
                    .setId(request.getId())
                    .putTags(ChirpStackProtocol.CHIRPSTACK_ASSET_TYPE_TAG, "ChirpStackTestAsset")
                    .build()

                GetDeviceProfileResponse response = GetDeviceProfileResponse.newBuilder()
                    .setDeviceProfile(profile)
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            }
        }
    }
}
