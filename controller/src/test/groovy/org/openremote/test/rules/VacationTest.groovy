package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.controller.ControllerService
import org.openremote.controller.event.CustomStateEvent
import org.openremote.controller.event.SwitchEvent
import org.openremote.controller.rules.RuleEngine
import org.openremote.test.ContainerTrait
import spock.lang.Specification

import java.util.stream.Stream

import static org.kie.api.io.ResourceType.DRL

class VacationTest extends Specification implements ContainerTrait {

    def "Vacation example"() {

        given: "a controller deployment"
        def controllerDeploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/vacation/controller.xml"
        )

        and: "event processors and rules"
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

        and: "the started controller server"
        def testCommandBuilder = new TestCommandBuilder();
        def controllerService = new ControllerService(
                controllerDeploymentXml,
                testCommandBuilder,
                ruleEngineProcessor
        )
        def services = Stream.of(controllerService)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), services)

        when: "the time of day is day"
        def customStateEvent = new CustomStateEvent(123, "time of day", "day");
        controllerService.getContext().update(customStateEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "21"

        when: "the time of day is night"
        customStateEvent = new CustomStateEvent(123, "time of day", "night");
        controllerService.getContext().update(customStateEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "18"

        when: "we go on vacation"
        def switchEvent = new SwitchEvent(789, "vacation start", "on", SwitchEvent.State.ON);
        controllerService.getContext().update(switchEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "15"

        when: "the time of day is day"
        customStateEvent = new CustomStateEvent(123, "time of day", "day");
        controllerService.getContext().update(customStateEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "15"

        when: "we manually set the temperature"
        controllerService.getContext().getCommands().execute("temp", 19)

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "19"

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
