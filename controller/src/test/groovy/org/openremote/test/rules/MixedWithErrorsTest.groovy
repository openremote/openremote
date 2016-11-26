package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.controller.ControllerService
import org.openremote.controller.event.CommandFacade
import org.openremote.controller.event.EventProcessorChain
import org.openremote.controller.event.RangeEvent
import org.openremote.controller.event.SwitchEvent
import org.openremote.controller.model.RangeSensor
import org.openremote.controller.rules.RuleEngine
import org.openremote.test.ContainerTrait
import org.openremote.test.util.EventGrabProcessor
import org.openremote.test.util.TestEventProducerCommand
import spock.lang.Specification

import java.util.stream.Stream

/**
 * Test deployment behavior when some of the rule definitions have
 * errors or can't be deployed. Make sure the correct ones still
 * operate.
 */
class MixedWithErrorsTest extends Specification implements ContainerTrait {

    def "Sensor event replaced by rule-triggered event"() {

        given: "some sensor event processors and rules"
        def ruleEngineProcessor = new RuleEngine() {
            @Override
            protected Stream<Resource> getResources(KieServices kieServices) {
                Stream.of(
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/mixed-with-errors/EventMod.drl"
                        ),
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/mixed-with-errors/IncorrectRule.drl"
                        ),
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/mixed-with-errors/BrokenDTable.csv"
                        ),
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/mixed-with-errors/DTableTest.csv"
                        )
                )
            }
        }
        def grabProcessor = new EventGrabProcessor()

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

        when: "a switch event is dispatched"
        def switchEvent = new SwitchEvent(1, "test", "on", SwitchEvent.State.ON);
        controllerService.getCache().update(switchEvent)

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100)
        def lastEvent = grabProcessor.lastEvent

        then: "the event should not be modified"
        lastEvent.getSourceID() == 1
        lastEvent.getSource() == "test"
        lastEvent.getValue() == "on"
        lastEvent.serialize() == "on"

        and: "the total event couht should match"
        grabProcessor.totalEventCount == 1

        when: "a range sensor is registered"
        controllerService.getCache().registerSensor(
                new RangeSensor(
                        "test level mod",
                        123,
                        controllerService.getCache(),
                        new TestEventProducerCommand(),
                        1,
                        0,
                        1000
                )
        )

        and: "a range event is dispatched"
        def rangeEvent = new RangeEvent(123, "test level mod", 30, 0, 1000);
        controllerService.getCache().update(rangeEvent)

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100)
        lastEvent = grabProcessor.lastEvent

        then: "the event should be modified/replaced"
        lastEvent.getSourceID() == 123
        lastEvent.getSource() == "test level mod"
        lastEvent.getValue() == 321
        lastEvent.serialize() == "321"
        lastEvent instanceof RangeEvent
        lastEvent != rangeEvent // Must be replaced by rule!

        and: "the total event couht should match"
        grabProcessor.totalEventCount == 2

        when: "another range sensor is registered"
        controllerService.getCache().registerSensor(
                new RangeSensor(
                        "test level mod 555",
                        555,
                        controllerService.getCache(),
                        new TestEventProducerCommand(),
                        1,
                        0,
                        10000
                )
        )

        and: "a range event is dispatched"
        rangeEvent = new RangeEvent(555, "test level mod 555", 101, 0, 10000)
        controllerService.getCache().update(rangeEvent)

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100)
        lastEvent = grabProcessor.lastEvent

        then: "the event should be modified/replaced"
        lastEvent.getSourceID() == 555
        lastEvent.getSource() == "test level mod 555"
        lastEvent.getValue() == 55
        lastEvent.serialize() == "55"
        lastEvent instanceof RangeEvent
        lastEvent != rangeEvent // Must be replaced by rule!

        and: "the total event couht should match"
        grabProcessor.totalEventCount == 3

        when: "another range sensor is registered"
        controllerService.getCache().registerSensor(
                new RangeSensor(
                        "test level mod 666",
                        666,
                        controllerService.getCache(),
                        new TestEventProducerCommand(),
                        1,
                        0,
                        10000
                )
        )

        and: "a range event is dispatched"
        rangeEvent = new RangeEvent(666, "test level mod 666", 1, 1, 10000)
        controllerService.getCache().update(rangeEvent)

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100)
        lastEvent = grabProcessor.lastEvent

        then: "the event should be modified/replaced"
        lastEvent.getSourceID() == 666
        lastEvent.getSource() == "test level mod 666"
        lastEvent.getValue() == 6666
        lastEvent.serialize() == "6666"
        lastEvent instanceof RangeEvent
        lastEvent != rangeEvent // Must be replaced by rule!

        and: "the total event couht should match"
        grabProcessor.totalEventCount == 4

        when: "another range sensor is registered"
        controllerService.getCache().registerSensor(
                new RangeSensor(
                        "test level mod 777",
                        777,
                        controllerService.getCache(),
                        new TestEventProducerCommand(),
                        1,
                        -100,
                        100
                )
        )

        and: "a range event is dispatched"
        rangeEvent = new RangeEvent(777, "test level mod 777", 10, -100, 100)
        controllerService.getCache().update(rangeEvent)

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100)
        lastEvent = grabProcessor.lastEvent

        then: "the event should be modified/replaced"
        lastEvent.getSourceID() == 777
        lastEvent.getSource() == "test level mod 777"
        lastEvent.getValue() == -77
        lastEvent.serialize() == "-77"
        lastEvent instanceof RangeEvent
        lastEvent != rangeEvent // Must be replaced by rule!

        and: "the total event couht should match"
        grabProcessor.totalEventCount == 5

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
