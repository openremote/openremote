package org.openremote.test.controller2

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.openremote.container.message.MessageBrokerService
import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables
import spock.util.concurrent.PollingConditions

class Controller2Test extends Specification implements ContainerTrait {

    def "Get Device Inventory"() {
        given: "a test controller service"
        def controllerService = new Controller2Service()

        and: "a clean result state"
        def conditions = new PollingConditions(timeout: 3)

        when: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices(controllerService))

        and: "discovery is triggered"
        controllerService.triggerDiscovery()

        then: "the service should receive the devices from the inventory"
        conditions.eventually {
            assert controllerService.addedDevices.size() > 0
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Write actuator and read updated sensor value"() {
        given: "a test controller service"
        def controllerService = new Controller2Service()

        and: "a clean result state"
        def conditions = new PollingConditions(timeout: 5)
        def result = new BlockingVariables(5)

        when: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices(controllerService))

        and: "an actuator and a sensor route are started"
        String deviceResourceEndpoint = "controller2://192.168.99.100:8083/TestDevice/light1switch";
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
