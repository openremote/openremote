package org.openremote.test.controller2

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.openremote.container.message.MessageBrokerService
import org.openremote.manager.client.service.RequestServiceImpl
import org.openremote.manager.client.service.SecurityService
import org.openremote.manager.server.SampleDataService
import org.openremote.manager.server.asset.AssetService
import org.openremote.manager.shared.Consumer
import org.openremote.manager.shared.Runnable
import org.openremote.manager.shared.agent.Agent
import org.openremote.manager.shared.agent.AgentResource
import org.openremote.manager.shared.asset.Asset
import org.openremote.manager.shared.asset.AssetType
import org.openremote.manager.shared.attribute.AttributeType
import org.openremote.manager.shared.device.DeviceAttributes
import org.openremote.manager.shared.device.DeviceResource
import org.openremote.manager.shared.http.EntityReader
import org.openremote.manager.shared.http.RequestException
import org.openremote.manager.shared.http.RequestParams
import org.openremote.manager.shared.validation.ConstraintViolationReport
import org.openremote.test.ClientObjectMapper
import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables

import static org.openremote.manager.shared.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.shared.Constants.MASTER_REALM

class Controller2Test extends Specification implements ContainerTrait {

    def "Get Device Inventory"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user and client security service"
        def realm = MASTER_REALM;
        def accessToken = authenticate(container, realm, MANAGER_CLIENT_ID, "admin", "admin").token
        def securityService = Stub(SecurityService) {
            getRealm() >> realm
            getToken() >> accessToken
            updateToken(_, _, _) >> { int minValiditySeconds, Consumer<Boolean> successFn, Runnable errorFn ->
                successFn.accept(true) // The token is always valid (this assumes the test doesn't run very long)
            };
            hasResourceRoleOrIsAdmin(_, _) >> { String role, String resource ->
                return true; // TODO: Should use the parsed token
            }
        }

        and: "a client request service and target"
        def constraintViolationReader = new ClientObjectMapper(container.JSON, ConstraintViolationReport.class) as EntityReader<ConstraintViolationReport>
        def requestService = new RequestServiceImpl(securityService, constraintViolationReader)
        def clientTarget = getClientTarget(createClient(container).build(), serverUri(serverPort), realm)

        and: "an agent client resource"
        def agentResource = Stub(AgentResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

        when: "we wait a bit for initial state"
        sleep(2000)

        then: "the device assets should be the children of the agent asset"
        def assetService = container.getService(AssetService.class)
        def agentAsset = assetService.get(container.getService(SampleDataService.class).SAMPLE_AGENT_ID)
        agentAsset != null
        def deviceAssetInfos = assetService.getChildren(agentAsset.getId())
        deviceAssetInfos.size() == 1
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
        requestService.execute(
                new Consumer<RequestParams<Void>>() {
                    @Override
                    void accept(RequestParams requestParams) {
                        agentResource.refreshInventory(requestParams, agentAsset.getId())
                    }
                },
                204,
                new java.lang.Runnable() {
                    @Override
                    void run() {
                    }
                },
                new Consumer<RequestException>() {
                    @Override
                    void accept(RequestException e) {
                    }
                }
        );

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
