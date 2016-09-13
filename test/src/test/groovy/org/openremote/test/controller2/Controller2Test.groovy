package org.openremote.test.controller2

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.openremote.container.message.MessageBrokerService
import org.openremote.manager.server.SampleDataService
import org.openremote.manager.server.asset.AssetService
import org.openremote.manager.shared.agent.Agent
import org.openremote.manager.shared.asset.Asset
import org.openremote.manager.shared.asset.AssetType
import org.openremote.manager.shared.attribute.AttributeType
import org.openremote.manager.shared.device.DeviceAttributes
import org.openremote.manager.shared.device.DeviceResource
import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables

class Controller2Test extends Specification implements ContainerTrait {

    def "Get Device Inventory"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        when: "we wait a bit for initial state"
        sleep(2000)

        then: "the device assets should be the children of the agent asset"
        def assetService = container.getService(AssetService.class)
        def agentAsset = assetService.get(container.getService(SampleDataService.class).SAMPLE_AGENT_ID)
        agentAsset != null
        def deviceAssetInfos = assetService.getChildren(agentAsset.getId())
        deviceAssetInfos .size() == 1
        deviceAssetInfos.each {
            Asset device = assetService.get(it.id)
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
            assetService.delete(it.id)
        }

        when: "discovery is triggered"
        getMessageProducerTemplate(container)
                .sendBody(Agent.TOPIC_TRIGGER_DISCOVERY, "") // Empty body triggers all agents

        and: "we wait a bit for inventory response"
        sleep(2000)

        then: "the device assets should be the children of the agent asset"
        def updatedDeviceAssetInfos = assetService.getChildren(agentAsset.getId())
        updatedDeviceAssetInfos.size() == 1

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
        def messageBrokerContext = container.getService(MessageBrokerService.class).context;
        messageBrokerContext.addRoutes(new RouteBuilder() {
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
        messageBrokerContext.createProducerTemplate().sendBody("direct:light1switch", "ON")

        then: "the sensor value should change"
        result.sensorSwitchedOn

        when: "the actuator is switched off"
        messageBrokerContext.createProducerTemplate().sendBody("direct:light1switch", "OFF")

        then: "the sensor value should change"
        result.sensorSwitchedoff

        cleanup: "the server should be stopped"
        stopContainer(container)

    }
}
