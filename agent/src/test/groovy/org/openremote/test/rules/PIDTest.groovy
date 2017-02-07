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

        def sensor = { $sensor -> Double.parseDouble(agentService.getContext().queryValue($sensor)) }
        def command = { $name -> agentService.getContext().getCommands().execute($name) }
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)


        when: "we increase Sp"
        // 2 ways of increasing the set point.
        // Set the value of sensor directly:
        //def customStateEvent = new CustomSensorState(358537, "PID.sp.inc", "ON");
        //agentService.getContext().update(customStateEvent);
        // Send a command linked with the button:
        command("PID.sp.inc.ON")

        then: "set point should be 1.5"
        conditions.eventually {
            assert sensor("GV.PID.Sp") == 1.5
            assert sensor("PID.Output") == 1.5
        }

        when: "we decrease Sp"
        command("PID.sp.dec.ON")
        conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)

        then: "the PID output should be 1.0"
        conditions.eventually {
            assert sensor("GV.PID.Sp") == 1 // set point
            assert sensor("PID.Output") == 1 // PID output
        }

        // TODO: how can I test if there is no overshot?
        // TODO: how can I test if there are no oscillations?

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
