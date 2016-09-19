package org.openremote.test.controller2

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.openremote.container.message.MessageBrokerService
import org.openremote.manager.server.SampleDataService
import org.openremote.manager.server.asset.AssetService
import org.openremote.manager.server.event.EventService
import org.openremote.manager.shared.agent.RefreshInventoryEvent
import org.openremote.manager.shared.asset.Asset
import org.openremote.manager.shared.asset.AssetModifiedEvent
import org.openremote.manager.shared.asset.AssetType
import org.openremote.manager.shared.asset.SubscribeAssetModified
import org.openremote.manager.shared.attribute.AttributeType
import org.openremote.manager.shared.device.DeviceAttributes
import org.openremote.manager.shared.device.DeviceResource
import org.openremote.test.BlockingWebsocketEndpoint
import org.openremote.test.ContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables

import static org.openremote.manager.shared.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.shared.Constants.MASTER_REALM

class Controller2Test extends Specification implements ContainerTrait {

    def "Get Device Inventory"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessToken = authenticate(container, realm, MANAGER_CLIENT_ID, "admin", "admin").token

        and: "a client target"
        def clientTarget = getClientTarget(createClient(container).build(), serverUri(serverPort), realm)
        def websocketClient = createWebsocketClient()

        when: "we wait a bit for initial state"
        sleep(2000)

        then: "the device assets should be the children of the agent asset"
        def assetService = container.getService(AssetService.class)
        def agentAsset = assetService.get(container.getService(SampleDataService.class).SAMPLE_AGENT_ID)
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

        and: "we listen to asset modification events"
        session.basicRemote.sendText(container.JSON.writeValueAsString(
                new SubscribeAssetModified()
        ));

        and: "discovery is triggered"
        session.basicRemote.sendText(container.JSON.writeValueAsString(
                new RefreshInventoryEvent(agentAsset.getId())
        ));

        and: "we wait a bit for inventory response"
        endpoint.awaitMessages()

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


    def "Write actuator and read updated sensor value"() {
        given: "a clean result state"
        def result = new BlockingVariables(5)

        when: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an actuator and a sensor route are started"
        String deviceResourceEndpoint = "controller2://192.168.99.100:8083/testdevice/light1switch";
        addRoutes(container, new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("direct:light1switch")
                        .to(deviceResourceEndpoint);

                from(deviceResourceEndpoint)
                        .process(new Processor() {
                    @Override
                    void process(Exchange exchange) throws Exception {
                        switch (exchange.getIn().getBody(String.class)) {
                            case "ON":
                                result.sensorSwitchedOn = true;
                                break;
                            case "OFF":
                                result.sensorSwitchedoff = true;
                                break;
                            default:
                                throw new IllegalArgumentException("Don't know how to handle: " + exchange.getIn().getBody())
                        }
                    }
                })
            }
        })

        and: "the actuator is switched on"
        getMessageProducerTemplate(container).sendBody("direct:light1switch", "ON")

        then: "the sensor value should change"
        result.sensorSwitchedOn

        when: "the actuator is switched off"
        getMessageProducerTemplate(container).sendBody("direct:light1switch", "OFF")

        then: "the sensor value should change"
        result.sensorSwitchedoff

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
