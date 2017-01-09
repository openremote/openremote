package org.openremote.test.controller2

import org.openremote.manager.client.event.ServerSendEvent
import org.openremote.manager.client.event.bus.EventBus
import org.openremote.manager.server.DemoDataService
import org.openremote.manager.server.asset.AssetService
import org.openremote.manager.server.event.EventService
import org.openremote.manager.shared.agent.DeviceResourceRead
import org.openremote.manager.shared.agent.DeviceResourceValueEvent
import org.openremote.manager.shared.agent.DeviceResourceWrite
import org.openremote.manager.shared.agent.RefreshInventoryEvent
import org.openremote.manager.shared.agent.SubscribeDeviceResourceUpdates
import org.openremote.manager.shared.agent.UnsubscribeDeviceResourceUpdates
import org.openremote.model.asset.Asset
import org.openremote.manager.shared.asset.AssetModifiedEvent
import org.openremote.model.asset.AssetType
import org.openremote.manager.shared.asset.SubscribeAssetModified
import org.openremote.model.AttributeType
import org.openremote.manager.shared.device.*
import org.openremote.test.BlockingWebsocketEndpoint
import org.openremote.test.ContainerTrait
import org.openremote.test.EventBusWebsocketEndpoint
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables

import static org.openremote.manager.shared.Constants.APP_CLIENT_ID
import static org.openremote.manager.shared.Constants.MASTER_REALM
import static org.openremote.manager.shared.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.manager.server.DemoDataService.ADMIN_PASSWORD

@Ignore
class Controller2Test extends Specification implements ContainerTrait {

    def "Get Device Inventory"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessToken = authenticate(container, realm, APP_CLIENT_ID, MASTER_REALM_ADMIN_USER, ADMIN_PASSWORD).token

        and: "a client target"
        def clientTarget = getClientTarget(createClient(container).build(), serverUri(serverPort), realm)
        def websocketClient = createWebsocketClient()

        when: "everything has started"
        sleep(2000)

        then: "the device assets should be the children of the agent asset"
        def assetService = container.getService(AssetService.class)
        def agentAsset = assetService.get(container.getService(DemoDataService.class).DEMO_AGENT_ID)
        agentAsset != null
        def deviceAssetInfos = assetService.getChildren(agentAsset.getId())
        deviceAssetInfos.size() == 1
        def deviceAssetInfo = deviceAssetInfos[0];
        Asset device = assetService.get(deviceAssetInfo.id)
        assert device.name == "TestDevice"
        assert device.id != null
        assert device.wellKnownType == AssetType.DEVICE
        def attributes = new DeviceAttributes(device.attributes)
        assert attributes.deviceResources.length == 5
        assert attributes.getDeviceResource("Light1Switch").type == AttributeType.STRING
        assert attributes.getDeviceResource("Light1Switch").valueAsString == "light1switch"
        assert attributes.getDeviceResource("Light1Switch").getResourceType() == AttributeType.BOOLEAN
        assert attributes.getDeviceResource("Light1Switch").getAccess() == DeviceResource.Access.RW

        // Clear database for next condition
        assetService.delete(deviceAssetInfo.id)

        when: "connecting to the websocket"
        def endpoint = new BlockingWebsocketEndpoint(2);
        def session = connect(websocketClient, endpoint, clientTarget, realm, accessToken, EventService.WEBSOCKET_EVENTS);

        and: "listening to asset modification events"
        session.basicRemote.sendText(container.JSON.writeValueAsString(
                new SubscribeAssetModified()
        ));

        and: "refreshing the agent's inventory"
        session.basicRemote.sendText(container.JSON.writeValueAsString(
                new RefreshInventoryEvent(agentAsset.getId())
        ));

        and: "waiting for inventory changes"
        endpoint.awaitMessagesAndCloseOnCompletion()

        then: "the device assets should be the children of the agent asset"
        def updatedDeviceAssetInfos = assetService.getChildren(agentAsset.getId())
        updatedDeviceAssetInfos.size() == 1

        and: "the asset modified events should have been received"
        def createEvent = container.JSON.readValue(endpoint.messages[0].toString(), AssetModifiedEvent.class)
        createEvent.cause == AssetModifiedEvent.Cause.CREATE
        createEvent.assetInfo.id == deviceAssetInfo.id

        def childrenModifiedEvent = container.JSON.readValue(endpoint.messages[1].toString(), AssetModifiedEvent.class)
        childrenModifiedEvent.cause == AssetModifiedEvent.Cause.CHILDREN_MODIFIED
        childrenModifiedEvent.assetInfo.id == deviceAssetInfo.parentId

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    // You might want to GET 192.168.99.100:8083/controller/rest/devices/TestDevice/commands?name=Light1Off
    // to turn off the light whenever this test failed, so you have a clean state to run the test again...
    def "Listen to, write, and read device resource value"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessToken = authenticate(container, realm, APP_CLIENT_ID, MASTER_REALM_ADMIN_USER, ADMIN_PASSWORD).token

        and: "a client target"
        def clientTarget = getClientTarget(createClient(container).build(), serverUri(serverPort), realm)
        def websocketClient = createWebsocketClient()

        and: "everything has started"
        sleep(2000)

        and: "an expected result"
        def result = new BlockingVariables(5)

        and: "connecting a client event bus"
        def eventBus = new EventBus()
        def endpoint = new EventBusWebsocketEndpoint(eventBus, container.JSON)
        connect(websocketClient, endpoint, clientTarget, realm, accessToken, EventService.WEBSOCKET_EVENTS);

        and: "preparing to receive device resource updates"
        def resourceUpdatesRegistration = eventBus.register(DeviceResourceValueEvent.class) { event ->
            switch (event.getValue()) {
                case "ON":
                    result.lightSwitchedOn = true;
                    break;
                case "OFF":
                    result.lightSwitchedOff = true;
                    break;
                default:
                    throw new IllegalArgumentException("Don't know how to handle: " + event)
            }
        }

        when: "subscribing to device resource updates through an agent"
        def assetService = container.getService(AssetService.class)
        def agentAsset = assetService.get(container.getService(DemoDataService.class).DEMO_AGENT_ID)
        eventBus.dispatch(new ServerSendEvent(new SubscribeDeviceResourceUpdates(
                agentAsset.getId(), "testdevice"
        )))

        and: "the device resource value is written"
        eventBus.dispatch(new ServerSendEvent(new DeviceResourceWrite(
                agentAsset.getId(),
                "testdevice",
                "light1switch",
                "ON"
        )))

        then: "the device resource value should change"
        result.lightSwitchedOn

        when: "the device resource value is written again"
        eventBus.dispatch(new ServerSendEvent(new DeviceResourceWrite(
                agentAsset.getId(),
                "testdevice",
                "light1switch",
                "OFF"
        )))

        then: "the device resource value should change again"
        result.lightSwitchedOff

        when: "unsubscribing from device resource updates"
        eventBus.dispatch(new ServerSendEvent(new UnsubscribeDeviceResourceUpdates(
                agentAsset.getId(), "testdevice"
        )))
        eventBus.remove(resourceUpdatesRegistration)

        and: "preparing to receive device resource value with a new handler"
        eventBus.register(DeviceResourceValueEvent.class) { event ->
            switch (event.getValue()) {
                case "off":
                    result.receivedReadValue = true;
                    break;
                default:
                    throw new IllegalArgumentException("Don't know how to handle: " + event)
            }
        }

        and: "device resource state is read"
        eventBus.dispatch(new ServerSendEvent(new DeviceResourceRead(
                agentAsset.getId(), "testdevice", "light1switch"
        )))

        then: "the result of that read operation should be received"
        result.receivedReadValue

        cleanup: "the server should be stopped"
        endpoint.close()
        stopContainer(container)
    }
}
