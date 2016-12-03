package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.agent.AgentService
import org.openremote.agent.rules.RulesProvider
import org.openremote.test.ContainerTrait
import spock.lang.Specification

import java.util.stream.Stream

/**
 * Basic rule engine test, trigger a rule through an event.
 * The rule dispatches a new event, replacing the original event.
 */
class SimpleRuleTest extends Specification implements ContainerTrait {

    def "Sensor event replaced by rule-triggered event"() {

        given: "a deployment with commands and sensors"
        def deploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/simple/agent.xml"
        )

        and: "some rules"
        def rulesProvider = new RulesProvider() {
            @Override
            Stream<Resource> getResources(KieServices kieServices) {
                Stream.of(
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/simple/EventMod.drl"
                        )
                )
            }
        }

        and: "the started server"
        def agentService = new AgentService(
                deploymentXml,
                new TestCommandBuilder(),
                rulesProvider
        )
        def services = Stream.of(agentService)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), services)

        when: "we wait for initial state polling of sensor and rule execution"
        Thread.sleep(500);

        then: "the rules should immediately switch the sensor off again"
        agentService.getContext().queryValue(123) == "off"

        and: "the deployment model should work"
        def deployment = agentService.getContext().getDeployment()
        deployment.getCommandDefinition(456).getCommandID() == 456
        deployment.getCommandDefinition(123123) == null
        deployment.getCommandDefinition("TestDevice", "TestCommand").getCommandID() == 456
        deployment.getCommandDefinition("TestDevice", "NoSuchThing") == null
        deployment.getCommandDefinition("NoSuchThing", "NoSuchThing") == null
        deployment.getCommandDefinition("", "") == null
        deployment.getCommandDefinition(null, "") == null
        deployment.getCommandDefinition("", null) == null
        deployment.getCommandDefinition(null, null) == null
        deployment.getCommandDefinition("TestCommand").getCommandID() == 456
        deployment.getCommandDefinition("NoSuchThing") == null
        deployment.getCommandDefinition("") == null
        deployment.getCommandDefinition(null) == null
        deployment.getDevices().length == 1
        deployment.getDevices()[0].getName() == "TestDevice"
        deployment.getDevices()[0].getDeviceID() == 111
        deployment.getDevice("TestDevice").getName() == "TestDevice"
        deployment.getDevice("TestDevice").getDeviceID() == 111
        deployment.getConfig().get("someString") == "BAR"
        deployment.getConfig().get("someBoolean") == "true"
        deployment.getConfig().get("someInteger") == "333"

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
