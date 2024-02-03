package org.openremote.test.gateway

import com.google.common.collect.Lists
import io.netty.channel.ChannelHandler
import org.apache.http.client.utils.URIBuilder
import org.openremote.agent.protocol.http.HTTPAgent
import org.openremote.agent.protocol.http.HTTPAgentLink
import org.openremote.agent.protocol.io.AbstractNettyIOClient
import org.openremote.agent.protocol.simulator.SimulatorProtocol
import org.openremote.agent.protocol.websocket.WebsocketIOClient
import org.openremote.container.timer.TimerService
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.gateway.GatewayClientService
import org.openremote.manager.gateway.GatewayConnector
import org.openremote.manager.gateway.GatewayService
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.security.ManagerKeycloakIdentityProvider
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.LightAsset
import org.openremote.model.security.User
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.model.asset.*
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.GatewayAsset
import org.openremote.model.asset.impl.MicrophoneAsset
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.auth.OAuthClientCredentialsGrant
import org.openremote.model.event.shared.EventRequestResponseWrapper
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.gateway.GatewayClientResource
import org.openremote.model.gateway.GatewayConnection
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.IntStream

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.gateway.GatewayConnector.mapAssetId
import static org.openremote.manager.gateway.GatewayService.getGatewayClientId
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.util.TextUtil.isNullOrEmpty
import static org.openremote.model.value.MetaItemType.*
import static org.openremote.model.value.ValueType.*

class GatewayTest extends Specification implements ManagerContainerTrait {

    def "Gateway asset provisioning and local manager logic test"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def executorService = container.getExecutorService()
        def timerService = container.getService(TimerService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def gatewayService = container.getService(GatewayService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def identityProvider = container.getService(ManagerIdentityService.class).identityProvider as ManagerKeycloakIdentityProvider

        expect: "the system should settle down"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 300)
        }

        when: "a gateway is provisioned in this manager"
        GatewayAsset gateway = assetStorageService.merge(new GatewayAsset("Test gateway")
            .setRealm(managerTestSetup.realmBuildingName))

        then: "a keycloak client should have been created for this gateway"
        conditions.eventually {
            def client = identityProvider.getClient(managerTestSetup.realmBuildingName, getGatewayClientId(gateway.getId()))
            assert client != null
        }

        and: "a set of credentials should have been created for this gateway and be stored against the gateway for easy reference"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId(), true)
            assert gateway.getAttribute("clientId").isPresent()
            assert !isNullOrEmpty(gateway.getAttribute("clientId").flatMap{it.getValue()}.orElse(""))
            assert !isNullOrEmpty(gateway.getAttribute("clientSecret").flatMap{it.getValue()}.orElse(""))
        }

        and: "a gateway connector should have been created for this gateway"
        conditions.eventually {
            assert gatewayService.gatewayConnectorMap.size() == 1
            assert gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT)).gatewayId == gateway.getId()
        }

        when: "the Gateway client is created"
        def gatewayClient = new WebsocketIOClient<String>(
            new URIBuilder("ws://127.0.0.1:$serverPort/websocket/events?Realm=$managerTestSetup.realmBuildingName").build(),
            null,
            new OAuthClientCredentialsGrant("http://127.0.0.1:$serverPort/auth/realms/$managerTestSetup.realmBuildingName/protocol/openid-connect/token",
                gateway.getClientId().orElse(""),
                gateway.getClientSecret().orElse(""),
                null).setBasicAuthHeader(true))
        gatewayClient.setEncoderDecoderProvider({
            [new AbstractNettyIOClient.MessageToMessageDecoder<String>(String.class, gatewayClient)].toArray(new ChannelHandler[0])
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
            gateway = assetStorageService.find(gateway.getId()) as GatewayAsset
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTING
        }

        and: "the server should have sent a CONNECTED message and an asset read request"
        conditions.eventually {
            assert clientReceivedMessages.size() >= 1
            assert clientReceivedMessages[0].startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            def response = ValueUtil.JSON.readValue(clientReceivedMessages[0].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            assert response.messageId == GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL
            def localReadAssetsEvent = response.event as ReadAssetsEvent
            assert localReadAssetsEvent.assetQuery != null
            assert localReadAssetsEvent.assetQuery.recursive
        }

        when: "the previously received messages are cleared"
        clientReceivedMessages.clear()

        and: "the gateway client assets are defined"
        List<String> agentAssetIds = []
        List<HTTPAgent> agentAssets = []
        List<String> assetIds = []
        List<Asset> assets = []

        IntStream.rangeClosed(1, 5).forEach {i ->
            agentAssetIds.add(UniqueIdentifierGenerator.generateId("Test Agent $i"))
            def agent = new HTTPAgent("Test Agent $i")
                    .setId(agentAssetIds[i-1])
                    .setBaseURI("https://google.co.uk")
                    .setRealm(MASTER_REALM)
                    .setCreatedOn(Date.from(timerService.getNow()))
            agent.path = (String[])[agentAssetIds[i-1]].toArray(new String[0])

            agentAssets.add(agent)

            assetIds.add(UniqueIdentifierGenerator.generateId("Test Building $i"))

            // Add assets out of order to test gateway connector re-ordering logic
            IntStream.rangeClosed(1, 4).forEach{j ->

                assetIds.add(UniqueIdentifierGenerator.generateId("Test Building $i Room $j"))

                def roomAsset = new RoomAsset("Test Building $i Room $j")
                    .setId(assetIds[(i-1)*5+j])
                    .setCreatedOn(Date.from(timerService.getNow()))
                    .setParentId(assetIds[(i-1)*5])
                    .setRealm(MASTER_REALM)

                roomAsset.path = (String[])[assetIds[(i-1)*5+j], assetIds[(i-1)*5]].toArray(new String[0])

                roomAsset.addOrReplaceAttributes(
                    new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10,11)).addOrReplaceMeta(
                        new MetaItem<>(ACCESS_PUBLIC_READ)
                    ),
                    new Attribute<>("temp", NUMBER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[i-1])
                            .setPath("")),
                        new MetaItem<>(UNITS, units(UNITS_CELSIUS))
                    ),
                    new Attribute<>("tempSetpoint", NUMBER).addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[i-1])
                            .setPath("")),
                        new MetaItem<>(UNITS, units(UNITS_CELSIUS))
                    )
                )

                assets.add(roomAsset)
            }

            def buildingAsset = new BuildingAsset("Test Building $i")
                .setId(assetIds[(i-1)*5])
                .setCreatedOn(Date.from(timerService.getNow()))
                .setRealm(MASTER_REALM)

            buildingAsset.path = (String[])[assetIds[(i-1)*5]].toArray(new String[0])

            buildingAsset.addOrReplaceAttributes(
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10,11)).addMeta(
                    new MetaItem<>(ACCESS_PUBLIC_READ)
                )
            )

            assets.add(buildingAsset)
        }

        and: "the gateway client replies to the central manager with the assets of the gateway"
        List<Asset> sendAssets = []
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(assets)
        def readAssetsReplyEvent = new EventRequestResponseWrapper(
            GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL,
            new AssetsEvent(sendAssets))
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the central manager should have requested the full loading of the first batch of assets"
        String messageId = null
        ReadAssetsEvent readAssetsEvent = null
        conditions.eventually {
            assert clientReceivedMessages.size() == 1
            assert clientReceivedMessages.get(0).startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(0).contains("read-assets")
            def response = ValueUtil.JSON.readValue(clientReceivedMessages[0].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            messageId = response.messageId
            readAssetsEvent = response.event as ReadAssetsEvent
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + "0"
            assert readAssetsEvent.assetQuery != null
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
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the central manager should have added the first batch of assets under the gateway asset"
        conditions.eventually {
            def syncedAssets = assetStorageService.findAll(new AssetQuery().parents(gateway.getId()).recursive(true))
            assert syncedAssets.size() == sendAssets.size()
            assert syncedAssets.stream().filter{syncedAsset -> sendAssets.stream().anyMatch{mapAssetId(gateway.getId(), it.id, false) == syncedAsset.id}}.count() == sendAssets.size()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[0]}.getName() == "Test Agent 1"
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[0]}.getType() == HTTPAgent.DESCRIPTOR.name
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[0]}.getRealm() == managerTestSetup.realmBuildingName
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[0]}.getParentId() == gateway.getId()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[4]}.getName() == "Test Agent 5"
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[4]}.getType() == HTTPAgent.DESCRIPTOR.name
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[4]}.getRealm() == managerTestSetup.realmBuildingName
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[4]}.getParentId() == gateway.getId()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[0]}.getName() == "Test Building 1"
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[0]}.getType() == BuildingAsset.DESCRIPTOR.name
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[0]}.getRealm() == managerTestSetup.realmBuildingName
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[0]}.getParentId() == gateway.getId()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[0]}.getLocation().get().x == 10
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[0]}.getLocation().get().y == 11
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[0]}.getAttribute(Asset.LOCATION).flatMap{it.getMetaItem(ACCESS_PUBLIC_READ)}.isPresent()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[1]}.getName() == "Test Building 1 Room 1"
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[1]}.getType() == RoomAsset.DESCRIPTOR.getName()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[1]}.getRealm() == managerTestSetup.realmBuildingName
            assert mapAssetId(gateway.getId(), syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[1]}.getParentId(), true) == assetIds[0]
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[1]}.getAttribute("temp").map{it.hasMeta(AGENT_LINK)}.orElse(false)
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[1]}.getAttribute("tempSetpoint").map{it.hasMeta(AGENT_LINK)}.orElse(false)
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[5]}.getName() == "Test Building 2"
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[5]}.getType() == BuildingAsset.DESCRIPTOR.getName()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[5]}.getRealm() == managerTestSetup.realmBuildingName
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[5]}.getParentId() == gateway.getId()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[5]}.getLocation().get().x == 10
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[5]}.getLocation().get().y == 11
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[5]}.getAttribute(Asset.LOCATION).flatMap{it.getMetaItem(ACCESS_PUBLIC_READ)}.isPresent()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[6]}.getName() == "Test Building 2 Room 1"
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[6]}.getType() == RoomAsset.DESCRIPTOR.getName()
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[6]}.getRealm() == managerTestSetup.realmBuildingName
            assert mapAssetId(gateway.getId(), syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[6]}.getParentId(), true) == assetIds[5]
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[6]}.getAttribute("temp").map{it.hasMeta(AGENT_LINK)}.orElse(false)
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[6]}.getAttribute("tempSetpoint").map{it.hasMeta(AGENT_LINK)}.orElse(false)
        }

        and: "the central manager should have requested the full loading of the second batch of assets"
        conditions.eventually {
            assert clientReceivedMessages.size() == 2
            assert clientReceivedMessages.get(1).startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(1).contains("read-assets")
            def response = ValueUtil.JSON.readValue(clientReceivedMessages[1].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            messageId = response.messageId
            readAssetsEvent = response.event as ReadAssetsEvent
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert readAssetsEvent.assetQuery != null
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
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the gateway asset status should become connected"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        and: "all the gateway assets should be replicated underneath the gateway"
        assert assetStorageService.findAll(new AssetQuery().parents(gateway.getId()).recursive(true)).size() == agentAssets.size() + assets.size()

        and: "the http client protocol of the gateway agents should not have been linked to the central manager"
        Thread.sleep(500)
        conditions.eventually {
            assert !agentService.protocolInstanceMap.containsKey(agentAssetIds[0])
            assert !agentService.agents.containsKey(agentAssetIds[0])
            assert !agentService.protocolInstanceMap.containsKey(agentAssetIds[4])
            assert !agentService.agents.containsKey(agentAssetIds[4])
        }

        when: "the previously received messages are cleared"
        clientReceivedMessages.clear()

        and: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "an attribute event for a gateway descendant asset (building 1 room 1) is sent to the local manager"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(mapAssetId(gateway.id, assetIds[1], false), "tempSetpoint", 20d))

        then: "the event should have been forwarded to the gateway"
        conditions.eventually {
            assert clientReceivedMessages.size() == 1
            assert clientReceivedMessages.get(0).startsWith(SharedEvent.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(0).contains("attribute")
        }

        when: "the gateway handles the forwarded attribute event and sends a follow up attribute event to the local manager"
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AttributeEvent(assetIds[1], "tempSetpoint", 20d)).get())

        then: "the descendant asset in the local manager should contain the new attribute value"
        conditions.eventually {
            def building1Room1Asset = assetStorageService.find(mapAssetId(gateway.id, assetIds[1], false))
            assert building1Room1Asset.getAttribute("tempSetpoint").flatMap {it.getValue()}.orElse(0d) == 20d
        }

        when: "an asset is added on the gateway and the local manager is notified"
        def building1Room5AssetId = UniqueIdentifierGenerator.generateId("Test Building 1 Room 5")
        def building1Room5Asset = new RoomAsset("Test Building 1 Room 5")
            .setId(building1Room5AssetId)
            .setCreatedOn(Date.from(timerService.getNow()))
            .setParentId(assetIds[0])
            .setRealm(MASTER_REALM)
        building1Room5Asset.path = (String[])[building1Room5AssetId, assetIds[0]]

        building1Room5Asset.addOrReplaceAttributes(
            new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10,11)).addOrReplaceMeta(
                new MetaItem<>(ACCESS_PUBLIC_READ)
            ),
            new Attribute<>("temp", NUMBER).addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[0])
                    .setPath("")),
                new MetaItem<>(UNITS, units(UNITS_CELSIUS))
            ),
            new Attribute<>("tempSetpoint", NUMBER).addMeta(
                new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[0])
                    .setPath("")),
                new MetaItem<>(UNITS, units(UNITS_CELSIUS))
            )
        )

        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.CREATE, building1Room5Asset, null)).get())

        then: "the asset should be replicated in the local manager"
        RoomAsset localBuilding1Room5Asset
        conditions.eventually {
            localBuilding1Room5Asset = assetStorageService.find(mapAssetId(gateway.id, building1Room5AssetId, false)) as RoomAsset
            assert localBuilding1Room5Asset != null
            assert localBuilding1Room5Asset.getName() == "Test Building 1 Room 5"
            assert localBuilding1Room5Asset.getType() == RoomAsset.DESCRIPTOR.getName()
            assert localBuilding1Room5Asset.getRealm() == managerTestSetup.realmBuildingName
            assert localBuilding1Room5Asset.getParentId() == mapAssetId(gateway.id, assetIds[0], false)
            assert localBuilding1Room5Asset.getAttributes().size() == 6
        }

        when: "an asset is modified on the gateway and the local manager is notified"
        building1Room5Asset.setName("Test Building 1 Room 5 Updated")
        building1Room5Asset.setVersion(1)
        building1Room5Asset.addAttributes(
            new Attribute<>("co2Level", POSITIVE_INTEGER, 500)
                .addMeta(
                    new MetaItem<>(UNITS, units(UNITS_PART_PER_MILLION)),
                    new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[0]).setPath(""))
                )
        )
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.UPDATE, building1Room5Asset, (String[]) ["name", "attributes"].toArray(new String[0]))).get())

        then: "the asset should also be updated in the local manager"
        conditions.eventually {
            localBuilding1Room5Asset = assetStorageService.find(mapAssetId(gateway.id, building1Room5AssetId, false))
            assert localBuilding1Room5Asset != null
            assert localBuilding1Room5Asset.getVersion() == 1
            assert localBuilding1Room5Asset.getName() == "Test Building 1 Room 5 Updated"
            assert localBuilding1Room5Asset.getType() == RoomAsset.DESCRIPTOR.getName()
            assert localBuilding1Room5Asset.getRealm() == managerTestSetup.realmBuildingName
            assert localBuilding1Room5Asset.getParentId() == mapAssetId(gateway.id, assetIds[0], false)
            assert localBuilding1Room5Asset.getAttributes().size() == 7
            assert localBuilding1Room5Asset.getAttribute("co2Level").flatMap{it.getValue()}.orElse(0i) == 500i
        }

        when: "an asset is deleted on the gateway and the local manager is notified"
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.DELETE, building1Room5Asset, null)).get())

        then: "the asset should also be deleted in the local manager"
        conditions.eventually {
            def asset = assetStorageService.find(mapAssetId(gateway.id, building1Room5AssetId, false))
            assert asset == null
        }

        when: "the client received messages are cleared"
        clientReceivedMessages.clear()

        and: "an asset is added under the gateway in the local manager and the gateway responds that it successfully added the asset"
        def responseFuture = new AtomicReference<ScheduledFuture>()
        responseFuture.set(executorService.scheduleAtFixedRate({
            if (!clientReceivedMessages.isEmpty()) {
                def assetAddEvent = ValueUtil.JSON.readValue(clientReceivedMessages[0].substring(SharedEvent.MESSAGE_PREFIX.length()), AssetEvent.class)
                if (assetAddEvent.cause == AssetEvent.Cause.CREATE && assetAddEvent.asset.id == building1Room5AssetId) {
                    gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.CREATE, building1Room5Asset, null)).get())
                    responseFuture.get().cancel(false)
                }
            }
        }, 100, 100, TimeUnit.MILLISECONDS))
        assetStorageService.merge(localBuilding1Room5Asset)
        localBuilding1Room5Asset = assetStorageService.find(mapAssetId(gateway.id, building1Room5AssetId, false)) // Re-fetch asset as modifying instance returned by merge will cause concurrency issues

        then: "the asset should have been added to the gateway and eventually replicated in the local manager"
        assert localBuilding1Room5Asset != null
        assert assetStorageService.find(mapAssetId(gateway.id, building1Room5AssetId, false)).version == localBuilding1Room5Asset.version

        when: "the client received messages are cleared"
        clientReceivedMessages.clear()

        and: "an asset is modified under the gateway in the local manager and the gateway responds that it successfully updated the asset"
        localBuilding1Room5Asset.setName("Test Building 1 Room 5")
        localBuilding1Room5Asset.getAttributes().remove("co2Level")
        responseFuture.set(executorService.scheduleAtFixedRate({
            if (!clientReceivedMessages.isEmpty()) {
                def assetAddEvent = ValueUtil.JSON.readValue(clientReceivedMessages[0].substring(SharedEvent.MESSAGE_PREFIX.length()), AssetEvent.class)
                if (assetAddEvent.cause == AssetEvent.Cause.UPDATE && assetAddEvent.asset.id == building1Room5AssetId) {
                    building1Room5Asset.setName(localBuilding1Room5Asset.name)
                    building1Room5Asset.getAttributes().remove("co2Level")
                    gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.UPDATE, building1Room5Asset, null)).get())
                    responseFuture.get().cancel(false)
                }
            }
        }, 100, 100, TimeUnit.MILLISECONDS))
        localBuilding1Room5Asset = assetStorageService.merge(localBuilding1Room5Asset)
        def version = localBuilding1Room5Asset.version
        localBuilding1Room5Asset = assetStorageService.find(mapAssetId(gateway.id, building1Room5AssetId, false)) // Re-fetch asset as modifying instance returned by merge will cause concurrency issues

        then: "the asset should also be updated in the local manager"
        assert localBuilding1Room5Asset != null
        assert localBuilding1Room5Asset.getVersion() == 2
        assert localBuilding1Room5Asset.getName() == "Test Building 1 Room 5"
        assert localBuilding1Room5Asset.getType() == RoomAsset.DESCRIPTOR.getName()
        assert localBuilding1Room5Asset.getRealm() == managerTestSetup.realmBuildingName
        assert localBuilding1Room5Asset.getParentId() == mapAssetId(gateway.id, assetIds[0], false)
        assert localBuilding1Room5Asset.getAttributes().size() == 6
        assert localBuilding1Room5Asset.version == version

        when: "the client received messages are cleared"
        clientReceivedMessages.clear()

        and: "an asset is deleted under the gateway in the local manager and the gateway responds that it successfully deleted the asset"
        responseFuture.set(executorService.scheduleAtFixedRate({
            if (!clientReceivedMessages.isEmpty()) {
                def request = ValueUtil.JSON.readValue(clientReceivedMessages[0].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
                def deleteRequest = request.event as DeleteAssetsRequestEvent
                if (deleteRequest.assetIds.size() == 1 && deleteRequest.assetIds.get(0) == building1Room5AssetId) {
                    gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + ValueUtil.asJSON(new EventRequestResponseWrapper(request.messageId, new DeleteAssetsResponseEvent(true, deleteRequest.assetIds))).get())
                    gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.DELETE, building1Room5Asset, null)).get())
                    responseFuture.get().cancel(false)
                }
            }
        }, 100, 100, TimeUnit.MILLISECONDS))
        def deleted = assetStorageService.delete([mapAssetId(gateway.id, building1Room5AssetId, false)])

        then: "the asset should have been deleted"
        assert deleted
        conditions.eventually {
            assert assetStorageService.find(mapAssetId(gateway.id, building1Room5AssetId, false)) == null
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "the gateway asset is marked as disabled"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(gateway.getId(), GatewayAsset.DISABLED, true))

        then: "the gateway asset status should become disabled"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId(), true)
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.DISABLED
        }

        and: "the gateway connector should be marked as disconnected and the gateway client should have been disconnected"
        conditions.eventually {
            assert gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT)).disabled
            assert !gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT)).connected
            assert gatewayClient.connectionStatus == ConnectionStatus.CONNECTING
        }

        and: "the central manager should have sent a disconnect event to the client"
        conditions.eventually {
            assert clientReceivedMessages.last().contains("gateway-disconnect")
        }
        gatewayClient.disconnect()

        when: "an attempt is made to add a descendant asset to a gateway that isn't connected"
        def failedAsset = new BuildingAsset("Failed Asset")
            .setId(UniqueIdentifierGenerator.generateId("Failed asset"))
            .setCreatedOn(Date.from(timerService.getNow()))
            .setParentId(gateway.id)
            .setRealm(managerTestSetup.realmBuildingName)
        failedAsset.path = (String[])[UniqueIdentifierGenerator.generateId("Failed asset")].toArray(new String[0])
        failedAsset.addOrReplaceAttributes(
            new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10,11)).addMeta(
                    new MetaItem<>(ACCESS_PUBLIC_READ)
            )
        )
        assetStorageService.merge(failedAsset)

        then: "an error should occur"
        thrown(IllegalStateException)

        when: "gateway assets are modified whilst the gateway is disconnected (building1Room5 re-added, building5 and descendants removed and building1 modified and building1Room1 attribute updated)"
        assets[4].setName("Test Building 1 Updated")
        assets[4].setVersion(2L)
        assets[0].getAttribute("temp").ifPresent{it.setValue(10)}
        assets = assets.subList(0, 20)

        and: "the client received messages are cleared"
        clientReceivedMessages.clear()

        and: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "the gateway is enabled again"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(gateway.getId(), GatewayAsset.DISABLED.getName(), false))

        then: "the gateway connector should be enabled"
        conditions.eventually {
            assert !gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT)).disabled
        }

        when: "the gateway asset client secret attribute is updated"
        def newSecret = UUID.randomUUID().toString()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(gateway.getId(), GatewayAsset.CLIENT_SECRET, newSecret))

        then: "the service user secret should be updated"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId()) as GatewayAsset
            assert gateway.getClientSecret().orElse(null) == newSecret
            def user = identityProvider.getUserByUsername(gateway.getRealm(), User.SERVICE_ACCOUNT_PREFIX + gateway.getClientId().orElse(""))
            assert user != null
            assert user.secret == newSecret
        }

        when: "the gateway client reconnects with the new secret"
        gatewayClient = new WebsocketIOClient<String>(
                new URIBuilder("ws://127.0.0.1:$serverPort/websocket/events?Realm=$managerTestSetup.realmBuildingName").build(),
                null,
                new OAuthClientCredentialsGrant("http://127.0.0.1:$serverPort/auth/realms/$managerTestSetup.realmBuildingName/protocol/openid-connect/token",
                        gateway.getClientId().orElse(""),
                        gateway.getClientSecret().orElse(""),
                        null).setBasicAuthHeader(true))
        gatewayClient.setEncoderDecoderProvider({
            [new AbstractNettyIOClient.MessageToMessageDecoder<String>(String.class, gatewayClient)].toArray(new ChannelHandler[0])
        })
        gatewayClient.addMessageConsumer({
            message -> clientReceivedMessages.add(message)
        })
        gatewayClient.addConnectionStatusConsumer({
            status -> connectionStatus = status
        })
        gatewayClient.connect()

        then: "the gateway netty client status should become CONNECTED"
        conditions.eventually {
            assert connectionStatus == ConnectionStatus.CONNECTED
            assert gatewayClient.connectionStatus == ConnectionStatus.CONNECTED
        }

        and: "the gateway asset connection status should be CONNECTING"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTING
        }

        and: "the local manager should have sent an asset read request"
        conditions.eventually {
            assert clientReceivedMessages.size() >= 1
            assert clientReceivedMessages.find {it.startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX) && it.contains("read-assets")} != null
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
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the central manager should have requested the full loading of the first batch of assets"
        conditions.eventually {
            assert clientReceivedMessages.size() == 1
            assert clientReceivedMessages.get(0).startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(0).contains("read-assets")
            def request = ValueUtil.JSON.readValue(clientReceivedMessages[0].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            messageId = request.messageId
            readAssetsEvent = request.event as ReadAssetsEvent
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + "0"
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert agentAssetIds.stream().filter{readAssetsEvent.assetQuery.ids.contains(it)}.count() == agentAssetIds.size()
            assert assetIds.stream().filter{readAssetsEvent.assetQuery.ids.contains(it)}.count() + 1 == GatewayConnector.SYNC_ASSET_BATCH_SIZE - agentAssetIds.size()
        }

        when: "another asset is added to the gateway during the initial sync process"
        def building2Room5Asset = new RoomAsset("Test Building 2 Room 5")
            .setId(UniqueIdentifierGenerator.generateId("Test Building 2 Room 5"))
            .setCreatedOn(Date.from(timerService.getNow()))
            .setParentId(assetIds[5])
            .setRealm(MASTER_REALM)
        building2Room5Asset.path = (String[])[UniqueIdentifierGenerator.generateId("Test Building 2 Room 5"), assetIds[5]]

        building2Room5Asset.addOrReplaceAttributes(
            new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10,11)).addOrReplaceMeta(
                new MetaItem<>(ACCESS_PUBLIC_READ)
            ),
            new Attribute<>("temp", NUMBER).addOrReplaceMeta(
                new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[1])
                    .setPath("")),
                new MetaItem<>(UNITS, units(UNITS_CELSIUS))
            ),
            new Attribute<>("tempSetpoint", NUMBER).addMeta(
                new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[1])
                    .setPath("")),
                new MetaItem<>(UNITS, units(UNITS_CELSIUS))
            )
        )

        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.CREATE, building2Room5Asset, null)).get())

        and: "another asset is deleted from the gateway during the initial sync process (Building 3 Room 1)"
        def removedAsset = assets.remove(10)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.DELETE, removedAsset, null)).get())

        and: "the gateway returns the requested assets (minus the deleted Building 3 Room 1 asset)"
        sendAssets = [building1Room5Asset]
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).filter{!agentAssetIds.contains(it) && it != removedAsset.id && it != building1Room5Asset.id}.map{id -> assets.stream().filter{it.id == id}.findFirst().orElse(null)}.collect(Collectors.toList()))
        readAssetsReplyEvent = new EventRequestResponseWrapper(messageId, new AssetsEvent(sendAssets))
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the central manager should have requested the full loading of the second batch of assets"
        conditions.eventually {
            assert clientReceivedMessages.size() == 2
            assert clientReceivedMessages.get(1).startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)
            assert clientReceivedMessages.get(1).contains("read-assets")
            def request = ValueUtil.JSON.readValue(clientReceivedMessages[1].substring(EventRequestResponseWrapper.MESSAGE_PREFIX.length()), EventRequestResponseWrapper.class)
            messageId = request.messageId
            readAssetsEvent = request.event as ReadAssetsEvent
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + (GatewayConnector.SYNC_ASSET_BATCH_SIZE - 1)
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == agentAssetIds.size() + assets.size() + 1 - GatewayConnector.SYNC_ASSET_BATCH_SIZE + 1
            assert assets.stream().filter{asset -> sendAssets.stream().noneMatch{it.id == asset.id}}.count() == readAssetsEvent.assetQuery.ids.length
        }

        when: "the gateway returns the requested assets"
        sendAssets = []
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).map{id -> assets.stream().filter{asset -> asset.id == id}.findFirst().orElse(null)}.collect(Collectors.toList()))
        readAssetsReplyEvent = new EventRequestResponseWrapper(messageId, new AssetsEvent(sendAssets))
        gatewayClient.sendMessage(EventRequestResponseWrapper.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the gateway asset status should become connected"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        and: "the gateway should have the correct assets"
        def gatewayAssets = assetStorageService.findAll(new AssetQuery().parents(gateway.getId()).recursive(true))
        assert gatewayAssets.size() == 2 + agentAssets.size() + assets.size()

        when: "the gateway asset is deleted"
        deleted = assetStorageService.delete([gateway.id])

        then: "all descendant assets should have been removed"
        conditions.eventually {
            assert assetStorageService.find(mapAssetId(gateway.id, assets[0].id, true)) == null
        }

        then: "the keycloak client should also be removed"
        assert deleted
        conditions.eventually {
            assert identityProvider.getClient(managerTestSetup.realmBuildingName, getGatewayClientId(gateway.getId())) == null
        }

        cleanup: "cleanup the gateway client"
        if (gatewayClient != null) {
            gatewayClient.disconnect()
            gatewayClient.removeAllMessageConsumers()
        }
    }

    def "Verify gateway client service"() {

        given: "the container environment is started with the spy gateway client service"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)
        def services = Lists.newArrayList(defaultServices())
        def container = startContainer(defaultConfig(), services)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def gatewayService = container.getService(GatewayService.class)
        def gatewayClientService = container.getService(GatewayClientService.class)
        def agentService = container.getService(AgentService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def clientEventService = container.getService(ClientEventService.class)

        and: "an authenticated admin user"
        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token


        and: "the gateway client resource"
        def gatewayClientResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(GatewayClientResource.class)

        expect: "the system should settle down"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 300)
        }

        when: "a gateway is provisioned in this manager in the building realm"
        GatewayAsset gateway = assetStorageService.merge(new GatewayAsset("Test gateway").setRealm(managerTestSetup.realmBuildingName))

        then: "a set of credentials should have been created for this gateway and be stored against the gateway for easy reference"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId(), true) as GatewayAsset
            assert !isNullOrEmpty(gateway.getClientId().orElse(null))
            assert !isNullOrEmpty(gateway.getClientSecret().orElse(null))
        }

        and: "a gateway connector should have been created for this gateway"
        conditions.eventually {
            assert gatewayService.gatewayConnectorMap.size() == 1
            assert gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT)).gatewayId == gateway.getId()
        }

        when: "a gateway client connection is created to connect the city realm to the gateway in the building realm"
        def gatewayConnection = new GatewayConnection(
            "127.0.0.1",
            serverPort,
            managerTestSetup.realmBuildingName,
            gateway.getAttribute("clientId").flatMap{it.getValue()}.orElse(""),
            gateway.getAttribute("clientSecret").flatMap{it.getValue()}.orElse(""),
            false,
            false
        )
        gatewayClientResource.setConnection(null, managerTestSetup.realmCityName, gatewayConnection)

        then: "the gateway client should become connected"
        conditions.eventually {
            assert gatewayClientService.clientRealmMap.get(managerTestSetup.realmCityName) != null
        }

        and: "the gateway asset connection status should become CONNECTED"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        and: "the assets should have been created under the gateway asset"
        conditions.eventually {
            def gatewayAssets = assetStorageService.findAll(new AssetQuery().parents(gateway.id).recursive(true))
            def cityAssets = assetStorageService.findAll(new AssetQuery().realm(new RealmPredicate(managerTestSetup.realmCityName)))
            assert gatewayAssets.size() == cityAssets.size()
        }

        when: "a gateway client asset is modified"
        MicrophoneAsset microphone1 = assetStorageService.find(managerTestSetup.microphone1Id)
        microphone1.setName("Microphone 1 Updated")
        microphone1.addAttributes(
            new Attribute<>("test", NUMBER, 100d)
        )
        microphone1 = assetStorageService.merge(microphone1)

        then: "the mirrored asset under the gateway should have also been updated"
        conditions.eventually {
            def mirroredMicrophone = assetStorageService.find(mapAssetId(gateway.id, managerTestSetup.microphone1Id, false))
            assert mirroredMicrophone != null
            assert mirroredMicrophone.getAttributes().get(MicrophoneAsset.SOUND_LEVEL).flatMap{it.getMetaItem(READ_ONLY)}.flatMap{it.getValue()}.orElse(false)
            assert mirroredMicrophone.getAttribute("test").isPresent()
            assert mirroredMicrophone.getAttribute("test").flatMap{it.getValue()}.orElse(0d) == 100d
        }

        when: "a gateway client asset is added"
        MicrophoneAsset microphone2 = new MicrophoneAsset("Microphone 2")
            .setRealm(managerTestSetup.realmCityName)
            .setParentId(managerTestSetup.area1Id)
            .addAttributes(
                new Attribute<>("test", TEXT, "testValue")
            )
        microphone2 = assetStorageService.merge(microphone2)

        then: "the new asset should have been created in the gateway and also mirrored under the gateway asset"
        assert microphone2.id != null
        conditions.eventually {
            def mirroredMicrophone2 = assetStorageService.find(mapAssetId(gateway.id, microphone2.id, false))
            assert mirroredMicrophone2 != null
            assert mirroredMicrophone2.getAttributes().get(MicrophoneAsset.SOUND_LEVEL).isPresent()
            assert mirroredMicrophone2.getAttribute("test").flatMap{it.getValue()}.orElse("") == "testValue"
        }

        when: "an asset is added under the gateway asset"
        def microphone3 = ValueUtil.clone(microphone1)
            .setName("Microphone 3")
            .setId(null)
            .setRealm(managerTestSetup.realmBuildingName)
            .setParentId(mapAssetId(gateway.id, managerTestSetup.area1Id, false))
        microphone3 = assetStorageService.merge(microphone3)

        then: "the new asset should have been created in the gateway and also mirrored under the gateway asset"
        assert microphone3.id != null
        conditions.eventually {
            def gatewayMicrophone3 = assetStorageService.find(mapAssetId(gateway.id, microphone3.id, true))
            assert gatewayMicrophone3 != null
            assert gatewayMicrophone3.getAttribute(MicrophoneAsset.SOUND_LEVEL).isPresent()
        }

        and: "the new asset microphone level should be correctly linked to the simulator protocol only on the gateway"
        conditions.eventually {
            assert !((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.agentId)).linkedAttributes.containsKey(new AttributeRef(microphone3.id, MicrophoneAsset.SOUND_LEVEL.name))
            assert ((SimulatorProtocol)agentService.getProtocolInstance(managerTestSetup.smartCityServiceAgentId)).linkedAttributes.containsKey(new AttributeRef(mapAssetId(gateway.id, microphone3.id, true), MicrophoneAsset.SOUND_LEVEL.name))
        }

        when: "an attribute is updated on the gateway client"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(microphone2.id, "test", "newValue"))

        then: "the mirrored asset attribute should also be updated"
        conditions.eventually {
            def mirroredMicrophone2 = assetStorageService.find(mapAssetId(gateway.id, microphone2.id, false))
            assert mirroredMicrophone2 != null
            assert mirroredMicrophone2.getAttribute("test").flatMap{it.getValue()}.orElse("") == "newValue"
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "a mirrored asset attribute is updated"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(mapAssetId(gateway.id, microphone2.id, false), "test", "newerValue"))

        then: "the attribute should be updated on the gateway client and the mirrored asset"
        conditions.eventually {
            microphone2 = assetStorageService.find(microphone2.id)
            def mirroredMicrophone2 = assetStorageService.find(mapAssetId(gateway.id, microphone2.id, false))
            assert microphone2 != null
            assert mirroredMicrophone2 != null
            assert microphone2.getAttribute("test").flatMap{it.getValue()}.orElse("") == "newerValue"
            assert mirroredMicrophone2.getAttribute("test").flatMap{it.getValue()}.orElse("") == "newerValue"
        }

        when: "we subscribe to attribute events"
        List<AttributeEvent> attributeEvents = []
        clientEventService.addInternalSubscription(AttributeEvent.class, null, {attributeEvent ->
            attributeEvents.add(attributeEvent)
        })

        and: "an attribute with an attribute link is updated on the gateway"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.light2Id, LightAsset.ON_OFF, true))

        then: "only four attribute events should have been generated, one for each of light1 and light2 and one for each of their mirrored assets below the gateway asset"
        Thread.sleep(1000)
        conditions.eventually {
            assert attributeEvents.size() == 4
            assert attributeEvents.any{it.id == managerTestSetup.light2Id && it.name == LightAsset.ON_OFF.name && it.value.orElse(false)}
            assert attributeEvents.any{it.id == managerTestSetup.light1Id && it.name == LightAsset.ON_OFF.name && it.value.orElse(false)}
            assert attributeEvents.any{it.id == mapAssetId(gateway.id, managerTestSetup.light2Id, false) && it.name == LightAsset.ON_OFF.name && it.value.orElse(false)}
            assert attributeEvents.any{it.id == mapAssetId(gateway.id, managerTestSetup.light1Id, false) && it.name == LightAsset.ON_OFF.name && it.value.orElse(false)}
        }

        when: "a gateway client asset is deleted"
        def deleted = assetStorageService.delete(Collections.singletonList(managerTestSetup.microphone1Id))

        then: "the client asset should have been deleted and also the mirrored asset under the gateway should also be deleted"
        assert deleted
        conditions.eventually {
            assert assetStorageService.find(mapAssetId(gateway.id, managerTestSetup.microphone1Id, false)) == null
        }
    }
}
