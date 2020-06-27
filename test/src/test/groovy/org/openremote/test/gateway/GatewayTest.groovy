package org.openremote.test.gateway

import com.google.common.collect.Lists
import io.netty.channel.ChannelHandler
import org.apache.http.client.utils.URIBuilder
import org.openremote.agent.protocol.http.HttpClientProtocol
import org.openremote.container.web.OAuthClientCredentialsGrant
import org.openremote.agent.protocol.io.AbstractNettyIoClient
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.agent.protocol.websocket.WebsocketIoClient
import org.openremote.container.Container
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.concurrent.ManagerExecutorService
import org.openremote.manager.gateway.GatewayClientService
import org.openremote.manager.gateway.GatewayConnector
import org.openremote.manager.gateway.GatewayService
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.security.ManagerKeycloakIdentityProvider
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.*
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.attribute.*
import org.openremote.model.event.Event
import org.openremote.model.event.shared.EventRequestResponseWrapper
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.gateway.GatewayClientResource
import org.openremote.model.gateway.GatewayConnection
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.IntStream

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.util.TextUtil.isNullOrEmpty

class GatewayTest extends Specification implements ManagerContainerTrait {

    def "Gateway asset provisioning and local manager logic test"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def executorService = container.getService(ManagerExecutorService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def gatewayService = container.getService(GatewayService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def identityProvider = container.getService(ManagerIdentityService.class).identityProvider as ManagerKeycloakIdentityProvider
        def httpClientProtocol = container.getService(HttpClientProtocol.class)

        expect: "the container should be running and initialised"
        conditions.eventually {
            assert container.isRunning()
            assert noEventProcessedIn(assetProcessingService, 300)
        }

        when: "a gateway is provisioned in this manager"
        def gateway = assetStorageService.merge(new Asset("Test gateway", AssetType.GATEWAY, null, managerDemoSetup.realmBuildingTenant))

        then: "a keycloak client should have been created for this gateway"
        conditions.eventually {
            def client = identityProvider.getClient(managerDemoSetup.realmBuildingTenant, GatewayService.GATEWAY_CLIENT_ID_PREFIX + gateway.getId())
            assert client != null
        }

        and: "a set of credentials should have been created for this gateway and be stored against the gateway for easy reference"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId(), true)
            assert gateway.getAttribute("clientId").isPresent()
            assert !isNullOrEmpty(gateway.getAttribute("clientId").flatMap{it.getValueAsString()}.orElse(""))
            assert !isNullOrEmpty(gateway.getAttribute("clientSecret").flatMap{it.getValueAsString()}.orElse(""))
        }

        and: "a gateway connector should have been created for this gateway"
        conditions.eventually {
            assert gatewayService.gatewayConnectorMap.size() == 1
            assert gatewayService.gatewayConnectorMap.get(gateway.getId()).gatewayId == gateway.getId()
        }

        when: "the Gateway client is created"
        def gatewayClient = new WebsocketIoClient<String>(
            new URIBuilder("ws://localhost:$serverPort/websocket/events?Auth-Realm=$managerDemoSetup.realmBuildingTenant").build(),
            null,
            new OAuthClientCredentialsGrant("http://localhost:$serverPort/auth/realms/$managerDemoSetup.realmBuildingTenant/protocol/openid-connect/token",
                gateway.getAttribute("clientId").flatMap{it.getValueAsString()}.orElse(""),
                gateway.getAttribute("clientSecret").flatMap{it.getValueAsString()}.orElse(""),
                null).setBasicAuthHeader(true),
            executorService)
        gatewayClient.setEncoderDecoderProvider({
            [new AbstractNettyIoClient.MessageToMessageDecoder<String>(String.class, gatewayClient)].toArray(new ChannelHandler[0])
        })

        and: "we add callback consumers to the client"
        def connectionStatus = gatewayClient.getConnectionStatus()
        List<String> clientReceivedMessages = []
        gatewayClient.addMessageConsumer({
            message -> clientReceivedMessages.add(message)
        })
        gatewayClient.addConnectionStatusConsumer({
            status -> connectionStatus = status
        })

        and: "the gateway connects to this manager"
        gatewayClient.connect()

        then: "the gateway netty client status should become CONNECTED"
        conditions.eventually {
            assert connectionStatus == ConnectionStatus.CONNECTED
            assert gatewayClient.connectionStatus == ConnectionStatus.CONNECTED
        }

        and: "the gateway asset connection status should be CONNECTING"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getAttribute("status").flatMap{it.getValueAsString()}.orElse(null) == ConnectionStatus.CONNECTING.name()
        }

        and: "the server should have sent a CONNECTED message and an asset read request"
        conditions.eventually {
            assert clientReceivedMessages.size() >= 1
            assert clientReceivedMessages[0].startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            def response = Container.JSON.readValue(clientReceivedMessages[0].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            assert response.messageId == GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL
            def readAssetsEvent = response.event as ReadAssetsEvent
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.select.excludeAttributes
            assert readAssetsEvent.assetQuery.select.excludePath
            assert readAssetsEvent.assetQuery.select.excludeParentInfo
            assert readAssetsEvent.assetQuery.recursive
        }

        when: "the previously received messages are cleared"
        clientReceivedMessages.clear()

        and: "the gateway client assets are defined"
        List<String> agentAssetIds = []
        List<Asset> agentAssets = []
        List<String> assetIds = []
        List<Asset> assets = []

        IntStream.rangeClosed(1, 5).forEach {i ->
            agentAssetIds.add(UniqueIdentifierGenerator.generateId("Test Agent $i"))
            agentAssets.add(
                new Asset(
                    agentAssetIds[i-1],
                    0L,
                    new Date(),
                    "Test Agent $i",
                    AssetType.AGENT.type,
                    false,
                    (String)null,
                    (String)null,
                    (String)null,
                    "master",
                    (String[])[agentAssetIds[i-1]].toArray(new String[0]),
                    null).addAttributes(
                    new AssetAttribute("protocolConfig", AttributeValueType.STRING, Values.create(HttpClientProtocol.PROTOCOL_NAME))
                        .addMeta(
                            new MetaItem(MetaItemType.PROTOCOL_CONFIGURATION),
                            new MetaItem(HttpClientProtocol.META_PROTOCOL_BASE_URI, Values.create("https://google.co.uk")),
                            new MetaItem(HttpClientProtocol.META_PROTOCOL_PING_PATH, Values.create(""))
                        ),
                    new AssetAttribute(AttributeType.SURFACE_AREA, Values.create(1000), new Date().getTime())
                )
            )

            assetIds.add(UniqueIdentifierGenerator.generateId("Test Building $i"))

            // Add assets out of order to test gateway connector re-ordering logic
            IntStream.rangeClosed(1, 4).forEach{j ->

                assetIds.add(UniqueIdentifierGenerator.generateId("Test Building $i Room $j"))
                assets.add(
                    new Asset(
                        assetIds[(i-1)*5+j],
                        0L,
                        new Date(),
                        "Test Building $i Room $j",
                        AssetType.ROOM.type,
                        false,
                        assetIds[(i-1)*5],
                        (String)null,
                        (String)null,
                        "master",
                        (String[])[assetIds[(i-1)*5+j], assetIds[(i-1)*5]].toArray(new String[0]),
                        null).addAttributes(
                        new AssetAttribute(AttributeType.LOCATION, new GeoJSONPoint(10,11).toValue(), new Date().getTime())
                            .addMeta(
                                MetaItemType.ACCESS_PUBLIC_READ
                            ),
                        new AssetAttribute("temp", AttributeValueType.TEMPERATURE, null, new Date().getTime())
                            .addMeta(
                                new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agentAssetIds[i-1], "protocolConfig").toArrayValue()),
                                new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create(""))
                            ),
                        new AssetAttribute("tempSetpoint", AttributeValueType.TEMPERATURE, null, new Date().getTime())
                            .addMeta(
                                new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agentAssetIds[i-1], "protocolConfig").toArrayValue()),
                                new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create(""))
                            )
                    )
                )
            }

            assets.add(
                new Asset(
                    assetIds[(i-1)*5],
                    0L,
                    new Date(),
                    "Test Building $i",
                    AssetType.BUILDING.type,
                    false,
                    (String)null,
                    (String)null,
                    (String)null,
                    "master",
                    (String[])[assetIds[(i-1)*5]].toArray(new String[0]),
                    null).addAttributes(
                    new AssetAttribute(AttributeType.LOCATION, new GeoJSONPoint(10,11).toValue(), new Date().getTime())
                        .addMeta(
                            MetaItemType.ACCESS_PUBLIC_READ
                        ),
                    new AssetAttribute(AttributeType.SURFACE_AREA, Values.create(1000), new Date().getTime())
                )
            )
        }

        and: "the gateway client replies to the central manager with the assets of the gateway"
        List<Asset> sendAssets = []
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(assets)
        def readAssetsReplyEvent = new EventRequestResponseWrapper(
            GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL,
            new AssetsEvent(sendAssets))
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + Container.JSON.writeValueAsString(readAssetsReplyEvent))

        then: "the central manager should have requested the full loading of the first batch of assets"
        String messageId = null
        ReadAssetsEvent readAssetsEvent = null
        conditions.eventually {
            assert clientReceivedMessages.size() == 1
            assert clientReceivedMessages.get(0).startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(0).contains("read-assets")
            def response = Container.JSON.readValue(clientReceivedMessages[0].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            messageId = response.messageId
            readAssetsEvent = response.event as ReadAssetsEvent
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + "0"
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.select.excludePath
            assert readAssetsEvent.assetQuery.select.excludeParentInfo
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert agentAssetIds.stream().filter{readAssetsEvent.assetQuery.ids.contains(it)}.count() == agentAssetIds.size()
            assert assetIds.stream().filter{readAssetsEvent.assetQuery.ids.contains(it)}.count() == GatewayConnector.SYNC_ASSET_BATCH_SIZE - agentAssetIds.size()
        }

        when: "the gateway returns the requested assets"
        sendAssets = []
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).filter{!agentAssetIds.contains(it)}.map{id -> assets.stream().filter{asset -> asset.id == id}.findFirst().orElse(null)}.collect(Collectors.toList()))
        readAssetsReplyEvent = new EventRequestResponseWrapper(
            messageId,
            new AssetsEvent(sendAssets)
        )
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + Container.JSON.writeValueAsString(readAssetsReplyEvent))

        then: "the central manager should have added the first batch of assets under the gateway asset"
        conditions.eventually {
            def syncedAssets = assetStorageService.findAll(new AssetQuery().parents(gateway.getId()).recursive(true))
            assert syncedAssets.size() == sendAssets.size()
            assert syncedAssets.stream().filter{syncedAsset -> sendAssets.stream().anyMatch{it.id == syncedAsset.id}}.count() == sendAssets.size()
            assert syncedAssets.find {it.id == agentAssetIds[0]}.getName() == "Test Agent 1"
            assert syncedAssets.find {it.id == agentAssetIds[0]}.getType() == AssetType.AGENT.type
            assert syncedAssets.find {it.id == agentAssetIds[0]}.getRealm() == managerDemoSetup.realmBuildingTenant
            assert syncedAssets.find {it.id == agentAssetIds[0]}.getParentId() == gateway.getId()
            assert syncedAssets.find {it.id == agentAssetIds[4]}.getName() == "Test Agent 5"
            assert syncedAssets.find {it.id == agentAssetIds[4]}.getType() == AssetType.AGENT.type
            assert syncedAssets.find {it.id == agentAssetIds[4]}.getRealm() == managerDemoSetup.realmBuildingTenant
            assert syncedAssets.find {it.id == agentAssetIds[4]}.getParentId() == gateway.getId()
            assert syncedAssets.find {it.id == assetIds[0]}.getName() == "Test Building 1"
            assert syncedAssets.find {it.id == assetIds[0]}.getType() == AssetType.BUILDING.type
            assert syncedAssets.find {it.id == assetIds[0]}.getRealm() == managerDemoSetup.realmBuildingTenant
            assert syncedAssets.find {it.id == assetIds[0]}.getParentId() == gateway.getId()
            assert syncedAssets.find {it.id == assetIds[0]}.getAttribute(AttributeType.LOCATION).flatMap {GeoJSONPoint.fromValue(it.getValue().orElse(null))}.get().x == 10
            assert syncedAssets.find {it.id == assetIds[0]}.getAttribute(AttributeType.LOCATION).flatMap {GeoJSONPoint.fromValue(it.getValue().orElse(null))}.get().y == 11
            assert syncedAssets.find {it.id == assetIds[0]}.getAttribute(AttributeType.LOCATION).flatMap{it.getMetaItem(MetaItemType.ACCESS_PUBLIC_READ)}.isPresent()
            assert syncedAssets.find {it.id == assetIds[0]}.getAttribute(AttributeType.SURFACE_AREA).flatMap {it.getValueAsNumber()}.orElse(0d) == 1000
            assert syncedAssets.find {it.id == assetIds[1]}.getName() == "Test Building 1 Room 1"
            assert syncedAssets.find {it.id == assetIds[1]}.getType() == AssetType.ROOM.type
            assert syncedAssets.find {it.id == assetIds[1]}.getRealm() == managerDemoSetup.realmBuildingTenant
            assert syncedAssets.find {it.id == assetIds[1]}.getParentId() == assetIds[0]
            assert syncedAssets.find {it.id == assetIds[1]}.getAttribute("temp").map{it.hasMetaItem(MetaItemType.AGENT_LINK)}.orElse(false)
            assert syncedAssets.find {it.id == assetIds[1]}.getAttribute("tempSetpoint").map{it.hasMetaItem(MetaItemType.AGENT_LINK)}.orElse(false)
            assert syncedAssets.find {it.id == assetIds[5]}.getName() == "Test Building 2"
            assert syncedAssets.find {it.id == assetIds[5]}.getType() == AssetType.BUILDING.type
            assert syncedAssets.find {it.id == assetIds[5]}.getRealm() == managerDemoSetup.realmBuildingTenant
            assert syncedAssets.find {it.id == assetIds[5]}.getParentId() == gateway.getId()
            assert syncedAssets.find {it.id == assetIds[5]}.getAttribute(AttributeType.LOCATION).flatMap {GeoJSONPoint.fromValue(it.getValue().orElse(null))}.get().x == 10
            assert syncedAssets.find {it.id == assetIds[5]}.getAttribute(AttributeType.LOCATION).flatMap {GeoJSONPoint.fromValue(it.getValue().orElse(null))}.get().y == 11
            assert syncedAssets.find {it.id == assetIds[5]}.getAttribute(AttributeType.LOCATION).flatMap{it.getMetaItem(MetaItemType.ACCESS_PUBLIC_READ)}.isPresent()
            assert syncedAssets.find {it.id == assetIds[5]}.getAttribute(AttributeType.SURFACE_AREA).flatMap {it.getValueAsNumber()}.orElse(0d) == 1000
            assert syncedAssets.find {it.id == assetIds[6]}.getName() == "Test Building 2 Room 1"
            assert syncedAssets.find {it.id == assetIds[6]}.getType() == AssetType.ROOM.type
            assert syncedAssets.find {it.id == assetIds[6]}.getRealm() == managerDemoSetup.realmBuildingTenant
            assert syncedAssets.find {it.id == assetIds[6]}.getParentId() == assetIds[5]
            assert syncedAssets.find {it.id == assetIds[6]}.getAttribute("temp").map{it.hasMetaItem(MetaItemType.AGENT_LINK)}.orElse(false)
            assert syncedAssets.find {it.id == assetIds[6]}.getAttribute("tempSetpoint").map{it.hasMetaItem(MetaItemType.AGENT_LINK)}.orElse(false)
        }

        and: "the central manager should have requested the full loading of the second batch of assets"
        conditions.eventually {
            assert clientReceivedMessages.size() == 2
            assert clientReceivedMessages.get(1).startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(1).contains("read-assets")
            def response = Container.JSON.readValue(clientReceivedMessages[1].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            messageId = response.messageId
            readAssetsEvent = response.event as ReadAssetsEvent
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.select.excludePath
            assert readAssetsEvent.assetQuery.select.excludeParentInfo
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == agentAssetIds.size() + assetIds.size() - GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert assetIds.stream().filter{id -> sendAssets.stream().noneMatch{it.id == id}}.count() == readAssetsEvent.assetQuery.ids.length
        }

        when: "the gateway returns the requested assets"
        sendAssets = []
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).map{id -> assets.stream().filter{asset -> asset.id == id}.findFirst().orElse(null)}.collect(Collectors.toList()))
        readAssetsReplyEvent = new EventRequestResponseWrapper(
            messageId,
            new AssetsEvent(sendAssets))
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + Container.JSON.writeValueAsString(readAssetsReplyEvent))

        then: "the gateway asset status should become connected"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getAttribute("status").flatMap{it.getValueAsString()}.orElse(null) == ConnectionStatus.CONNECTED.name()
        }

        and: "all the gateway assets should be replicated underneath the gateway"
        assert assetStorageService.findAll(new AssetQuery().parents(gateway.getId()).recursive(true)).size() == agentAssets.size() + assets.size()

        and: "the http client protocol of the gateway agents should not have been linked to the central manager"
        Thread.sleep(2000)
        conditions.eventually {
            assert !httpClientProtocol.clientMap.containsKey(new AttributeRef(agentAssetIds[0], "protocolConfig"))
            assert !httpClientProtocol.clientMap.containsKey(new AttributeRef(agentAssetIds[4], "protocolConfig"))
        }

        when: "the previously received messages are cleared"
        clientReceivedMessages.clear()

        and: "an attribute event for a gateway descendant asset (building 1 room 1) is sent to the local manager"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(assetIds[1], "tempSetpoint", Values.create(20d)))

        then: "the event should have been forwarded to the gateway"
        conditions.eventually {
            assert clientReceivedMessages.size() == 1
            assert clientReceivedMessages.get(0).startsWith(SharedEvent.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(0).contains("attribute")
        }

        when: "the gateway handles the forwarded attribute event and sends a follow up attribute event to the local manager"
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new AttributeEvent(assetIds[1], "tempSetpoint", Values.create(20d))))

        then: "the descendant asset in the local manager should contain the new attribute value"
        conditions.eventually {
            def building1Room1Asset = assetStorageService.find(assetIds[1])
            assert building1Room1Asset.getAttribute("tempSetpoint").flatMap {it.getValueAsNumber()}.orElse(0d) == 20d
        }

        when: "an asset is added on the gateway and the local manager is notified"
        def building1Room5AssetId = UniqueIdentifierGenerator.generateId("Test Building 1 Room 5")
        def building1Room5Asset = new Asset(
            building1Room5AssetId,
            0L,
            new Date(),
            "Test Building 1 Room 5",
            AssetType.ROOM.type,
            false,
            assetIds[0],
            (String)null,
            (String)null,
            "master",
            (String[])[building1Room5AssetId, assetIds[0]].toArray(new String[0]),
            null).addAttributes(
            new AssetAttribute(AttributeType.LOCATION, new GeoJSONPoint(10,11).toValue(), new Date().getTime())
                .addMeta(
                    MetaItemType.ACCESS_PUBLIC_READ
                ),
            new AssetAttribute("temp", AttributeValueType.TEMPERATURE, null, new Date().getTime())
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agentAssetIds[0], "protocolConfig").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create(""))
                ),
            new AssetAttribute("tempSetpoint", AttributeValueType.TEMPERATURE, null, new Date().getTime())
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agentAssetIds[0], "protocolConfig").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create(""))
                )
        )
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new AssetEvent(AssetEvent.Cause.CREATE, building1Room5Asset, null)))

        then: "the asset should be replicated in the local manager"
        conditions.eventually {
            building1Room5Asset = assetStorageService.find(building1Room5AssetId)
            assert building1Room5Asset != null
            assert building1Room5Asset.getName() == "Test Building 1 Room 5"
            assert building1Room5Asset.getType() == AssetType.ROOM.type
            assert building1Room5Asset.getRealm() == managerDemoSetup.realmBuildingTenant
            assert building1Room5Asset.getParentId() == assetIds[0]
            assert building1Room5Asset.getAttributesList().size() == 3
        }

        when: "an asset is modified on the gateway and the local manager is notified"
        building1Room5Asset.setName("Test Building 1 Room 5 Updated")
        building1Room5Asset.setVersion(1)
        building1Room5Asset.addAttributes(
            new AssetAttribute("co2Level", AttributeValueType.CO2, Values.create(500), new Date().getTime())
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agentAssetIds[0], "protocolConfig").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create(""))
                )
        )
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new AssetEvent(AssetEvent.Cause.UPDATE, building1Room5Asset, (String[]) ["name", "attributes"].toArray(new String[0]))))

        then: "the asset should also be updated in the local manager"
        conditions.eventually {
            building1Room5Asset = assetStorageService.find(building1Room5AssetId)
            assert building1Room5Asset != null
            assert building1Room5Asset.getVersion() == 1
            assert building1Room5Asset.getName() == "Test Building 1 Room 5 Updated"
            assert building1Room5Asset.getType() == AssetType.ROOM.type
            assert building1Room5Asset.getRealm() == managerDemoSetup.realmBuildingTenant
            assert building1Room5Asset.getParentId() == assetIds[0]
            assert building1Room5Asset.getAttributesList().size() == 4
            assert building1Room5Asset.getAttribute("co2Level").flatMap{it.getValueAsNumber()}.orElse(0d) == 500d
        }

        when: "an asset is deleted on the gateway and the local manager is notified"
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new AssetEvent(AssetEvent.Cause.DELETE, building1Room5Asset, null)))

        then: "the asset should also be deleted in the local manager"
        conditions.eventually {
            def asset = assetStorageService.find(building1Room5AssetId)
            assert asset == null
        }

        when: "the client received messages are cleared"
        clientReceivedMessages.clear()

        and: "an asset is added under the gateway in the local manager and the gateway responds that it successfully added the asset"
        def responseFuture = new AtomicReference<ScheduledFuture>()
        responseFuture.set(executorService.scheduleAtFixedRate({
            if (!clientReceivedMessages.isEmpty()) {
                def assetAddEvent = Container.JSON.readValue(clientReceivedMessages[0].substring(SharedEvent.MESSAGE_PREFIX.length()), AssetEvent.class)
                if (assetAddEvent.cause == AssetEvent.Cause.CREATE && assetAddEvent.asset.id.equals(building1Room5AssetId)) {
                    gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new AssetEvent(AssetEvent.Cause.CREATE, building1Room5Asset, null)))
                    responseFuture.get().cancel(false)
                }
            }
        }, 100, 100))
        building1Room5Asset = assetStorageService.merge(building1Room5Asset)

        then: "the asset should have been added to the gateway and eventually replicated in the local manager"
        assert building1Room5Asset != null
        assert assetStorageService.find(building1Room5AssetId).version == building1Room5Asset.version

        when: "the client received messages are cleared"
        clientReceivedMessages.clear()

        and: "an asset is modified under the gateway in the local manager and the gateway responds that it successfully updated the asset"
        building1Room5Asset.setName("Test Building 1 Room 5")
        building1Room5Asset.removeAttribute("co2Level")
        responseFuture.set(executorService.scheduleAtFixedRate({
            if (!clientReceivedMessages.isEmpty()) {
                def assetAddEvent = Container.JSON.readValue(clientReceivedMessages[0].substring(SharedEvent.MESSAGE_PREFIX.length()), AssetEvent.class)
                if (assetAddEvent.cause == AssetEvent.Cause.UPDATE && assetAddEvent.asset.id.equals(building1Room5AssetId)) {
                    gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new AssetEvent(AssetEvent.Cause.UPDATE, building1Room5Asset, null)))
                    responseFuture.get().cancel(false)
                }
            }
        }, 100, 100))
        building1Room5Asset = assetStorageService.merge(building1Room5Asset)

        then: "the asset should also be updated in the local manager"
        assert building1Room5Asset != null
        assert building1Room5Asset.getVersion() == 2
        assert building1Room5Asset.getName() == "Test Building 1 Room 5"
        assert building1Room5Asset.getType() == AssetType.ROOM.type
        assert building1Room5Asset.getRealm() == managerDemoSetup.realmBuildingTenant
        assert building1Room5Asset.getParentId() == assetIds[0]
        assert building1Room5Asset.getAttributesList().size() == 3
        assert assetStorageService.find(building1Room5AssetId).version == building1Room5Asset.version

        when: "the client received messages are cleared"
        clientReceivedMessages.clear()

        and: "an asset is deleted under the gateway in the local manager and the gateway responds that it successfully deleted the asset"
        responseFuture.set(executorService.scheduleAtFixedRate({
            if (!clientReceivedMessages.isEmpty()) {
                def request = Container.JSON.readValue(clientReceivedMessages[0].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
                def deleteRequest = request.event as DeleteAssetsRequestEvent
                if (deleteRequest.assetIds.size() == 1 && deleteRequest.assetIds.get(0) == building1Room5AssetId) {
                    gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new EventRequestResponseWrapper(request.messageId, new DeleteAssetsResponseEvent(true, deleteRequest.assetIds))))
                    gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new AssetEvent(AssetEvent.Cause.DELETE, building1Room5Asset, null)))
                    responseFuture.get().cancel(false)
                }
            }
        }, 100, 100))
        def deleted = assetStorageService.delete([building1Room5AssetId])

        then: "the asset should have been deleted"
        assert deleted
        conditions.eventually {
            assert assetStorageService.find(building1Room5AssetId) == null
        }

        when: "the gateway asset is marked as disabled"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(gateway.getId(), "disabled", Values.create(true)))

        then: "the gateway asset status should become disabled"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId(), true)
            assert gateway.getAttribute("status").flatMap{it.getValueAsString()}.orElse("") == ConnectionStatus.DISABLED.name()
        }

        and: "the gateway connector should be marked as disconnected and the gateway client should have been disconnected"
        conditions.eventually {
            assert gatewayService.gatewayConnectorMap.get(gateway.getId()).disabled
            assert !gatewayService.gatewayConnectorMap.get(gateway.getId()).connected
            assert gatewayClient.connectionStatus == ConnectionStatus.CONNECTING
        }

        and: "the central manager should have sent a disconnect event to the client"
        conditions.eventually {
            assert clientReceivedMessages.last().contains("gateway-disconnect")
        }
        gatewayClient.disconnect()

        when: "an attempt is made to add a descendant asset to a gateway that isn't connected"
        def failedAsset = new Asset(
            UniqueIdentifierGenerator.generateId("Failed asset"),
            0L,
            new Date(),
            "Failed Asset",
            AssetType.BUILDING.type,
            false,
            gateway.id,
            (String)null,
            (String)null,
            managerDemoSetup.realmBuildingTenant,
            (String[])[UniqueIdentifierGenerator.generateId("Failed asset")].toArray(new String[0]),
            null).addAttributes(
            new AssetAttribute(AttributeType.LOCATION, new GeoJSONPoint(10,11).toValue(), new Date().getTime())
                .addMeta(
                    MetaItemType.ACCESS_PUBLIC_READ
                ),
            new AssetAttribute(AttributeType.SURFACE_AREA, Values.create(1000), new Date().getTime())
        )
        assetStorageService.merge(failedAsset)

        then: "an error should occur"
        thrown(IllegalStateException)

        when: "gateway assets are modified whilst the gateway is disconnected (building1Room5 re-added, building5 and descendants removed and building1 modified and building1Room1 attribute updated)"
        assets[4].setName("Test Building 1 Updated")
        assets[4].setVersion(2L)
        assets[0].getAttribute("temp").ifPresent{it.setValue(Values.create(10), new Date().getTime())}
        assets = assets.subList(0, 20)

        and: "the client received messages are cleared"
        clientReceivedMessages.clear()

        and: "the gateway is enabled again"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(gateway.getId(), "disabled", Values.create(false)))

        then: "the gateway connector should be enabled"
        conditions.eventually {
            assert !gatewayService.gatewayConnectorMap.get(gateway.getId()).disabled
        }

        and: "the gateway client reconnects"
        gatewayClient.connect()

        then: "the gateway netty client status should become CONNECTED"
        conditions.eventually {
            assert connectionStatus == ConnectionStatus.CONNECTED
            assert gatewayClient.connectionStatus == ConnectionStatus.CONNECTED
        }

        and: "the gateway asset connection status should be CONNECTING"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getAttribute("status").flatMap{it.getValueAsString()}.orElse(null) == ConnectionStatus.CONNECTING.name()
        }

        and: "the local manager should have sent an asset read request"
        conditions.eventually {
            assert clientReceivedMessages.size() >= 1
            assert clientReceivedMessages[0].startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            assert clientReceivedMessages[0].contains("read-assets")
        }

        when: "the previously received messages are cleared"
        clientReceivedMessages.clear()

        and: "the gateway client replies to the central manager with the assets of the gateway"
        sendAssets = [building1Room5Asset]
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(assets)
        readAssetsReplyEvent = new EventRequestResponseWrapper(
            GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL,
            new AssetsEvent(sendAssets)
        )
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + Container.JSON.writeValueAsString(readAssetsReplyEvent))

        then: "the central manager should have requested the full loading of the first batch of assets"
        conditions.eventually {
            assert clientReceivedMessages.size() == 1
            assert clientReceivedMessages.get(0).startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(0).contains("read-assets")
            def request = Container.JSON.readValue(clientReceivedMessages[0].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            messageId = request.messageId
            readAssetsEvent = request.event as ReadAssetsEvent
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + "0"
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.select.excludePath
            assert readAssetsEvent.assetQuery.select.excludeParentInfo
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert agentAssetIds.stream().filter{readAssetsEvent.assetQuery.ids.contains(it)}.count() == agentAssetIds.size()
            assert assetIds.stream().filter{readAssetsEvent.assetQuery.ids.contains(it)}.count() + 1 == GatewayConnector.SYNC_ASSET_BATCH_SIZE - agentAssetIds.size()
        }

        when: "another asset is added to the gateway during the initial sync process"
        def building2Room5Asset = new Asset(
            UniqueIdentifierGenerator.generateId("Test Building 2 Room 5"),
            0L,
            new Date(),
            "Test Building 2 Room 5",
            AssetType.ROOM.type,
            false,
            assetIds[5],
            (String)null,
            (String)null,
            "master",
            (String[])[UniqueIdentifierGenerator.generateId("Test Building 2 Room 5"), assetIds[5]].toArray(new String[0]),
            null).addAttributes(
            new AssetAttribute(AttributeType.LOCATION, new GeoJSONPoint(10,11).toValue(), new Date().getTime())
                .addMeta(
                    MetaItemType.ACCESS_PUBLIC_READ
                ),
            new AssetAttribute("temp", AttributeValueType.TEMPERATURE, null, new Date().getTime())
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agentAssetIds[1], "protocolConfig").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create(""))
                ),
            new AssetAttribute("tempSetpoint", AttributeValueType.TEMPERATURE, null, new Date().getTime())
                .addMeta(
                    new MetaItem(MetaItemType.AGENT_LINK, new AttributeRef(agentAssetIds[1], "protocolConfig").toArrayValue()),
                    new MetaItem(HttpClientProtocol.META_ATTRIBUTE_PATH, Values.create(""))
                )
        )
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new AssetEvent(AssetEvent.Cause.CREATE, building2Room5Asset, null)))

        and: "another asset is deleted from the gateway during the initial sync process (Building 3 Room 1)"
        def removedAsset = assets.remove(10)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(new AssetEvent(AssetEvent.Cause.DELETE, removedAsset, null)))

        and: "the gateway returns the requested assets (minus the deleted Building 3 Room 1 asset)"
        sendAssets = [building1Room5Asset]
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).filter{!agentAssetIds.contains(it) && it != removedAsset.id && it != building1Room5Asset.id}.map{id -> assets.stream().filter{asset -> asset.id == id}.findFirst().orElse(null)}.collect(Collectors.toList()))
        readAssetsReplyEvent = new EventRequestResponseWrapper(messageId, new AssetsEvent(sendAssets))
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + Container.JSON.writeValueAsString(readAssetsReplyEvent))

        then: "the central manager should have requested the full loading of the second batch of assets"
        conditions.eventually {
            assert clientReceivedMessages.size() == 2
            assert clientReceivedMessages.get(1).startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(1).contains("read-assets")
            def request = Container.JSON.readValue(clientReceivedMessages[1].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            messageId = request.messageId
            readAssetsEvent = request.event as ReadAssetsEvent
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + (GatewayConnector.SYNC_ASSET_BATCH_SIZE - 1)
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.select.excludePath
            assert readAssetsEvent.assetQuery.select.excludeParentInfo
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == agentAssetIds.size() + assets.size() + 1 - GatewayConnector.SYNC_ASSET_BATCH_SIZE + 1
            assert assets.stream().filter{asset -> sendAssets.stream().noneMatch{it.id == asset.id}}.count() == readAssetsEvent.assetQuery.ids.length
        }

        when: "the gateway returns the requested assets"
        sendAssets = []
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).map{id -> assets.stream().filter{asset -> asset.id == id}.findFirst().orElse(null)}.collect(Collectors.toList()))
        readAssetsReplyEvent = new EventRequestResponseWrapper(messageId, new AssetsEvent(sendAssets))
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + Container.JSON.writeValueAsString(readAssetsReplyEvent))

        then: "the gateway asset status should become connected"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getAttribute("status").flatMap{it.getValueAsString()}.orElse(null) == ConnectionStatus.CONNECTED.name()
        }

        and: "the gateway should have the correct assets"
        def gatewayAssets = assetStorageService.findAll(new AssetQuery().parents(gateway.getId()).recursive(true))
        assert gatewayAssets.size() == 2 + agentAssets.size() + assets.size()

        when: "the gateway asset is deleted"
        deleted = assetStorageService.delete([gateway.id])

        then: "all descendant assets should have been removed"
        conditions.eventually {
            assert assetStorageService.find(assets[0].id) == null
        }

        then: "the keycloak client should also be removed"
        assert deleted
        conditions.eventually {
            assert identityProvider.getClient(managerDemoSetup.realmBuildingTenant, GatewayService.GATEWAY_CLIENT_ID_PREFIX + gateway.getId()) == null
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Verify gateway client service"() {

        given: "a spy gateway client service"
        // This spy maps asset IDs to fake IDs to allow both the client and central manager to actually be the same
        // manager instance
        Map<String, String> realIdToFakeIdMap = new HashMap<>()
        Map<String, String> fakeIdToRealIdMap = new HashMap<>()
        GatewayClientService spyGatewayClientService = Spy(GatewayClientService) {
            messageToString(_ as String, _ as Object) >> {
                String prefix, Object ev ->
                    String messageId
                    Object event = ev

                    if (ev instanceof EventRequestResponseWrapper) {
                        messageId = ev.messageId
                        event = ev.event
                    }

                    if (event instanceof AssetsEvent) {
                        if (GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL.equals(messageId)) {
                            // Store real IDs and map to a fake IDs
                            event.getAssets().forEach {
                                String oldId = it.id
                                it.id = UniqueIdentifierGenerator.generateId()
                                realIdToFakeIdMap.put(oldId, it.id)
                                fakeIdToRealIdMap.put(it.id, oldId)
                            }
                        } else {
                            event.getAssets().forEach {
                                it.id = realIdToFakeIdMap.get(it.id)
                                it.parentId = it.parentId == null ? null : realIdToFakeIdMap.get(it.parentId)
                            }
                        }
                    } else if (event instanceof AssetEvent) {
                        if (!realIdToFakeIdMap.containsKey(event.asset.id)) {
                            def fakeId = UniqueIdentifierGenerator.generateId()
                            realIdToFakeIdMap.put(event.asset.id, fakeId)
                            fakeIdToRealIdMap.put(fakeId, event.asset.id)
                        }
                        event.asset.id = realIdToFakeIdMap.get(event.entityId)
                        event.asset.parentId = event.parentId == null ? null : realIdToFakeIdMap.get(event.parentId)
                    } else if (event instanceof AttributeEvent) {
                        event.attributeState = new AttributeState(
                            realIdToFakeIdMap.get(event.entityId),
                            event.attributeState.attributeRef.attributeName,
                            event.attributeState.value.orElse(null)
                        )
                        event.parentId = event.parentId == null ? null : realIdToFakeIdMap.get(event.parentId)
                    }

                    return callRealMethodWithArgs(prefix, ev)
            }

            messageFromString(_ as String, _ as String, _ as Class) >> {
                String message, String prefix, Class clazz ->
                    Object ev = callRealMethod()
                    Object event = ev

                    if (event instanceof EventRequestResponseWrapper) {
                        event = event.event
                    }
                    if (event instanceof ReadAssetsEvent) {
                        if (event.assetQuery.ids != null && event.assetQuery.ids.length > 0) {
                            event.assetQuery.ids(Arrays.stream(event.assetQuery.ids).map{fakeIdToRealIdMap.get(it)}.toArray{new String[it]})
                        }
                    }
                    if (event instanceof AssetEvent) {
                        if (fakeIdToRealIdMap.containsKey(event.entityId)) {
                            event.asset.id = fakeIdToRealIdMap.get(event.entityId)
                        } else {
                            // This is a new asset creation originating from the central manager
                            def id = UniqueIdentifierGenerator.generateId()
                            realIdToFakeIdMap.put(id, event.entityId)
                            fakeIdToRealIdMap.put(event.entityId, id)
                        }
                        event.asset.id = fakeIdToRealIdMap.get(event.entityId)
                        event.asset.parentId = event.parentId == null ? null : fakeIdToRealIdMap.get(event.parentId)
                    } else if (event instanceof AttributeEvent) {
                        event.attributeState = new AttributeState(
                            fakeIdToRealIdMap.get(event.entityId),
                            event.attributeState.attributeRef.attributeName,
                            event.attributeState.value.orElse(null)
                        )
                        event.parentId = event.parentId == null ? null : fakeIdToRealIdMap.get(event.parentId)
                    } else if (event instanceof DeleteAssetsRequestEvent) {
                        event.assetIds = event.assetIds.stream().map{fakeIdToRealIdMap.get(it)}.collect(Collectors.toList())
                    }
                    return ev
            }
        }

        and: "the container environment is started with the spy gateway client service"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def services = Lists.newArrayList(defaultServices())
        services.replaceAll{it instanceof GatewayClientService ? spyGatewayClientService : it}
        def container = startContainer(defaultConfig(serverPort), services)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def gatewayService = container.getService(GatewayService.class)
        def gatewayClientService = container.getService(GatewayClientService.class)
        def simulatorProtocol = container.getService(SimulatorProtocol.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        and: "an authenticated admin user"
        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the gateway client resource"
        def gatewayClientResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(GatewayClientResource.class)

        expect: "the container should be running and initialised"
        conditions.eventually {
            assert container.isRunning()
            assert noEventProcessedIn(assetProcessingService, 300)
        }

        when: "a gateway is provisioned in this manager in the building realm"
        def gateway = assetStorageService.merge(new Asset("Test gateway", AssetType.GATEWAY, null, managerDemoSetup.realmBuildingTenant))

        then: "a set of credentials should have been created for this gateway and be stored against the gateway for easy reference"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId(), true)
            assert gateway.getAttribute("clientId").isPresent()
            assert !isNullOrEmpty(gateway.getAttribute("clientId").flatMap{it.getValueAsString()}.orElse(""))
            assert !isNullOrEmpty(gateway.getAttribute("clientSecret").flatMap{it.getValueAsString()}.orElse(""))
        }

        and: "a gateway connector should have been created for this gateway"
        conditions.eventually {
            assert gatewayService.gatewayConnectorMap.size() == 1
            assert gatewayService.gatewayConnectorMap.get(gateway.getId()).gatewayId == gateway.getId()
        }

        when: "a gateway client connection is created to connect the city realm to the gateway"
        def gatewayConnection = new GatewayConnection(
            "localhost",
            serverPort,
            managerDemoSetup.realmBuildingTenant,
            gateway.getAttribute("clientId").flatMap{it.getValueAsString()}.orElse(""),
            gateway.getAttribute("clientSecret").flatMap{it.getValueAsString()}.orElse(""),
            false,
            false
        )
        gatewayClientResource.setConnection(null, managerDemoSetup.realmCityTenant, gatewayConnection)

        then: "the gateway client should become connected"
        conditions.eventually {
            assert gatewayClientService.clientRealmMap.get(managerDemoSetup.realmCityTenant) != null
        }

        and: "the gateway asset connection status should become CONNECTED"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getAttribute("status").flatMap{it.getValueAsString()}.orElse(null) == ConnectionStatus.CONNECTED.name()
        }

        and: "the fake mirrored assets should have been created under the gateway asset"
        conditions.eventually {
            def fakeAssets = assetStorageService.findAll(new AssetQuery().parents(gateway.id).recursive(true))
            fakeAssets.size() == realIdToFakeIdMap.size()
        }

        when: "a gateway client asset is modified"
        def microphone1 = assetStorageService.find(managerDemoSetup.microphone1Id)
        microphone1.setName("Microphone 1 Updated")
        microphone1.getAttribute("microphoneLevel").ifPresent{it.addMeta(MetaItemType.UNIT_TYPE.withInitialValue(UNITS_SOUND_DECIBELS))}
        microphone1.addAttributes(
            new AssetAttribute("test", AttributeValueType.DISTANCE, Values.create(100))
        )
        microphone1 = assetStorageService.merge(microphone1)

        then: "the mirrored asset under the gateway should have also been updated"
        conditions.eventually {
            def mirroredMicrophone = assetStorageService.find(realIdToFakeIdMap.get(managerDemoSetup.microphone1Id))
            assert mirroredMicrophone != null
            assert mirroredMicrophone.getAttribute("microphoneLevel").flatMap{it.getMetaItem(MetaItemType.UNIT_TYPE)}.flatMap{it.getValueAsString()}.orElse("") == UNITS_SOUND_DECIBELS
            assert mirroredMicrophone.getAttribute("test").isPresent()
            assert mirroredMicrophone.getAttribute("test").flatMap{it.getValueAsNumber()}.orElse(0) == 100
        }

        when: "a gateway client asset is added"
        def microphone2 = new Asset("Microphone 2", AssetType.MICROPHONE, null, managerDemoSetup.realmCityTenant).addAttributes(
            new AssetAttribute("test", AttributeValueType.STRING, Values.create("testValue"))
        )
        microphone2.setParentId(managerDemoSetup.area1Id)
        microphone2 = assetStorageService.merge(microphone2)

        then: "the new asset should have been created in the gateway and also mirrored under the gateway asset"
        assert microphone2.id != null
        conditions.eventually {
            def mirroredMicrophone2 = assetStorageService.find(realIdToFakeIdMap.get(microphone2.id))
            assert mirroredMicrophone2 != null
            assert mirroredMicrophone2.getAttribute("microphoneLevel").isPresent()
            assert mirroredMicrophone2.getAttribute("test").flatMap{it.getValueAsString()}.orElse("") == "testValue"
        }

        when: "an asset is added under the gateway client"
        def microphone3 = new Asset()
        Asset.map(microphone1, microphone3, "Microphone 3", managerDemoSetup.realmBuildingTenant, realIdToFakeIdMap.get(managerDemoSetup.area1Id), null, null, null)
        microphone3 = assetStorageService.merge(microphone3)

        then: "the new asset should have been created in the gateway and also mirrored under the gateway asset"
        assert microphone3.id != null
        conditions.eventually {
            def gatewayMicrophone3 = assetStorageService.find(fakeIdToRealIdMap.get(microphone3.id))
            assert gatewayMicrophone3 != null
            assert gatewayMicrophone3.getAttribute("microphoneLevel").isPresent()
        }

        and: "the new asset microphone level should be correctly linked to the simulator protocol only on the gateway"
        conditions.eventually {
            assert !simulatorProtocol.elements.containsKey(new AttributeRef(microphone3.id, "microphoneLevel"))
            assert simulatorProtocol.elements.containsKey(new AttributeRef(fakeIdToRealIdMap.get(microphone3.id), "microphoneLevel"))
        }

        when: "an attribute is updated on the gateway client"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(microphone2.id, "test", Values.create("newValue")))

        then: "the mirrored asset attribute should also be updated"
        conditions.eventually {
            def mirroredMicrophone2 = assetStorageService.find(realIdToFakeIdMap.get(microphone2.id))
            assert mirroredMicrophone2 != null
            assert mirroredMicrophone2.getAttribute("test").flatMap{it.getValueAsString()}.orElse("") == "newValue"
        }

        when: "a mirrored asset attribute is updated"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(realIdToFakeIdMap.get(microphone2.id), "test", Values.create("newerValue")))

        then: "the attribute should be updated on the gateway client and the mirrored asset"
        conditions.eventually {
            microphone2 = assetStorageService.find(microphone2.id)
            def mirroredMicrophone2 = assetStorageService.find(realIdToFakeIdMap.get(microphone2.id))
            assert microphone2 != null
            assert mirroredMicrophone2 != null
            assert microphone2.getAttribute("test").flatMap{it.getValueAsString()}.orElse("") == "newerValue"
            assert mirroredMicrophone2.getAttribute("test").flatMap{it.getValueAsString()}.orElse("") == "newerValue"
        }

        when: "a gateway client asset is deleted"
        def deleted = assetStorageService.delete(Collections.singletonList(managerDemoSetup.microphone1Id))

        then: "the client asset should have been deleted and also the mirrored asset under the gateway should also be deleted"
        assert deleted
        conditions.eventually {
            assert assetStorageService.find(realIdToFakeIdMap.get(managerDemoSetup.microphone1Id)) == null
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
