package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.agent.AgentService
import org.openremote.agent.rules.RulesProvider
import org.openremote.agent.sensor.CustomSensorState
import org.openremote.test.ContainerTrait
import spock.lang.Specification

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
        def testCommandBuilder = new TestCommandBuilder();
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        when: "we increase Sp"
        // can we run PID.sp.inc.ON command instead?
        def customStateEvent = new CustomSensorState(358537, "PID.sp.inc", "ON");
        agentService.getContext().update(customStateEvent);

        and: "we wait"
        Thread.sleep(1000);
        
        then: "set point should be 1.5"
        agentService.getContext().queryValue(358531) == "1.5" // can we check sensors by name?
        agentService.getContext().queryValue(358532) == "1.5000" // PID output

        when: "we decrease Sp"
        customStateEvent = new CustomSensorState(358525, "PID.sp.dec", "ON");
        agentService.getContext().update(customStateEvent);

        and: "we wait"
        Thread.sleep(1000);

        then: "set point should be 1.0"
        agentService.getContext().queryValue(358531) == "1.0" // set point
        agentService.getContext().queryValue(358532) == "1.0000" // PID output

        // how can I test if there is no overshot?
        // how can I test if there are no oscillations?

        // Problem: sometimes it passes, sometimes fails...

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
