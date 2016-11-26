package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.controller.ControllerService
import org.openremote.controller.event.CommandFacade
import org.openremote.controller.event.EventProcessorChain
import org.openremote.controller.event.SwitchEvent
import org.openremote.controller.model.SwitchSensor
import org.openremote.controller.rules.RuleEngine
import org.openremote.test.ContainerTrait
import org.openremote.test.util.EventGrabProcessor
import org.openremote.test.util.TestEventProducerCommand
import spock.lang.Specification

import java.util.stream.Stream

import static org.kie.api.io.ResourceType.DRL

/**
 * Basic rule engine test, trigger a rule through an event.
 * The rule dispatches a new event, replacing the original event.
 */
class SimpleRuleTest extends Specification implements ContainerTrait {

    def "Sensor event replaced by rule-triggered event"() {

        given: "event processors and rules"
        def ruleEngineProcessor = new RuleEngine() {
            @Override
            protected Stream<Resource> getResources(KieServices kieServices) {
                Stream.of(
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/simple/EventMod.drl"
                        ).setResourceType(DRL)
                )
            }
        }
        def grabProcessor = new EventGrabProcessor();

        def commandDefinitions = []
        def commandFacade = new CommandFacade(commandDefinitions)
        def eventProcessorChain = new EventProcessorChain(
                commandFacade,
                ruleEngineProcessor,
                grabProcessor
        )

        and: "the started controller server"
        def controllerService = new ControllerService(eventProcessorChain)
        def services = Stream.of(controllerService)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), services)

        and: "registered sensor"
        def sensorName = "test"
        def sensorId = 123
        def commandId = 1
        def switchSensor = new SwitchSensor(
                sensorName,
                sensorId,
                controllerService.getCache(),
                new TestEventProducerCommand(),
                commandId
        );
        controllerService.getCache().registerSensor(switchSensor);

        when: "the sensor says it has been switched on"
        def switchEvent = new SwitchEvent(sensorId, sensorName, "on", SwitchEvent.State.ON);
        controllerService.getCache().update(switchEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the rules should immediately switch the sensor off again"
        def lastEvent = grabProcessor.lastEvent;
        lastEvent.getSourceID() == sensorId
        lastEvent.getSource() == sensorName
        lastEvent instanceof SwitchEvent
        def grabbedSwitchEvent = (SwitchEvent) lastEvent;
        grabbedSwitchEvent.getValue() == "off"
        grabbedSwitchEvent.getState() == SwitchEvent.State.OFF

        and: "the total event count is 1, as the event fired in the rule terminates processing of our switch event"
        grabProcessor.totalEventCount == 1

        and: "the cache state of the sensor should be 'off'"
        controllerService.getCache().queryStatus(sensorId) == "off"

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
