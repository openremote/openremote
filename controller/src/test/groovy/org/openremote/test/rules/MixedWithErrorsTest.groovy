package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.controller.ControllerService
import org.openremote.controller.rules.RuleEngine
import org.openremote.test.ContainerTrait
import org.openremote.test.util.EventGrabProcessor
import spock.lang.Specification

import java.util.stream.Stream

/**
 * Test deployment behavior when some of the rule definitions have
 * errors or can't be deployed. Make sure the correct ones still
 * operate.
 */
class MixedWithErrorsTest extends Specification implements ContainerTrait {

    def "Execute good rules - ignore broken rules"() {

        given: "a controller deployment"
        def controllerDeploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/mixed-with-errors/controller.xml"
        )

        and: "some sensor event processors and rules"
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

        when: "we wait a bit for initial state and rules to fire"
        Thread.sleep(500)

        then: "the state should match"
        controllerService.getDataContext().queryValue(444) == "12345" // TODO This should be limited to max, which is 1000
        controllerService.getDataContext().queryValue(555) == "55"

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
