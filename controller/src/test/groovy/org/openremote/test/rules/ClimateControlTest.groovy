package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.controller.ControllerService
import org.openremote.controller.rules.RulesProvider
import org.openremote.test.ContainerTrait
import spock.lang.Specification

import java.util.stream.Stream

class ClimateControlTest extends Specification implements ContainerTrait {

    def "Climate control basic test"() {

        given: "a controller deployment with commands and sensors"
        def controllerDeploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/climatecontrol/controller.xml"
        )

        and: "some rules"
        def rulesProvider = new RulesProvider() {
            @Override
            Stream<Resource> getResources(KieServices kieServices) {
                Stream.of(
                        kieServices.getResources().newClassPathResource(
                                "org/openremote/test/rules/climatecontrol/ClimateControl.drl"
                        )
                )
            }
        }

        and: "the started controller server"
        def testCommandBuilder = new TestCommandBuilder();
        def controllerService = new ControllerService(
                controllerDeploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def services = Stream.of(controllerService)
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), services)

        // TODO Write tests

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
