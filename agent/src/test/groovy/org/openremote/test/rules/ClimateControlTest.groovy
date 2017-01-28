package org.openremote.test.rules

import org.kie.api.KieServices
import org.kie.api.io.Resource
import org.openremote.agent.AgentService
import org.openremote.agent.rules.RulesProvider
import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable
import spock.util.concurrent.PollingConditions
import spock.lang.Ignore

import java.util.stream.Stream

class ClimateControlTest extends Specification implements ContainerTrait {

    @Ignore
    def "Climate control basic test template"() {

        given: "a deployment with commands and sensors"
        def deploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/climatecontrol/agent.xml"
        )
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)

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

        and: "the started server"
        def testCommandBuilder = new TestCommandBuilder();
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        // TODO Write tests

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    @Ignore
    def "Person sense test"() {

        given: "a deployment with commands and sensors"
        def deploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/climatecontrol/agent.xml"
        )
 //       def conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)
        def result = new BlockingVariable<String>()

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

        and: "the started server"
        def testCommandBuilder = new TestCommandBuilder();
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        // TODO Write tests

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Estimate times initial values check"() {

        given: "a deployment with commands and sensors"
        def deploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/climatecontrol/agent.xml"
        )
        def conditions = new PollingConditions(timeout: 1, initialDelay: 0.01)

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

        and: "the started server"
//        def testCommandBuilder = new TestCommandBuilder();
        def testCommandBuilder = new CustomTimeCommandBuilder();
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        when: "give some time for initialization rules"
//        agentService.getContext().getCommands().execute("FS.Tinside.dec.ON")
//        Thread.sleep(10)
        testCommandBuilder.currentTime = "7:00:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "10:27:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "17:55:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "23:58:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "23:59:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "24:00:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "24:01:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "24:02:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "24:03:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "48:04:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "72:05:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "96:06:00"
        Thread.sleep(1000)
        testCommandBuilder.currentTime = "120:07:00"

        then: "check all defaults"
        conditions.eventually {
//            assert agentService.getContext().queryValue("VR1.ET") == "--:--"
//            assert agentService.getContext().queryValue("VR1.ET.inc") == "OFF"
//            assert agentService.getContext().queryValue("VR1.ET.dec") == "OFF"
//            assert agentService.getContext().queryValue("VETA.inc") == "OFF"
//            assert agentService.getContext().queryValue("VETA.dec") == "OFF"
//            assert agentService.getContext().queryValue("VETD.inc") == "OFF"
//            assert agentService.getContext().queryValue("VETD.dec") == "OFF"
//            assert agentService.getContext().queryValue("VR1.COMFORT") == "20.0°"
//            assert agentService.getContext().queryValue("VR1.COMFORT.inc") == "OFF"
//            assert agentService.getContext().queryValue("VR1.COMFORT.dec") == "OFF"
//            assert agentService.getContext().queryValue("VR1.TEMPERATURE") == "19.5\u00B0"
//            assert agentService.getContext().queryValue("VR1.TEMPERATURE.inc") == "OFF"
//            assert agentService.getContext().queryValue("VR1.TEMPERATURE.dec") == "OFF"
//            assert agentService.getContext().queryValue("VNEXTACTION") == "-"
//            assert agentService.getContext().queryValue("VPERSONSENSETIME") == "-"
//            assert agentService.getContext().queryValue("VLEAVES") == "1.0"
//            assert agentService.getContext().queryValue("VSCORE") == "0"
//            assert agentService.getContext().queryValue("VHEATINGSETPOINT") == "16"
//            assert agentService.getContext().queryValue("VWINDOW") == "Closed"
//            assert agentService.getContext().queryValue("VADVICEDONE") == "OFF"
//            assert agentService.getContext().queryValue("VSUMMER") == "No"
//            assert agentService.getContext().queryValue("VADVICE") == "You're doing great!"
//            assert agentService.getContext().queryValue("VACATION.inc") == "OFF"
//            assert agentService.getContext().queryValue("VACATION.dec") == "OFF"
//            assert agentService.getContext().queryValue("VACATION") == "0"
//            assert agentService.getContext().queryValue("VTOTALSCORE") == "0"
//            assert agentService.getContext().queryValue("VLEVEL") == "0"
//            assert agentService.getContext().queryValue("VATA") == "-"
//            assert agentService.getContext().queryValue("VATD") == "-"
//            assert agentService.getContext().queryValue("VPRESENCE") == "No"
//            assert agentService.getContext().queryValue("GVconfig") == "OpenRemote"
////            assert agentService.getContext().queryValue("VArrivalBackground") == "on"
////            assert agentService.getContext().queryValue("VDepartureBackground") == "off"
//            assert agentService.getContext().queryValue("FS.Toutside") == "15°"
//            assert agentService.getContext().queryValue("FS.Tinside") == "19.5°"
//            assert agentService.getContext().queryValue("FS.PIR") == "off"
//            assert agentService.getContext().queryValue("FS.Window") == "off"
//            assert agentService.getContext().queryValue("VETA.Mon") == "09:00"
//            assert agentService.getContext().queryValue("VETD.Mon") == "17:00"
//            assert agentService.getContext().queryValue("VETA.Tue") == "09:00"
//            assert agentService.getContext().queryValue("VETD.Tue") == "17:00"
//            assert agentService.getContext().queryValue("VETA.Wed") == "09:00"
//            assert agentService.getContext().queryValue("VETD.Wed") == "17:00"
//            assert agentService.getContext().queryValue("VETA.Thu") == "09:00"
//            assert agentService.getContext().queryValue("VETD.Thu") == "17:00"
//            assert agentService.getContext().queryValue("VETA.Fri") == "09:00"
//            assert agentService.getContext().queryValue("VETD.Fri") == "17:00"
//            assert agentService.getContext().queryValue("VETA.Sat") == "09:00"
//            assert agentService.getContext().queryValue("VETD.Sat") == "17:00"
//            assert agentService.getContext().queryValue("VETA.Sun") == "09:00"
//            assert agentService.getContext().queryValue("VETD.Sun") == "17:00"
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}
