package org.openremote.test.gateway


import io.netty.channel.ChannelHandler
import jakarta.ws.rs.ForbiddenException
import org.apache.http.client.utils.URIBuilder
import org.openremote.agent.protocol.http.HTTPAgent
import org.openremote.agent.protocol.http.HTTPAgentLink
import org.openremote.agent.protocol.io.AbstractNettyIOClient
import org.openremote.agent.protocol.websocket.WebsocketIOClient
import org.openremote.container.timer.TimerService
import org.openremote.container.web.WebTargetBuilder
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.gateway.*
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.security.ManagerKeycloakIdentityProvider
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetEvent
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.AssetsEvent
import org.openremote.model.asset.ReadAssetsEvent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.*
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.auth.OAuthClientCredentialsGrant
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.gateway.*
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.model.security.User
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.ValueFormat
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
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
            gateway = assetStorageService.find(gateway.getId(), true) as GatewayAsset
            assert !isNullOrEmpty(gateway.getClientId().orElse(""))
            assert !isNullOrEmpty(gateway.getClientSecret().orElse(""))
        }

        and: "a gateway connector should have been created for this gateway"
        conditions.eventually {
            assert gatewayService.gatewayConnectorMap.size() == 1
            assert gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT)).gatewayId == gateway.getId()
        }

        when: "the gateway service user credentials are used to try and access the asset resources"
        def accessToken = authenticate(
            container,
            managerTestSetup.realmBuildingName,
            gateway.getClientId().orElse(""),
            gateway.getClientSecret().orElse("")
        ).token
        def assetResource = getClientApiTarget(serverUri(serverPort), managerTestSetup.realmBuildingName, accessToken).proxy(AssetResource.class)

        and: "the realm assets are requested"
        def realmAssets = assetResource.queryAssets(null, null)

        then: "an exception should be thrown"
        thrown(ForbiddenException.class)

        when: "the Gateway client is created"
        def gatewayClient = new GatewayIOClient(
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

        then: "the gateway netty client status should become CONNECTING"
        conditions.eventually {
            assert connectionStatus == ConnectionStatus.CONNECTING
            assert gatewayClient.connectionStatus == ConnectionStatus.CONNECTING
        }

        and: "the gateway asset connection status should be CONNECTING"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId()) as GatewayAsset
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTING
        }

        and: "the server should have sent an asset read request"
        conditions.eventually {
            assert clientReceivedMessages.size() >= 1
            def request = ValueUtil.JSON.readValue(clientReceivedMessages[0].substring(SharedEvent.MESSAGE_PREFIX.length()), ReadAssetsEvent.class)
            assert request.messageID == GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL
            assert request.assetQuery != null
            assert request.assetQuery.recursive
        }

        when: "the previously received messages are cleared"
        clientReceivedMessages.clear()

        and: "the gateway client assets are defined"
        List<String> agentAssetIds = []
        List<HTTPAgent> agentAssets = []
        List<String> assetIds = []
        List<Asset> assets = []

        IntStream.rangeClosed(1, 5).forEach { i ->
            agentAssetIds.add(UniqueIdentifierGenerator.generateId("Test Agent $i"))
            def agent = new HTTPAgent("Test Agent $i")
                    .setId(agentAssetIds[i - 1])
                    .setBaseURI("https://google.co.uk")
                    .setRealm(MASTER_REALM)
                    .setCreatedOn(Date.from(timerService.getNow()))
            agent.path = (String[]) [agentAssetIds[i - 1]].toArray(new String[0])

            agentAssets.add(agent)

            assetIds.add(UniqueIdentifierGenerator.generateId("Test Building $i"))

            // Add assets out of order to test gateway connector re-ordering logic
            IntStream.rangeClosed(1, 4).forEach { j ->

                assetIds.add(UniqueIdentifierGenerator.generateId("Test Building $i Room $j"))

                def roomAsset = new RoomAsset("Test Building $i Room $j")
                        .setId(assetIds[(i - 1) * 5 + j])
                        .setCreatedOn(Date.from(timerService.getNow()))
                        .setParentId(assetIds[(i - 1) * 5])
                        .setRealm(MASTER_REALM)

                roomAsset.path = (String[]) [assetIds[(i - 1) * 5 + j], assetIds[(i - 1) * 5]].toArray(new String[0])

                roomAsset.addOrReplaceAttributes(
                        new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10, 11)).addOrReplaceMeta(
                                new MetaItem<>(ACCESS_PUBLIC_READ)
                        ),
                        new Attribute<>("temp", NUMBER).addOrReplaceMeta(
                                new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[i - 1])
                                        .setPath("")),
                                new MetaItem<>(UNITS, Constants.units(UNITS_CELSIUS))
                        ),
                        new Attribute<>("tempSetpoint", NUMBER).addMeta(
                                new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[i - 1])
                                        .setPath("")),
                                new MetaItem<>(UNITS, Constants.units(UNITS_CELSIUS))
                        )
                )

                assets.add(roomAsset)
            }

            def buildingAsset = new BuildingAsset("Test Building $i")
                    .setId(assetIds[(i - 1) * 5])
                    .setCreatedOn(Date.from(timerService.getNow()))
                    .setRealm(MASTER_REALM)

            buildingAsset.path = (String[]) [assetIds[(i - 1) * 5]].toArray(new String[0])

            buildingAsset.addOrReplaceAttributes(
                    new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10, 11)).addMeta(
                            new MetaItem<>(ACCESS_PUBLIC_READ)
                    )
            )

            assets.add(buildingAsset)
        }

        and: "the gateway client replies to the central manager with the assets of the gateway"
        List<Asset> sendAssets = []
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(assets)
        def readAssetsReplyEvent = new AssetsEvent(sendAssets)
        readAssetsReplyEvent.setMessageID(GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the central manager should have requested the full loading of the first batch of assets"
        String messageId = null
        ReadAssetsEvent readAssetsEvent = null
        conditions.eventually {
            assert clientReceivedMessages.size() == 1
            assert clientReceivedMessages.get(0).contains("read-assets")
            readAssetsEvent = ValueUtil.JSON.readValue(clientReceivedMessages[0].substring(SharedEvent.MESSAGE_PREFIX.length()), ReadAssetsEvent.class)
            messageId = readAssetsEvent.messageID
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + "0"
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert agentAssetIds.stream().filter { readAssetsEvent.assetQuery.ids.contains(it) }.count() == agentAssetIds.size()
            assert assetIds.stream().filter { readAssetsEvent.assetQuery.ids.contains(it) }.count() == GatewayConnector.SYNC_ASSET_BATCH_SIZE - agentAssetIds.size()
        }

        when: "the gateway returns the requested assets"
        sendAssets = []
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).filter { !agentAssetIds.contains(it) }.map { id -> assets.stream().filter { asset -> asset.id == id }.findFirst().orElse(null) }.collect(Collectors.toList()))
        readAssetsReplyEvent = new AssetsEvent(sendAssets)
        readAssetsReplyEvent.setMessageID(messageId)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the central manager should have added the first batch of assets under the gateway asset"
        conditions.eventually {
            def syncedAssets = assetStorageService.findAll(new AssetQuery().parents(gateway.getId()).recursive(true))
            assert syncedAssets.size() == sendAssets.size()
            assert syncedAssets.stream().filter { syncedAsset -> sendAssets.stream().anyMatch { mapAssetId(gateway.getId(), it.id, false) == syncedAsset.id } }.count() == sendAssets.size()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[0] }.getName() == "Test Agent 1"
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[0] }.getType() == HTTPAgent.DESCRIPTOR.name
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[0] }.getRealm() == managerTestSetup.realmBuildingName
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[0] }.getParentId() == gateway.getId()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[4] }.getName() == "Test Agent 5"
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[4] }.getType() == HTTPAgent.DESCRIPTOR.name
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[4] }.getRealm() == managerTestSetup.realmBuildingName
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == agentAssetIds[4] }.getParentId() == gateway.getId()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[0] }.getName() == "Test Building 1"
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[0] }.getType() == BuildingAsset.DESCRIPTOR.name
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[0] }.getRealm() == managerTestSetup.realmBuildingName
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[0] }.getParentId() == gateway.getId()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[0] }.getLocation().get().x == 10
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[0] }.getLocation().get().y == 11
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[0]}.getAttribute(Asset.LOCATION).flatMap{it.getMetaItem(ACCESS_PUBLIC_READ)}.isPresent()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[1] }.getName() == "Test Building 1 Room 1"
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[1] }.getType() == RoomAsset.DESCRIPTOR.getName()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[1] }.getRealm() == managerTestSetup.realmBuildingName
            assert mapAssetId(gateway.getId(), syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[1] }.getParentId(), true) == assetIds[0]
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[1]}.getAttribute("temp").map{it.hasMeta(AGENT_LINK)}.orElse(false)
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[1]}.getAttribute("tempSetpoint").map{it.hasMeta(AGENT_LINK)}.orElse(false)
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[5] }.getName() == "Test Building 2"
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[5] }.getType() == BuildingAsset.DESCRIPTOR.getName()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[5] }.getRealm() == managerTestSetup.realmBuildingName
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[5] }.getParentId() == gateway.getId()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[5] }.getLocation().get().x == 10
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[5] }.getLocation().get().y == 11
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[5]}.getAttribute(Asset.LOCATION).flatMap{it.getMetaItem(ACCESS_PUBLIC_READ)}.isPresent()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[6] }.getName() == "Test Building 2 Room 1"
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[6] }.getType() == RoomAsset.DESCRIPTOR.getName()
            assert syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[6] }.getRealm() == managerTestSetup.realmBuildingName
            assert mapAssetId(gateway.getId(), syncedAssets.find { mapAssetId(gateway.getId(), it.id, true) == assetIds[6] }.getParentId(), true) == assetIds[5]
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[6]}.getAttribute("temp").map{it.hasMeta(AGENT_LINK)}.orElse(false)
            assert syncedAssets.find {mapAssetId(gateway.getId(), it.id, true) == assetIds[6]}.getAttribute("tempSetpoint").map{it.hasMeta(AGENT_LINK)}.orElse(false)
        }

        and: "the central manager should have requested the full loading of the second batch of assets"
        conditions.eventually {
            assert clientReceivedMessages.size() == 2
            assert clientReceivedMessages.get(1).contains("read-assets")
            readAssetsEvent = ValueUtil.JSON.readValue(clientReceivedMessages[1].substring(SharedEvent.MESSAGE_PREFIX.length()), ReadAssetsEvent.class)
            messageId = readAssetsEvent.messageID
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == agentAssetIds.size() + assetIds.size() - GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert assetIds.stream().filter { id -> sendAssets.stream().noneMatch { it.id == id } }.count() == readAssetsEvent.assetQuery.ids.length
        }

        when: "the gateway returns the requested assets"
        sendAssets = []
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).map { id -> assets.stream().filter { asset -> asset.id == id }.findFirst().orElse(null) }.collect(Collectors.toList()))
        readAssetsReplyEvent = new AssetsEvent(sendAssets)
        readAssetsReplyEvent.setMessageID(messageId)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the gateway connector initial sync should be completed"
        conditions.eventually {
            def gatewayConnector = gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT))
            assert gatewayConnector.isConnected()
            assert !gatewayConnector.isInitialSyncInProgress()
        }

        and: "the central manager should have requested the capabilities of the gateway"
        conditions.eventually {
            assert clientReceivedMessages.size() == 3
            assert clientReceivedMessages.get(2).contains(GatewayCapabilitiesRequestEvent.TYPE)
            def gatewayCapabilitiesRequest = ValueUtil.JSON.readValue(clientReceivedMessages[2].substring(SharedEvent.MESSAGE_PREFIX.length()), GatewayCapabilitiesRequestEvent.class)
            messageId = gatewayCapabilitiesRequest.messageID
        }

        when: "the gateway returns the capabilities"
        def capabilitiesReplyEvent = new GatewayCapabilitiesResponseEvent(true)
        capabilitiesReplyEvent.setMessageID(messageId)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(capabilitiesReplyEvent).get())

        then: "the gateway connector capabilities should be updated"
        conditions.eventually {
            def gatewayConnector = gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT))
            assert gatewayConnector.isConnected()
            assert !gatewayConnector.isInitialSyncInProgress()
            assert gatewayConnector.isTunnellingSupported()
        }

        and: "the gateway client should now be CONNECTED"
        conditions.eventually {
            assert connectionStatus == ConnectionStatus.CONNECTED
            assert gatewayClient.connectionStatus == ConnectionStatus.CONNECTED
        }

        and: "the gateway asset connection status should be CONNECTED"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId()) as GatewayAsset
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        and: "all the gateway assets should be replicated underneath the gateway"
        conditions.eventually {
            assert assetStorageService.findAll(new AssetQuery().parents(gateway.getId()).recursive(true)).size() == agentAssets.size() + assets.size()
        }

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
            assert building1Room1Asset.getAttribute("tempSetpoint").flatMap { it.getValue() }.orElse(0d) == 20d
        }

        when: "an asset is added on the gateway and the local manager is notified"
        def building1Room5AssetId = UniqueIdentifierGenerator.generateId("Test Building 1 Room 5")
        def building1Room5Asset = new RoomAsset("Test Building 1 Room 5")
                .setId(building1Room5AssetId)
                .setCreatedOn(Date.from(timerService.getNow()))
                .setParentId(assetIds[0])
                .setRealm(MASTER_REALM)
        building1Room5Asset.path = (String[]) [building1Room5AssetId, assetIds[0]]

        building1Room5Asset.addOrReplaceAttributes(
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10, 11)).addOrReplaceMeta(
                        new MetaItem<>(ACCESS_PUBLIC_READ)
                ),
                new Attribute<>("temp", NUMBER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[0])
                                .setPath("")),
                        new MetaItem<>(UNITS, Constants.units(UNITS_CELSIUS))
                ),
                new Attribute<>("tempSetpoint", NUMBER).addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[0])
                                .setPath("")),
                        new MetaItem<>(UNITS, Constants.units(UNITS_CELSIUS))
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
                                new MetaItem<>(UNITS, Constants.units(UNITS_PART_PER_MILLION)),
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
            assert localBuilding1Room5Asset.getAttribute("co2Level").flatMap { it.getValue() }.orElse(0i) == 500i
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

        and: "an attempt is made to add a child asset to the gateway in the local manager"
        assetStorageService.merge(localBuilding1Room5Asset)

        then: "an exception should be thrown"
        thrown(IllegalStateException)

        when: "the client received messages are cleared"
        clientReceivedMessages.clear()

        and: "an attempt is made to modify a child asset of the gateway in the local manager"
        localBuilding1Room5Asset.setName("Test Building 1 Room 5")
        localBuilding1Room5Asset.getAttributes().remove("co2Level")
        localBuilding1Room5Asset = assetStorageService.merge(localBuilding1Room5Asset)

        then: "an exception should be thrown"
        thrown(IllegalStateException)

        when: "an attempt is made to delete a child asset of the gateway in the local manager"
        assetStorageService.delete([mapAssetId(gateway.id, assetIds[0], false)])

        then: "an exception should be thrown"
        thrown(IllegalStateException)

        then: "the asset should not have been deleted"
        assert assetStorageService.find(mapAssetId(gateway.id, assetIds[0], false)) != null

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "the gateway asset is marked as disabled"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(gateway.getId(), GatewayAsset.DISABLED, true))

        then: "the gateway asset status should become DISCONNECTED"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId(), true)
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.DISCONNECTED
        }

        and: "the gateway connector should be marked as disconnected and the gateway client should have been disconnected"
        conditions.eventually {
            assert gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT)).disabled
            assert !gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT)).connected
        }

        when: "an attempt is made to add a descendant asset to a gateway that isn't connected"
        gatewayClient.disconnect()
        def failedAsset = new BuildingAsset("Failed Asset")
                .setId(UniqueIdentifierGenerator.generateId("Failed asset"))
                .setCreatedOn(Date.from(timerService.getNow()))
                .setParentId(gateway.id)
                .setRealm(managerTestSetup.realmBuildingName)
        failedAsset.path = (String[]) [UniqueIdentifierGenerator.generateId("Failed asset")].toArray(new String[0])
        failedAsset.addOrReplaceAttributes(
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10, 11)).addMeta(
                        new MetaItem<>(ACCESS_PUBLIC_READ)
                )
        )
        assetStorageService.merge(failedAsset)

        then: "an error should occur"
        thrown(IllegalStateException)

        when: "gateway assets are modified whilst the gateway is disconnected (building1Room5 re-added, building5 and descendants removed and building1 modified and building1Room1 attribute updated)"
        assets[4].setName("Test Building 1 Updated")
        assets[4].setVersion(2L)
        assets[0].getAttribute("temp").ifPresent { it.setValue(10) }
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

        then: "the gateway netty client status should become CONNECTING"
        conditions.eventually {
            assert connectionStatus == ConnectionStatus.CONNECTING
            assert gatewayClient.connectionStatus == ConnectionStatus.CONNECTING
        }

        and: "the gateway asset connection status should be CONNECTING"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTING
        }

        and: "the local manager should have sent an asset read request"
        conditions.eventually {
            assert clientReceivedMessages.size() >= 1
            assert clientReceivedMessages.find { it.startsWith(SharedEvent.MESSAGE_PREFIX) && it.contains("read-assets") } != null
        }

        when: "the previously received messages are cleared"
        clientReceivedMessages.clear()

        and: "the gateway client replies to the central manager with the assets of the gateway"
        sendAssets = [building1Room5Asset]
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(assets)
        readAssetsReplyEvent = new AssetsEvent(sendAssets)
        readAssetsReplyEvent.setMessageID(GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the central manager should have requested the full loading of the first batch of assets"
        conditions.eventually {
            assert clientReceivedMessages.size() == 1
            assert clientReceivedMessages.get(0).contains("read-assets")
            readAssetsEvent = ValueUtil.JSON.readValue(clientReceivedMessages[0].substring(SharedEvent.MESSAGE_PREFIX.length()), ReadAssetsEvent.class)
            messageId = readAssetsEvent.messageID
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + "0"
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == GatewayConnector.SYNC_ASSET_BATCH_SIZE
            assert agentAssetIds.stream().filter { readAssetsEvent.assetQuery.ids.contains(it) }.count() == agentAssetIds.size()
            assert assetIds.stream().filter { readAssetsEvent.assetQuery.ids.contains(it) }.count() + 1 == GatewayConnector.SYNC_ASSET_BATCH_SIZE - agentAssetIds.size()
        }

        when: "another asset is added to the gateway during the initial sync process"
        def building2Room5Asset = new RoomAsset("Test Building 2 Room 5")
                .setId(UniqueIdentifierGenerator.generateId("Test Building 2 Room 5"))
                .setCreatedOn(Date.from(timerService.getNow()))
                .setParentId(assetIds[5])
                .setRealm(MASTER_REALM)
        building2Room5Asset.path = (String[]) [UniqueIdentifierGenerator.generateId("Test Building 2 Room 5"), assetIds[5]]

        building2Room5Asset.addOrReplaceAttributes(
                new Attribute<>(Asset.LOCATION, new GeoJSONPoint(10, 11)).addOrReplaceMeta(
                        new MetaItem<>(ACCESS_PUBLIC_READ)
                ),
                new Attribute<>("temp", NUMBER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[1])
                                .setPath("")),
                        new MetaItem<>(UNITS, Constants.units(UNITS_CELSIUS))
                ),
                new Attribute<>("tempSetpoint", NUMBER).addMeta(
                        new MetaItem<>(AGENT_LINK, new HTTPAgentLink(agentAssetIds[1])
                                .setPath("")),
                        new MetaItem<>(UNITS, Constants.units(UNITS_CELSIUS))
                )
        )

        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.CREATE, building2Room5Asset, null)).get())

        and: "another asset is deleted from the gateway during the initial sync process (Building 3 Room 1)"
        def removedAsset = assets.remove(10)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(new AssetEvent(AssetEvent.Cause.DELETE, removedAsset, null)).get())

        and: "the gateway returns the requested assets (minus the deleted Building 3 Room 1 asset)"
        sendAssets = [building1Room5Asset]
        sendAssets.addAll(agentAssets)
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).filter { !agentAssetIds.contains(it) && it != removedAsset.id && it != building1Room5Asset.id }.map { id -> assets.stream().filter { it.id == id }.findFirst().orElse(null) }.collect(Collectors.toList()))
        readAssetsReplyEvent = new AssetsEvent(sendAssets)
        readAssetsReplyEvent.setMessageID(messageId)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the central manager should have requested the full loading of the second batch of assets"
        conditions.eventually {
            assert clientReceivedMessages.size() == 2
            assert clientReceivedMessages.get(1).contains("read-assets")
            readAssetsEvent = ValueUtil.JSON.readValue(clientReceivedMessages[1].substring(SharedEvent.MESSAGE_PREFIX.length()), ReadAssetsEvent.class)
            messageId = readAssetsEvent.messageID
            assert messageId == GatewayConnector.ASSET_READ_EVENT_NAME_BATCH + (GatewayConnector.SYNC_ASSET_BATCH_SIZE - 1)
            assert readAssetsEvent.assetQuery != null
            assert readAssetsEvent.assetQuery.ids != null
            assert readAssetsEvent.assetQuery.ids.length == agentAssetIds.size() + assets.size() + 1 - GatewayConnector.SYNC_ASSET_BATCH_SIZE + 1
            assert assets.stream().filter { asset -> sendAssets.stream().noneMatch { it.id == asset.id } }.count() == readAssetsEvent.assetQuery.ids.length
        }

        when: "the gateway returns the requested assets"
        sendAssets = []
        sendAssets.addAll(Arrays.stream(readAssetsEvent.assetQuery.ids).map { id -> assets.stream().filter { asset -> asset.id == id }.findFirst().orElse(null) }.collect(Collectors.toList()))
        readAssetsReplyEvent = new AssetsEvent(sendAssets)
        readAssetsReplyEvent.setMessageID(messageId)
        gatewayClient.sendMessage(SharedEvent.MESSAGE_PREFIX + ValueUtil.asJSON(readAssetsReplyEvent).get())

        then: "the gateway connector sync should be completed"
        conditions.eventually {
            def gatewayConnector = gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT))
            assert gatewayConnector.isConnected()
            assert !gatewayConnector.isInitialSyncInProgress()
        }

        and: "the gateway netty client status should become CONNECTED"
        conditions.eventually {
            assert connectionStatus == ConnectionStatus.CONNECTED
            assert gatewayClient.connectionStatus == ConnectionStatus.CONNECTED
        }

        and: "the gateway asset connection status should be CONNECTED"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        and: "the gateway should have the correct assets"
        def gatewayAssets = assetStorageService.findAll(new AssetQuery().parents(gateway.getId()).recursive(true))
        assert gatewayAssets.size() == 2 + agentAssets.size() + assets.size()

        when: "the gateway asset is deleted"
        def deleted = assetStorageService.delete([gateway.id])

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

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 30, delay: 0.2)
        def delayedConditions = new PollingConditions(initialDelay: 1, delay: 1, timeout: 10)
        def container = startContainer(defaultConfig(), defaultServices())
        def timerService = container.getService(TimerService.class)
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
        GatewayAsset gateway = assetStorageService.merge(new GatewayAsset("Test gateway")
                .setRealm(managerTestSetup.realmBuildingName))

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
                managerTestSetup.realmCityName,
                "127.0.0.1",
                serverPort,
                managerTestSetup.realmBuildingName,
                gateway.getClientId().orElse(""),
                gateway.getClientSecret().orElse(""),
                false,
                null,
                Map.of("test", new GatewayAssetSyncRule()),
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

        when: "the gateway client is abruptly disconnected"
        // Overwrite the client connection status to prevent reconnection
        gatewayClientService.clientRealmMap.get(managerTestSetup.realmCityName).connectionStatus = ConnectionStatus.DISCONNECTED
        gatewayClientService.clientRealmMap.get(managerTestSetup.realmCityName).channel.close()

        then: "the gateway asset connection status should become DISCONNECTED"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.DISCONNECTED
        }

        when: "the connection is reestablished"
        gatewayClientService.clientRealmMap.get(managerTestSetup.realmCityName).connect()

        then: "the gateway asset connection status should become CONNECTED"
        conditions.eventually {
            gateway = assetStorageService.find(gateway.getId())
            assert gateway.getGatewayStatus().orElse(null) == ConnectionStatus.CONNECTED
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
            assert mirroredMicrophone.getAttributes().get(MicrophoneAsset.SOUND_LEVEL).flatMap { it.getMetaItem(READ_ONLY) }.flatMap { it.getValue() }.orElse(false)
            assert mirroredMicrophone.getAttribute("test").isPresent()
            assert mirroredMicrophone.getAttribute("test").flatMap { it.getValue() }.orElse(0d) == 100d
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
            assert mirroredMicrophone2.getAttribute("test").flatMap { it.getValue() }.orElse("") == "testValue"
        }

        when: "an attempt is made to add an asset under the gateway asset"
        def microphone3 = ValueUtil.clone(microphone1)
                .setName("Microphone 3")
                .setId(null)
                .setRealm(managerTestSetup.realmBuildingName)
                .setParentId(mapAssetId(gateway.id, managerTestSetup.area1Id, false))
        microphone3 = assetStorageService.merge(microphone3)

        then: "an exception should be thrown"
        thrown(IllegalStateException)

        when: "an asset with a type unknown on the central instance is added on the gateway"
        // We simulate this as both edge and central are in the same instance here in the test
        def msgEvent = new AssetEvent(
                AssetEvent.Cause.CREATE,
                new BuildingAsset("UnknownAssetType")
                        .setId(UniqueIdentifierGenerator.generateId()),
                null
        )
        msgEvent.asset.type = "CustomBuildingAsset"
        gatewayClientService.sendCentralManagerMessage(gatewayConnection.getLocalRealm(), gatewayClientService.messageToString(SharedEvent.MESSAGE_PREFIX, msgEvent))

        then: "it should be added to the central instance as a thing asset"
        conditions.eventually {
            def mirroredCustomAssetType = assetStorageService.find(mapAssetId(gateway.id, msgEvent.asset.id, false))
            assert mirroredCustomAssetType != null
            assert mirroredCustomAssetType instanceof UnknownAsset
            assert mirroredCustomAssetType.type == "CustomBuildingAsset"
        }

        when: "an attribute is updated on the gateway client"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(microphone2.id, "test", "newValue"))

        then: "the mirrored asset attribute should also be updated"
        conditions.eventually {
            def mirroredMicrophone2 = assetStorageService.find(mapAssetId(gateway.id, microphone2.id, false))
            assert mirroredMicrophone2 != null
            assert mirroredMicrophone2.getAttribute("test").flatMap { it.getValue() }.orElse("") == "newValue"
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
            assert microphone2.getAttribute("test").flatMap { it.getValue() }.orElse("") == "newerValue"
            assert mirroredMicrophone2.getAttribute("test").flatMap { it.getValue() }.orElse("") == "newerValue"
        }

        when: "we subscribe to attribute events"
        List<AttributeEvent> attributeEvents = new CopyOnWriteArrayList<>()
        Consumer<AttributeEvent> eventConsumer = { attributeEvent ->
            attributeEvents.add(attributeEvent)
        }
        clientEventService.addSubscription(AttributeEvent.class, null, eventConsumer)

        and: "an attribute with an attribute link is updated on the gateway"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.light2Id, LightAsset.ON_OFF, true))

        then: "only four attribute events should have been generated, one for each of light1 and light2 and one for each of their mirrored assets below the gateway asset"
        delayedConditions.eventually {
            assert attributeEvents.size() == 4
            assert attributeEvents.any { it.id == managerTestSetup.light2Id && it.name == LightAsset.ON_OFF.name && it.value.orElse(false) }
            assert attributeEvents.any { it.id == managerTestSetup.light1Id && it.name == LightAsset.ON_OFF.name && it.value.orElse(false) }
            assert attributeEvents.any { it.id == mapAssetId(gateway.id, managerTestSetup.light2Id, false) && it.name == LightAsset.ON_OFF.name && it.value.orElse(false) }
            assert attributeEvents.any { it.id == mapAssetId(gateway.id, managerTestSetup.light1Id, false) && it.name == LightAsset.ON_OFF.name && it.value.orElse(false) }
        }

        when: "a gateway client asset is deleted"
        def deleted = assetStorageService.delete(Collections.singletonList(managerTestSetup.microphone1Id))

        then: "the client asset should have been deleted and also the mirrored asset under the gateway should also be deleted"
        assert deleted
        conditions.eventually {
            assert assetStorageService.find(mapAssetId(gateway.id, managerTestSetup.microphone1Id, false)) == null
        }

        when: "attribute filters are added to the gateway connection and persisted"
        gatewayConnection.setAttributeFilters([
                new GatewayAttributeFilter().setMatcher(new AssetQuery().types(LightAsset.class)).setValueChange(true),
                new GatewayAttributeFilter().setMatcher(new AssetQuery().types(PeopleCounterAsset.class).attributeNames(PeopleCounterAsset.COUNT_TOTAL.name, PeopleCounterAsset.COUNT_GROWTH_PER_MINUTE.name))
                        .setDelta(1.5d),
                new GatewayAttributeFilter().setMatcher(new AssetQuery().names("Microphone 2")).setDuration("PT1M"),
                // Ignore any other attribute
                new GatewayAttributeFilter().setSkipAlways(true)
        ])
        def oldClient = gatewayClientService.clientRealmMap.get(gatewayConnection.getLocalRealm())
        gatewayClientResource.setConnection(null, managerTestSetup.realmCityName, gatewayConnection)

        then: "the gateway connection IO client should have been replaced and synchronisation should be complete"
        conditions.eventually {
            assert gatewayClientService.clientRealmMap.get(gatewayConnection.getLocalRealm()) != null
            assert gatewayClientService.clientRealmMap.get(gatewayConnection.getLocalRealm()) != oldClient
            GatewayConnector connector = gatewayService.gatewayConnectorMap.get(gateway.getId().toLowerCase(Locale.ROOT))
            assert connector != null
            assert !connector.initialSyncInProgress
            assert attributeEvents.any { it.id == gateway.id && it.value.get() == ConnectionStatus.CONNECTED }
        }

        when: "an attribute event occurs in the edge gateway realm for an attribute with a value change filter and the attribute value has not changed"
        attributeEvents.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.light2Id, LightAsset.ON_OFF, true))

        then: "the value should not have been forwarded to the central instance"
        delayedConditions.eventually {
            assert attributeEvents.any { it.id == managerTestSetup.light2Id && it.name == LightAsset.ON_OFF.name && it.value.orElse(false) }
            assert !attributeEvents.any { it.id == mapAssetId(gateway.id, managerTestSetup.light2Id, false) && it.name == LightAsset.ON_OFF.name }
        }

        when: "an attribute event occurs in the edge gateway realm for an attribute with a value change filter and the attribute value has changed"
        attributeEvents.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.light2Id, LightAsset.ON_OFF, false))

        then: "the value should now have been forwarded to the central instance"
        conditions.eventually {
            def mirroredLight2 = assetStorageService.find(mapAssetId(gateway.id, managerTestSetup.light2Id, false))
            assert mirroredLight2 != null
            assert !mirroredLight2.getAttribute(LightAsset.ON_OFF).flatMap { it.getValue() }.orElse(true)
            assert attributeEvents.any { it.id == managerTestSetup.light2Id && it.name == LightAsset.ON_OFF.name && !it.value.orElse(true) }
            assert attributeEvents.any { it.id == mapAssetId(gateway.id, managerTestSetup.light2Id, false) && it.name == LightAsset.ON_OFF.name && !it.value.orElse(true) }
        }

        when: "an attribute event occurs in the edge gateway realm for an attribute with a delta filter and the attribute value has changed by less than delta"
        attributeEvents.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.peopleCounter1AssetId, PeopleCounterAsset.COUNT_GROWTH_PER_MINUTE, 0.5d))

        then: "the value should not have been forwarded to the central instance"
        delayedConditions.eventually {
            assert attributeEvents.any { it.id == managerTestSetup.peopleCounter1AssetId && it.name == PeopleCounterAsset.COUNT_GROWTH_PER_MINUTE.name && Math.abs(it.value.orElse(0d) - 0.5d) < 0.000001d }
            assert !attributeEvents.any { it.id == mapAssetId(gateway.id, managerTestSetup.peopleCounter1AssetId, false) && it.name == PeopleCounterAsset.COUNT_GROWTH_PER_MINUTE.name }
        }

        when: "an attribute event occurs in the edge gateway realm for an attribute with a delta filter and the attribute value has changed by more than delta"
        attributeEvents.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.peopleCounter1AssetId, PeopleCounterAsset.COUNT_GROWTH_PER_MINUTE, -1.1d))

        then: "the value should have been forwarded to the central instance"
        conditions.eventually {
            assert attributeEvents.any { it.id == managerTestSetup.peopleCounter1AssetId && it.name == PeopleCounterAsset.COUNT_GROWTH_PER_MINUTE.name && Math.abs(it.value.orElse(0d) + 1.1d) < 0.000001d }
            assert attributeEvents.any { it.id == mapAssetId(gateway.id, managerTestSetup.peopleCounter1AssetId, false) && it.name == PeopleCounterAsset.COUNT_GROWTH_PER_MINUTE.name && Math.abs(it.value.orElse(0d) + 1.1d) < 0.000001d }
        }

        when: "an attribute event occurs in the edge gateway realm for an attribute with a duration filter"
        attributeEvents.clear()
        microphone2 = assetStorageService.find(microphone2.id)
        assetProcessingService.sendAttributeEvent(new AttributeEvent(microphone2.id, MicrophoneAsset.SOUND_LEVEL, 50d))

        then: "the value should have been forwarded to the central instance (as no prior value has been sent since the filter was created)"
        conditions.eventually {
            assert attributeEvents.any { it.id == microphone2.id && it.name == MicrophoneAsset.SOUND_LEVEL.name && Math.abs(it.value.orElse(0d) - 50d) < 0.000001d }
            assert attributeEvents.any { it.id == mapAssetId(gateway.id, microphone2.id, false) && it.name == MicrophoneAsset.SOUND_LEVEL.name && Math.abs(it.value.orElse(0d) - 50d) < 0.000001d }
        }

        when: "an attribute event occurs in the edge gateway realm for the same attribute before the duration filter has elapsed"
        attributeEvents.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(microphone2.id, MicrophoneAsset.SOUND_LEVEL, 60d))

        then: "the value should not have been forwarded to the central instance"
        delayedConditions.eventually {
            assert attributeEvents.any { it.id == microphone2.id && it.name == MicrophoneAsset.SOUND_LEVEL.name && Math.abs(it.value.orElse(0d) - 60d) < 0.000001d }
            assert !attributeEvents.any { it.id == mapAssetId(gateway.id, microphone2.id, false) && it.name == MicrophoneAsset.SOUND_LEVEL.name }
        }

        when: "an attribute event occurs in the edge gateway realm for an attribute that matches the catch all and skip filter"
        attributeEvents.clear()
        assetProcessingService.sendAttributeEvent(new AttributeEvent(managerTestSetup.area1Id, Asset.NOTES, "Some test notes"))

        then: "the value should not have been forwarded to the central instance"
        delayedConditions.eventually {
            assert attributeEvents.any { it.id == managerTestSetup.area1Id && it.name == Asset.NOTES.name && "Some test notes".equals(it.value.orElse(null)) }
            assert !attributeEvents.any { it.id == mapAssetId(gateway.id, managerTestSetup.area1Id, false) && it.name == Asset.NOTES.name }
        }

        cleanup: "Remove any subscriptions created"
        if (clientEventService != null) {
            clientEventService.removeSubscription(eventConsumer)
        }
        if (gateway != null) {
            assetStorageService.delete([gateway.id])
        }
    }

    /**
     * This test requires a manager instance with tunnelling configured, so is manual for now unfortunately.
     * Change the test url and key path to match the instance to connect to.
     * Recommended to run profile/dev-proxy.yml profile.
     */
    @Ignore
    def "Verify gateway tunnel factory"() {
        given: "an ssh private key and the URL of a manager instance with tunnelling configured"
        def keyPath = Paths.get(System.getProperty("user.home"), ".ssh", "test_key")
        def tunnelSSHHost = "test.openremote.app"
        def tunnelSSHPort = 2222

        and: "the container environment is started"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)
        def container = startContainer(defaultConfig() << [(GatewayService.OR_GATEWAY_TUNNEL_SSH_KEY_FILE): keyPath.toAbsolutePath().toString()], defaultServices())
        def gatewayClientService = container.getService(GatewayClientService)
        def tunnelFactory = gatewayClientService.gatewayTunnelFactory as JSchGatewayTunnelFactory
        def client = WebTargetBuilder.createClient(container.getScheduledExecutor())
        def tunnelInfo = new GatewayTunnelInfo(
                "",
                UniqueIdentifierGenerator.generateId(),
                GatewayTunnelInfo.Type.HTTPS,
                "localhost",
                443)
        def target = client.target("https://${tunnelInfo.getId()}.${tunnelSSHHost}/auth/")

        expect: "the tunnel factory to be created"
        tunnelFactory != null

        when: "a tunnel is requested to start"
        def startEvent = new GatewayTunnelStartRequestEvent(
                tunnelSSHHost,
                tunnelSSHPort,
                null,
                null,
                tunnelInfo)
        tunnelFactory.startTunnel(startEvent)

        then: "the tunnel should be established and be usable"
        tunnelFactory.sessionMap.containsKey(tunnelInfo)
        def response = target.request().get()
        response.status == 200

        when: "the tunnel is stopped"
        tunnelFactory.stopTunnel(tunnelInfo)

        then: "the tunnel should be destroyed"
        !tunnelFactory.sessionMap.containsKey(tunnelInfo)

        and: "requests should fail"
        def response2 = target.request().get()
        response2.status != 200

        cleanup: "cleanup"
        if (client != null) {
            client.close()
        }
    }

    def "Verify GatewayAssetSyncRules"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 30, delay: 0.2)
        def delayedConditions = new PollingConditions(initialDelay: 1, delay: 1, timeout: 10)
        def container = startContainer(defaultConfig(), defaultServices())
        def timerService = container.getService(TimerService.class)
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

        when: "a gateway asset is provisioned in this manager in the building realm"
        GatewayAsset gateway = assetStorageService.merge(new GatewayAsset("Test gateway")
                .setRealm(managerTestSetup.realmBuildingName))

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
                managerTestSetup.realmCityName,
                "127.0.0.1",
                serverPort,
                managerTestSetup.realmBuildingName,
                gateway.getClientId().orElse(""),
                gateway.getClientSecret().orElse(""),
                false,
                null,
                [
                        "*" : new GatewayAssetSyncRule().setExcludeAttributeMeta([
                                "*": [
                                    ACCESS_PUBLIC_READ.name,
                                    ACCESS_PUBLIC_WRITE.name,
                                    STORE_DATA_POINTS.name
                                ]
                        ]).setAddAttributeMeta([
                                "*": new MetaMap([
                                    new MetaItem<>(ACCESS_RESTRICTED_READ, true)
                                ])
                        ]).setExcludeAttributes([
                                Asset.NOTES.name
                        ]),
                        "LightAsset" : new GatewayAssetSyncRule().setExcludeAttributeMeta([
                                "*": [
                                        ATTRIBUTE_LINKS.name,
                                        ACCESS_PUBLIC_WRITE.name,
                                        FORMAT.name
                                ]
                        ]).setAddAttributeMeta([
                                "onOff" : new MetaMap([
                                        new MetaItem<>(FORMAT, ValueFormat.BOOLEAN_AS_PRESSED_RELEASED())
                                ])
                        ]).setExcludeAttributes([
                                LightAsset.COLOUR_RGB.name
                        ])
                ] as Map<String, GatewayAssetSyncRule>,
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

        and: "the asset sync rules should have been applied"
        conditions.eventually {
            def mirroredMicrophone = assetStorageService.find(mapAssetId(gateway.id, managerTestSetup.microphone1Id, false))
            def mirroredLight = assetStorageService.find(mapAssetId(gateway.id, managerTestSetup.light1Id, false))
            assert mirroredMicrophone != null
            assert mirroredLight != null
            assert mirroredMicrophone.getAttributes().get(MicrophoneAsset.SOUND_LEVEL).flatMap { it.getMetaItem(READ_ONLY) }.flatMap { it.getValue() }.orElse(false)
            assert mirroredMicrophone.getAttributes().get(MicrophoneAsset.SOUND_LEVEL).flatMap { it.getMetaItem(ACCESS_RESTRICTED_READ) }.flatMap { it.getValue() }.orElse(false)
            assert !mirroredMicrophone.getAttributes().get(MicrophoneAsset.SOUND_LEVEL).flatMap { it.getMetaItem(STORE_DATA_POINTS) }.flatMap { it.getValue() }.orElse(false)
            assert mirroredMicrophone.getAttributes().get(MicrophoneAsset.LOCATION).flatMap { it.getMetaItem(ACCESS_RESTRICTED_READ) }.flatMap { it.getValue() }.orElse(false)
            assert !mirroredMicrophone.getAttribute(Asset.NOTES).isPresent()
            assert mirroredLight.getAttributes().get(LightAsset.ON_OFF).flatMap { it.getMetaItem(FORMAT) }.flatMap { it.getValue() }.map {it.asPressedReleased}.orElse(false) == ValueFormat.BOOLEAN_AS_PRESSED_RELEASED().asPressedReleased
            assert !mirroredLight.getAttribute(LightAsset.COLOUR_RGB).isPresent()
            assert mirroredLight.getAttribute(Asset.NOTES).isPresent()
        }
    }
}
