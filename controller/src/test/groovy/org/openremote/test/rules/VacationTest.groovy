package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.controller.ControllerService
import org.openremote.controller.command.ExecutableCommand
import org.openremote.controller.command.ExecutableCommandFactory
import org.openremote.controller.event.CommandFacade
import org.openremote.controller.event.CustomStateEvent
import org.openremote.controller.event.EventProcessorChain
import org.openremote.controller.event.SwitchEvent
import org.openremote.controller.model.CommandDefinition
import org.openremote.controller.rules.RuleEngine
import org.openremote.test.ContainerTrait
import org.openremote.test.util.EventGrabProcessor
import spock.lang.Specification

import java.util.stream.Stream

import static org.kie.api.io.ResourceType.DRL

class VacationTest extends Specification implements ContainerTrait {

    def "Vacation example"() {

        given: "event processors and rules"
        def ruleEngineProcessor = new RuleEngine() {
            @Override
            protected Stream<Resource> getResources(KieServices kieServices) {
                Stream.of(
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/vacation/Vacation.drl"
                        ).setResourceType(DRL)
                )
            }
        }
        def grabProcessor = new EventGrabProcessor();

        String lastCommandValue = null
        int totalCommandCount = 0
        def commandFactory = new ExecutableCommandFactory() {
            @Override
            protected ExecutableCommand buildCommand(CommandDefinition commandDefinition) {
                return new ExecutableCommand() {
                    @Override
                    void send(String value) {
                        lastCommandValue = value
                        totalCommandCount++
                    }
                }
            }
        }
        def commandDefinitions = [
                new CommandDefinition(commandFactory, 11, "test", [(CommandDefinition.NAME_PROPERTY): "temp"])
        ]
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

        when: "the time of day is day"
        def customStateEvent = new CustomStateEvent(222, "time of day", "day");
        controllerService.getCache().update(customStateEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature should be set"
        lastCommandValue == "21"
        totalCommandCount == 1

        when: "the time of day is night"
        customStateEvent = new CustomStateEvent(222, "time of day", "night");
        controllerService.getCache().update(customStateEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature should be set"
        lastCommandValue == "18"
        totalCommandCount == 2

        when: "we go on vacation"
        def switchEvent = new SwitchEvent(333, "vacation start", "on", SwitchEvent.State.ON);
        controllerService.getCache().update(switchEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature should be set"
        lastCommandValue == "15"
        totalCommandCount == 3

        when: "the time of day is day"
        customStateEvent = new CustomStateEvent(222, "time of day", "day");
        controllerService.getCache().update(customStateEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature should be set"
        lastCommandValue == "15"
        totalCommandCount == 1

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
