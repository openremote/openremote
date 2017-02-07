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
        def testCommandBuilder = new CustomTimeCommandBuilder();
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        def sensor = { $sensor -> agentService.getContext().queryValue($sensor) }
        def command = { $name, $arg -> agentService.getContext().getCommands().execute($name, $arg) }
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)

        // TODO Write tests

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    @Ignore
    def "Estimate times initial values check"() {

        given: "a deployment with commands and sensors"
        def deploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/climatecontrol/agent.xml"
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

        and: "the started server"
        def testCommandBuilder = new CustomTimeCommandBuilder();
        testCommandBuilder.currentTime = "07:07:00"
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        def sensor = { $sensor -> agentService.getContext().queryValue($sensor) }
        def command = { $name, $arg -> agentService.getContext().getCommands().execute($name, $arg) }

        // TODO: wait for answer from Drools forum how it should be used. It does not work now.
        // KieSession ksession = agentService.getContext().getDeployment().getRuleEngine().getKnowledgeSession()
        // PseudoClockScheduler clock = new PseudoClockScheduler(ksession)

        when: "give some time for initialization rules"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)

        then: "check all defaults"
        conditions.eventually {
            assert sensor("VR1.ET") == "09:00"
            assert sensor("VR1.ET.inc") == "OFF"
            assert sensor("VR1.ET.dec") == "OFF"
            assert sensor("VETA.inc") == "OFF"
            assert sensor("VETA.dec") == "OFF"
            assert sensor("VETD.inc") == "OFF"
            assert sensor("VETD.dec") == "OFF"
            assert sensor("VR1.COMFORT") == "20.0°"
            assert sensor("VR1.COMFORT.inc") == "OFF"
            assert sensor("VR1.COMFORT.dec") == "OFF"
            assert sensor("VR1.TEMPERATURE") == "16.0\u00B0" // ECO set by heating setpoint ECO rule
            assert sensor("VR1.TEMPERATURE.inc") == "OFF"
            assert sensor("VR1.TEMPERATURE.dec") == "OFF"
            assert sensor("VNEXTACTION") == "Arrive"
            assert sensor("VPERSONSENSETIME") == "-"
            assert sensor("VLEAVES") == "3.0"
            assert sensor("VSCORE") == "4"
            assert Double.parseDouble(sensor("VHEATINGSETPOINT")) == 16
            assert sensor("VWINDOW") == "Closed"
            assert sensor("VADVICEDONE") == "OFF"
            assert sensor("VSUMMER") == "No"
            assert sensor("VADVICE") == "You're doing great!"
            assert sensor("VACATION.inc") == "OFF"
            assert sensor("VACATION.dec") == "OFF"
            assert sensor("VACATION") == "0"
            assert sensor("VTOTALSCORE") == "0"
            assert sensor("VLEVEL") == "0"
            assert sensor("VATA") == "-"
            assert sensor("VATD") == "-"
            assert sensor("VPRESENCE") == "No"
            assert sensor("GVconfig") == "OpenRemote"
            assert sensor("VArrivalBackground") == "on"
            assert sensor("VDepartureBackground") == "on"
            assert sensor("FS.Toutside") == "15°"
            assert sensor("FS.Tinside") == "19.5°"
            assert sensor("FS.PIR") == "off"
            assert sensor("FS.Window") == "off"
            assert sensor("VETA.Mon") == "09:00"
            assert sensor("VETD.Mon") == "17:00"
            assert sensor("VETA.Tue") == "09:00"
            assert sensor("VETD.Tue") == "17:00"
            assert sensor("VETA.Wed") == "09:00"
            assert sensor("VETD.Wed") == "17:00"
            assert sensor("VETA.Thu") == "09:00"
            assert sensor("VETD.Thu") == "17:00"
            assert sensor("VETA.Fri") == "09:00"
            assert sensor("VETD.Fri") == "17:00"
            assert sensor("VETA.Sat") == "09:00"
            assert sensor("VETD.Sat") == "17:00"
            assert sensor("VETA.Sun") == "09:00"
            assert sensor("VETD.Sun") == "17:00"
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    @Ignore
    def "Person sense test"() {

        given: "a deployment with commands and sensors"
        def deploymentXml = getClass().getResourceAsStream(
                "/org/openremote/test/rules/climatecontrol/agent.xml"
        )
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
        def testCommandBuilder = new CustomTimeCommandBuilder();
        def time = { $time -> testCommandBuilder.currentTime = $time }
        time "8:25:00"
        def agentService = new AgentService(
                deploymentXml,
                testCommandBuilder,
                rulesProvider
        )
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), [agentService])

        def sensor = { $sensor -> agentService.getContext().queryValue($sensor) }
        def command = { $name, $arg -> agentService.getContext().getCommands().execute($name, $arg) }

        when: "Set time before going to office"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)

        then: "Check if there is no person"
        conditions.eventually {
            assert sensor("VNEXTACTION") == "Arrive"
            assert sensor("FS.PIR") != "on"
            assert sensor("VPERSONSENSE") == "0"
            assert sensor("VPRESENCE") == "No"
        }

        when: "now let's enter the office at 10 AM"
        time "10:00:00"
        command "FS.PIR", "on"
        conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)

        then: "see if the arrival is registered"
        conditions.eventually {
            assert sensor("VNEXTACTION") == "Departure"
            assert sensor("VATA") == "10:00"
            assert sensor("VPERSONSENSE") == "0"
            assert sensor("VPRESENCE") == "Yes"
            assert sensor("FS.PIR") == "off"
        }

        when: "go home at 4PM"
        time "16:00:00"
        command "FS.PIR", "on"
        conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)

        then: "wait for PIR off"
        conditions.eventually {
            assert sensor("VPERSONSENSE") == "1"
            assert sensor("FS.PIR") == "off"
        }

        when: "Go at midnight adjusting"
        time "23:58:00"
        conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)

        then: "check departure"
        conditions.eventually {
            assert sensor("VPRESENCE") == "No"
            assert sensor("VATD") == "16:00"
            assert sensor("VNEXTACTION") == "-"
        }

        when: "Check updated arrival and departure times"
        time "23:59:00"
        conditions = new PollingConditions(timeout: 10, initialDelay: 0.25)

        then: "see if times schifted by half an hour"
        conditions.eventually {
            def dow = sensor("TimerEEE")
            assert sensor("VETA.$dow") == "09:30"
            assert sensor("VETD.$dow") == "16:30"
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}