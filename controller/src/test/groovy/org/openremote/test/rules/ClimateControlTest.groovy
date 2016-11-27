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

class ClimateControlTest extends Specification implements ContainerTrait {

    def "Climate control basic test"() {

        given: "event processors and rules"
        def ruleEngineProcessor = new RuleEngine() {
            @Override
            protected Stream<Resource> getResources(KieServices kieServices) {
                Stream.of(
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/climatecontrol/ClimateControl.drl"
                        )
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

        // TODO Write tests

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
