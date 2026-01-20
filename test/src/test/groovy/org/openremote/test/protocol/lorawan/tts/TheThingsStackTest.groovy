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
package org.openremote.test.protocol.lorawan.tts

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.grpc.ServerBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
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
import org.openremote.model.query.AssetQuery
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import ttn.lorawan.v3.*

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

import static org.openremote.agent.protocol.lorawan.LoRaWANConstants.ATTRIBUTE_NAME_DEV_EUI
import static org.openremote.agent.protocol.lorawan.tts.TheThingsStackProtocol.THE_THINGS_STACK_ASSET_TYPE_TAG
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.util.MapAccess.getString
import static org.openremote.setup.integration.model.asset.TheThingsStackTestAsset.*
import static ttn.lorawan.v3.EndDeviceOuterClass.*
import static ttn.lorawan.v3.EventsOuterClass.Event
import static ttn.lorawan.v3.EventsOuterClass.StreamEventsRequest
import static ttn.lorawan.v3.Identifiers.*

class TheThingsStackTest extends Specification implements ManagerContainerTrait {

    static final String TENANT_ID = "ttn"
    static final String API_KEY = "NNSXS.1234"
    static final String APPLICATION_ID = "test-app-id"
    static final String CLIENT_ID = "ttsAgentClientId"
    static final String DEV_EUI_1 = "1111111111111111"
    static final String DEV_EUI_2 = "2222222222222222"
    static final String DEVICE_ID_1 = "device_id_1"
    static final String DEVICE_ID_2 = "device_id_2"
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
    int grpcPort

    @Shared
    TheThingsStackGrpcServer grpcServer

    @Shared
    Mqtt3AsyncClient mqttClient

    def setupSpec() {
        mqttBrokerPort = findEphemeralPort()
        def props = new Properties()
        props.setProperty('port', mqttBrokerPort.toString())
        props.setProperty('persistence_enabled', 'false')
        def config = new MemoryConfig(props)
        mqttBroker = new Server()
        mqttBroker.startServer(config)

        grpcPort = findEphemeralPort()
        grpcServer = new TheThingsStackGrpcServer(grpcPort)
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
    }

    def cleanup() {
        grpcServer?.shutdownEventStream()
    }

    def "TheThingsStack CSV Import Test"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "a TheThingsStack agent is created"
        def agent = new TheThingsStackAgent("TheThingsStackAgent")
        agent.setRealm(MASTER_REALM)
        agent.setHost(HOST)
        agent.setPort(grpcPort)
        agent.setSecureGRPC(false)
        agent.setMqttHost(HOST)
        agent.setMqttPort(mqttBrokerPort)
        agent.setClientId(CLIENT_ID)
        agent.setApplicationId(APPLICATION_ID)
        agent.setTenantId(TENANT_ID)
        agent.setApiKey(API_KEY)
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((TheThingsStackProtocol)agentService.getProtocolInstance(agent.id)) != null
        }

        and: "the gRPC event stream connection should have been established"
        conditions.eventually {
            assert grpcServer.getEventObserverCount() == 1
        }

        when: "the initial gRPC event has been received"
        def streamStartEvent = TheThingsStackGrpcServer.createStreamStartEvent(APPLICATION_ID)
        grpcServer.pushEvent(streamStartEvent)

        then: "the connection status should be CONNECTED"
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
            ${DEV_EUI_1},${ASSET_NAME_1},TheThingsStackTestAsset,${VENDOR_ID},${MODEL_ID},${FIRMWARE_VERSION}
            ${DEV_EUI_2},${ASSET_NAME_2},TheThingsStackTestAsset,${VENDOR_ID},${MODEL_ID},${FIRMWARE_VERSION}
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
            uplink_message: [
                f_port: UPLINK_PORT,
                decoded_payload: [
                    Temperature: TEMPERATURE_VALUE,
                    Humidity: HUMIDITY_VALUE
                ]
            ]
        ]
        mqttClient.publishWith()
            .topic("v3/${APPLICATION_ID}@${TENANT_ID}/devices/${DEVICE_ID_1}/up")
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
            .topicFilter("v3/${APPLICATION_ID}@${TENANT_ID}/devices/${DEVICE_ID_1}/down/push")
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

    def "TheThingsStack Auto-Discovery Test"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "a TheThingsStack agent is created"
        def agent = new TheThingsStackAgent("TheThingsStackAgent")
        agent.setRealm(MASTER_REALM)
        agent.setHost(HOST)
        agent.setPort(grpcPort)
        agent.setSecureGRPC(false)
        agent.setMqttHost(HOST)
        agent.setMqttPort(mqttBrokerPort)
        agent.setClientId(CLIENT_ID)
        agent.setApplicationId(APPLICATION_ID)
        agent.setTenantId(TENANT_ID)
        agent.setApiKey(API_KEY)
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((TheThingsStackProtocol)agentService.getProtocolInstance(agent.id)) != null
        }

        and: "the gRPC event stream connection should have been established"
        conditions.eventually {
            assert grpcServer.getEventObserverCount() == 1
        }

        when: "the initial gRPC event has been received"
        def streamStartEvent = TheThingsStackGrpcServer.createStreamStartEvent(APPLICATION_ID)
        grpcServer.pushEvent(streamStartEvent)

        then: "the connection status should be CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.id)
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "a sensor device sends data for the first time"
        def uplinkEvent = TheThingsStackGrpcServer.createUplinkEvent(APPLICATION_ID, DEVICE_ID_1, DEV_EUI_1, "010A", 1, 101)
        grpcServer.pushEvent(uplinkEvent)

        then: "a new asset should have been auto-discovered"
        conditions.eventually {
            def asset = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_1.toUpperCase()))
            assert asset != null
        }

        when: "a device is created"
        def createEvent = TheThingsStackGrpcServer.createEndDeviceCreateEvent(APPLICATION_ID, DEVICE_ID_2, DEV_EUI_2)
        grpcServer.pushEvent(createEvent)

        then: "a new asset should have been auto-discovered"
        conditions.eventually {
            def asset = assetStorageService.find(new AssetQuery().attributeValue(ATTRIBUTE_NAME_DEV_EUI, DEV_EUI_2.toUpperCase()))
            assert asset != null
        }

        when: "a device sends a LoRaWAN uplink message"
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
            .topic("v3/${APPLICATION_ID}@${TENANT_ID}/devices/${DEVICE_ID_1}/up")
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
            .topicFilter("v3/${APPLICATION_ID}@${TENANT_ID}/devices/${DEVICE_ID_1}/down/push")
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

    static class TheThingsStackGrpcServer {
        final int port
        final EventsService eventsService
        io.grpc.Server server

        TheThingsStackGrpcServer(int port) {
            this.port = port
            this.eventsService = new EventsService()
        }

        void start() {
            server = ServerBuilder.forPort(port)
                .addService(new DeviceService())
                .addService(eventsService)
                .build()
                .start()
        }

        void stop() {
            if (server != null) {
                server.shutdownNow()
            }
        }

        void pushEvent(Event event) {
            eventsService.pushEvent(event)
        }

        int getEventObserverCount() {
            eventsService.eventObservers.size()
        }

        void shutdownEventStream() {
            eventsService.completeAll()
        }

        private static class DeviceService extends EndDeviceRegistryGrpc.EndDeviceRegistryImplBase {

            @Override
            void list(ListEndDevicesRequest request, StreamObserver<EndDevices> responseObserver) {
                EndDeviceIdentifiers identifiers1 = EndDeviceIdentifiers.newBuilder()
                    .setApplicationIds(ApplicationIdentifiers.newBuilder().setApplicationId(APPLICATION_ID))
                    .setDeviceId(DEVICE_ID_1)
                    .setDevEui(ByteString.copyFrom(DEV_EUI_1.decodeHex()))
                    .build()

                EndDevice device1 = EndDevice.newBuilder()
                    .setIds(identifiers1)
                    .setName("Device 1")
                    .putAttributes(THE_THINGS_STACK_ASSET_TYPE_TAG, "TheThingsStackTestAsset")
                    .build()

                EndDeviceIdentifiers identifiers2 = EndDeviceIdentifiers.newBuilder()
                    .setApplicationIds(ApplicationIdentifiers.newBuilder().setApplicationId(APPLICATION_ID))
                    .setDeviceId(DEVICE_ID_2)
                    .setDevEui(ByteString.copyFrom(DEV_EUI_2.decodeHex()))
                    .build()

                EndDevice device2 = EndDevice.newBuilder()
                    .setIds(identifiers2)
                    .setName("Device 2")
                    .putAttributes(THE_THINGS_STACK_ASSET_TYPE_TAG, "TheThingsStackTestAsset")
                    .build()

                EndDevices response = EndDevices.newBuilder()
                    .addEndDevices(device1)
                    .addEndDevices(device2)
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            }

            @Override
            void get(GetEndDeviceRequest request, StreamObserver<EndDevice> responseObserver) {
                def deviceId = request.getEndDeviceIds().getDeviceId()

                if (deviceId == DEVICE_ID_1) {
                    EndDeviceIdentifiers identifiers1 = EndDeviceIdentifiers.newBuilder()
                        .setApplicationIds(ApplicationIdentifiers.newBuilder().setApplicationId(APPLICATION_ID))
                        .setDeviceId(DEVICE_ID_1)
                        .setDevEui(ByteString.copyFrom(DEV_EUI_1.decodeHex()))
                        .build()

                    EndDevice device1 = EndDevice.newBuilder()
                        .setIds(identifiers1)
                        .setName("Device 1")
                        .putAttributes(THE_THINGS_STACK_ASSET_TYPE_TAG, "TheThingsStackTestAsset")
                        .build()

                    responseObserver.onNext(device1)
                    responseObserver.onCompleted()
                } else if (deviceId == DEVICE_ID_2) {
                    EndDeviceIdentifiers identifiers2 = EndDeviceIdentifiers.newBuilder()
                        .setApplicationIds(ApplicationIdentifiers.newBuilder().setApplicationId(APPLICATION_ID))
                        .setDeviceId(DEVICE_ID_2)
                        .setDevEui(ByteString.copyFrom(DEV_EUI_2.decodeHex()))
                        .build()

                    EndDevice device2 = EndDevice.newBuilder()
                        .setIds(identifiers2)
                        .setName("Device 2")
                        .putAttributes(THE_THINGS_STACK_ASSET_TYPE_TAG, "TheThingsStackTestAsset")
                        .build()

                    responseObserver.onNext(device2)
                    responseObserver.onCompleted()
                } else {
                    responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND))
                }
            }
        }

        private static class EventsService extends EventsGrpc.EventsImplBase {
            private final List<StreamObserver<Event>> eventObservers = new CopyOnWriteArrayList<>()

            @Override
            void stream(StreamEventsRequest request, StreamObserver<Event> responseObserver) {
                eventObservers.add(responseObserver)
            }

            void pushEvent(Event event) {
                eventObservers.removeIf { observer ->
                    try {
                        observer.onNext(event)
                        return false
                    } catch (Exception e) {
                        return true
                    }
                }
            }

            void completeAll() {
                eventObservers.each {it.onCompleted()}
                eventObservers.clear()
            }
        }

        static Event createStreamStartEvent(String applicationId) {
            def applicationIds = ApplicationIdentifiers.newBuilder()
                .setApplicationId(applicationId)
                .build()

            def entityIds = EntityIdentifiers.newBuilder()
                .setApplicationIds(applicationIds)
                .build()

            def anyPayload = Any.pack(com.google.protobuf.Empty.getDefaultInstance())

            return Event.newBuilder()
                .setName("events.stream.start")
                .addIdentifiers(entityIds)
                .setData(anyPayload)
                .build()
        }

        static Event createUplinkEvent(String applicationId, String deviceId, String devEui, String payloadData, int fPort, int fCnt) {
            def devEuiBytes = ByteString.copyFrom(devEui.decodeHex())
            def frmPayloadBytes = ByteString.copyFrom(payloadData.decodeHex())
            def uniqueCorrelationId = "mock-corr-id-${System.currentTimeMillis()}-${(int)(Math.random() * 1000)}"

            def applicationIds = ApplicationIdentifiers.newBuilder()
                .setApplicationId(applicationId)
                .build()

            def endDeviceIds = EndDeviceIdentifiers.newBuilder()
                .setApplicationIds(applicationIds)
                .setDeviceId(deviceId)
                .setDevEui(devEuiBytes)
                .build()

            def entityIds = EntityIdentifiers.newBuilder()
                .setDeviceIds(endDeviceIds)
                .build()

            def loraDataRate = Lorawan.LoRaDataRate.newBuilder()
                .setBandwidth(125000)
                .setSpreadingFactor(7)
                .build()

            def dataRate = Lorawan.DataRate.newBuilder()
                .setLora(loraDataRate)
                .build()

            def txSettings = Lorawan.TxSettings.newBuilder()
                .setDataRate(dataRate)
                .setFrequency(868100000)
                .build()

            def rxMetadata = Metadata.RxMetadata.newBuilder()
                .setGatewayIds(GatewayIdentifiers.newBuilder().setGatewayId("mock-gateway-1"))
                .setRssi(-50)
                .setSnr(10)
                .build()

            def applicationUplink = Messages.ApplicationUplink.newBuilder()
                .setFPort(fPort)
                .setFCnt(fCnt)
                .setFrmPayload(frmPayloadBytes)
                .setSettings(txSettings)
                .addRxMetadata(rxMetadata)
                .setReceivedAt(Timestamp.newBuilder().setSeconds((System.currentTimeMillis() / 1000) as long))
                .setConsumedAirtime(Duration.newBuilder().setNanos(50000000))
                .setConfirmed(false)
                .build()

            def applicationUp = Messages.ApplicationUp.newBuilder()
                .setEndDeviceIds(endDeviceIds)
                .setUplinkMessage(applicationUplink)
                .addCorrelationIds(uniqueCorrelationId)
                .build()

            def anyPayload = Any.pack(applicationUp)

            return Event.newBuilder()
                .setName("as.up.data.forward")
                .addCorrelationIds(uniqueCorrelationId)
                .addIdentifiers(entityIds)
                .setData(anyPayload)
                .build()
        }

        def static Event createEndDeviceCreateEvent(String applicationId, String deviceId, String devEui) {
            def devEuiBytes = ByteString.copyFrom(devEui.decodeHex())

            def applicationIds = ApplicationIdentifiers.newBuilder()
                .setApplicationId(applicationId)
                .build()

            def endDeviceIds = EndDeviceIdentifiers.newBuilder()
                .setApplicationIds(applicationIds)
                .setDeviceId(deviceId)
                .setDevEui(devEuiBytes)
                .build()

            def entityIds = EntityIdentifiers.newBuilder()
                .setDeviceIds(endDeviceIds)
                .build()

            def endDevice = EndDevice.newBuilder()
                .setIds(endDeviceIds)
                .build()

            def anyPayload = Any.pack(endDevice)

            return Event.newBuilder()
                .setName("end_device.create")
                .addIdentifiers(entityIds)
                .setData(anyPayload)
                .build()
        }
    }
}
