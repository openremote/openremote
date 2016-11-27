package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.controller.ControllerService
import org.openremote.controller.rules.RuleEngine
import org.openremote.test.ContainerTrait
import org.openremote.test.util.EventGrabProcessor
import spock.lang.Specification

import java.util.stream.Stream

class ClimateControlTest extends Specification implements ContainerTrait {

    def "Climate control basic test"() {

        given: "a controller deployment"
        def controllerDeploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/climatecontrol/controller.xml"
        )

        and: "event processors and rules"
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


        and: "the started controller server"
        def testCommandFactory = new TestCommandFactory();
        def controllerService = new ControllerService(
                controllerDeploymentXml,
                testCommandFactory,
                ruleEngineProcessor,
                grabProcessor
        )
        def services = Stream.of(controllerService)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), services)

        // TODO Write tests

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
