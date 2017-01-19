package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.agent.AgentService
import org.openremote.agent.rules.RulesProvider
import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.stream.Stream

class PIDTest extends Specification implements ContainerTrait {

    def "PID controller basic test"() {

        given: "a deployment with commands and sensors"
        def deploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/pid/agent.xml"
        )
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.5, factor: 0.25)

        and: "some rules"
        def rulesProvider = new RulesProvider() {
            @Override
            Stream<Resource> getResources(KieServices kieServices) {
                Stream.of(
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/pid/PID.drl"
                        )
                )
            }
        }

        and: "the started server"
        def testCommandBuilder = new TestCommandBuilder()
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        when: "we increase Sp"
        // 2 ways of increasing the set point.
        // Set the value of sensor directly:
        //def customStateEvent = new CustomSensorState(358537, "PID.sp.inc", "ON");
        //agentService.getContext().update(customStateEvent);
        // Send a command linked with the button:
        agentService.getContext().getCommands().execute("PID.sp.inc.ON")

        then: "set point should be 1.5"
        conditions.eventually {
            assert agentService.getContext().queryValue("GV.PID.Sp") == "1.5"
            assert agentService.getContext().queryValue("PID.Output") == "1.5000"
        }


        when: "we decrease Sp"
        agentService.getContext().getCommands().execute("PID.sp.dec.ON")

        then: "the PID output should be 1.0"
        conditions.eventually {
            assert agentService.getContext().queryValue("GV.PID.Sp") == "1.0" // set point
            assert agentService.getContext().queryValue("PID.Output") == "1.0000" // PID output
        }

        // TODO: how can I test if there is no overshot?
        // TODO: how can I test if there are no oscillations?

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
