package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.agent.AgentService
import org.openremote.agent.rules.RulesProvider
import org.openremote.agent.sensor.CustomSensorState
import org.openremote.agent.sensor.SwitchSensorState
import org.openremote.test.ContainerTrait
import spock.lang.Specification

import java.util.stream.Stream

import static org.kie.api.io.ResourceType.DRL

class VacationTest extends Specification implements ContainerTrait {

    def "Vacation example"() {

        given: "a deployment with commands and sensors"
        def deploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/vacation/agent.xml"
        )

        and: "some rules"
        def rulesProvider = new RulesProvider() {
            @Override
            Stream<Resource> getResources(KieServices kieServices) {
                Stream.of(
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/vacation/Vacation.drl"
                        ).setResourceType(DRL)
                )
            }
        }

        and: "the started server"
        def testCommandBuilder = new TestCommandBuilder();
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        when: "the time of day is day"
        def customStateEvent = new CustomSensorState(123, "time of day", "day");
        agentService.getContext().update(customStateEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "21"

        when: "the time of day is night"
        customStateEvent = new CustomSensorState(123, "time of day", "night");
        agentService.getContext().update(customStateEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "18"

        when: "we go on vacation"
        def switchEvent = new SwitchSensorState(789, "vacation start", "on", SwitchSensorState.State.ON);
        agentService.getContext().update(switchEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "15"

        when: "the time of day is day"
        customStateEvent = new CustomSensorState(123, "time of day", "day");
        agentService.getContext().update(customStateEvent);

        and: "we wait a bit for the rules to fire"
        Thread.sleep(100);

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "15"

        when: "we manually set the temperature"
        agentService.getContext().getCommands().execute("temp", 19)

        then: "the temperature change should be executed"
        testCommandBuilder.lastExecutionArgument == "19"

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
