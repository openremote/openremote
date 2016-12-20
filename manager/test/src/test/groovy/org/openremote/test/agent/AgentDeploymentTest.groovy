package org.openremote.test.agent

import elemental.json.Json
import elemental.json.JsonObject
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.openremote.agent3.protocol.simulator.SimulatorProtocol
import org.openremote.model.AttributeRef
import org.openremote.model.AttributeValueChange
import org.openremote.model.asset.Color
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables
import spock.util.concurrent.PollingConditions
import org.openremote.container.message.MessageBrokerService

import static org.openremote.manager.server.DemoDataService.DEMO_THING_ID
import static org.openremote.agent3.protocol.Protocol.ACTUATOR_TOPIC
import static org.openremote.agent3.protocol.Protocol.SENSOR_TOPIC

class AgentDeploymentTest extends Specification implements ManagerContainerTrait {

    def "Check agent and thing deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 3)

        when: "the demo agent and thing have been deployed"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def simulatorProtocol = container.getService(SimulatorProtocol.class)

        then: "the simulator elements should have the initial state"
        conditions.eventually {
            assert simulatorProtocol.getState(DEMO_THING_ID, "light1Toggle").asBoolean()
            assert simulatorProtocol.getState(DEMO_THING_ID, "light1Dimmer").asNumber() == 55
            assert new Color(simulatorProtocol.getState(DEMO_THING_ID, "light1Color") as JsonObject) == new Color(88, 123, 88)
            assert simulatorProtocol.getState(DEMO_THING_ID, "light1PowerConsumption").asNumber() == 12.345d
        }

        when: "a thing attribute value change occurs"
        conditions = new PollingConditions(timeout: 3, initialDelay: 2)
        def light1DimmerChange = new AttributeValueChange(
                new AttributeRef(DEMO_THING_ID, "light1Dimmer"), Json.create(66)
        )
        container.getService(MessageBrokerService.class).getProducerTemplate().sendBody(
                ACTUATOR_TOPIC, light1DimmerChange
        )

        then: "the simulator state should be updated"
        conditions.eventually {
            assert simulatorProtocol.getState(DEMO_THING_ID, "light1Dimmer").asNumber() == 66
        }

        when: "we listen to sensor changes so we can update the thing attribute value"
        def result = new BlockingVariables(3)
        addRoutes(container, new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from(SENSOR_TOPIC)
                 .process(new Processor() {
                    @Override
                    void process(Exchange exchange) throws Exception {
                        result.attributeValueChange =
                                exchange.getIn().getBody(AttributeValueChange.class)
                    }
                })
            }
        })

        and: "a simulated sensor changes its value"
        simulatorProtocol.putState(
                new AttributeRef(DEMO_THING_ID, "light1Dimmer"),
                Json.create(77),
                true
        )

        then: "a thing value change should occur"
        result.attributeValueChange.attributeRef.entityId == DEMO_THING_ID
        result.attributeValueChange.attributeRef.attributeName == "light1Dimmer"
        result.attributeValueChange.value.asNumber() == 77

        cleanup: "the server should be stopped"
        stopContainer(container);
    }
}
