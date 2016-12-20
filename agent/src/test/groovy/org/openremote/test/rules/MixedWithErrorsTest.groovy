package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.agent.AgentService
import org.openremote.agent.rules.RulesProvider
import org.openremote.test.ContainerTrait
import spock.lang.Specification

import java.util.stream.Stream

/**
 * Test deployment behavior when some of the rule definitions have
 * errors or can't be deployed. Make sure the correct ones still
 * operate.
 */
class MixedWithErrorsTest extends Specification implements ContainerTrait {

    def "Execute good rules - ignore broken rules"() {

        given: "a deployment with commands and sensors"
        def deploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/mixed-with-errors/agent.xml"
        )

        and: "some rules"
        def rulesProvider = new RulesProvider() {
            @Override
            Stream<Resource> getResources(KieServices kieServices) {
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

        and: "the started server"
        def testCommandBuilder = new TestCommandBuilder();
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        when: "we wait a bit for initial state and rules to fire"
        Thread.sleep(500)

        then: "the state should match"
        agentService.getContext().queryValue(444) == "12345" // TODO This should be limited to max, which is 1000
        agentService.getContext().queryValue(555) == "55"

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
