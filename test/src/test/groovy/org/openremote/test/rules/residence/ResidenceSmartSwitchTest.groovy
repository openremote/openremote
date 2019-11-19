package org.openremote.test.rules.residence

import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.HOURS
import static org.openremote.manager.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_APARTMENT_1

class ResidenceSmartSwitchTest extends Specification implements ManagerContainerTrait {

    static final double CYCLE_TIME_SECONDS = 2.5 * 60 * 60

    def "Set mode ON_AT and begin/end cycle time in future"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerDemoSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/demo/rules/DemoResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert !kitchen.getAttribute("smartSwitchModeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "mode is set to ON_AT"
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchModeA", Values.create("ON_AT"))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be ON_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "ON_AT"
            assert !kitchen.getAttribute("smartSwitchBeginEndA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "begin/end cycle time is set to future"
        long oneMinuteInFutureMillis = getClockTimeOf(container) + 60000
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchBeginEndA", Values.create(oneMinuteInFutureMillis))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be ON_AT and actuator enabled with correct start/stop time"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "ON_AT"
            assert kitchen.getAttribute("smartSwitchBeginEndA").get().getValueAsNumber().get() == (double)oneMinuteInFutureMillis
            assert kitchen.getAttribute("smartSwitchStartTimeA").get().getValueAsNumber().get() == Math.floor((double)oneMinuteInFutureMillis/1000)
            assert kitchen.getAttribute("smartSwitchStopTimeA").get().getValueAsNumber().get() == Math.floor((double)oneMinuteInFutureMillis/1000) + CYCLE_TIME_SECONDS
            assert kitchen.getAttribute("smartSwitchEnabledA").get().getValueAsNumber().get() == 1
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "time advances beyond end of cycle"
        advancePseudoClock(5, HOURS, container)

        then: "mode should be NOW_ON and actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "NOW_ON"
            assert !kitchen.getAttribute("smartSwitchBeginEndA").get().getValue().isPresent()
            assert kitchen.getAttribute("smartSwitchStartTimeA").get().getValueAsNumber().get() == 0
            assert kitchen.getAttribute("smartSwitchStopTimeA").get().getValueAsNumber().get() == 0
            assert kitchen.getAttribute("smartSwitchEnabledA").get().getValueAsNumber().get() == 0
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Set mode ON_AT and begin/end cycle time in past"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerDemoSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/demo/rules/DemoResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert !kitchen.getAttribute("smartSwitchModeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "mode is set to ON_AT"
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchModeA", Values.create("ON_AT"))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be ON_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "ON_AT"
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "begin/end cycle time is set to past"
        long fiveMinutesInPastMillis = getClockTimeOf(container) - (5 * 60 * 1000)
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchBeginEndA", Values.create(fiveMinutesInPastMillis))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be ON_AT and actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "ON_AT"
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Set mode READY_AT and begin/end cycle time in future"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerDemoSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/demo/rules/DemoResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert !kitchen.getAttribute("smartSwitchModeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "mode is set to READY_AT"
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchModeA", Values.create("READY_AT"))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "READY_AT"
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "begin/end cycle time is set to future"
        long threeHoursInFutureMillis = getClockTimeOf(container) + (3 * 60 * 60 * 1000)
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchBeginEndA", Values.create(threeHoursInFutureMillis))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT and actuator enabled with correct start/stop time"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "READY_AT"
            assert kitchen.getAttribute("smartSwitchBeginEndA").get().getValueAsNumber().get() == (double)threeHoursInFutureMillis
            assert kitchen.getAttribute("smartSwitchStartTimeA").get().getValueAsNumber().get() == Math.floor((double)getClockTimeOf(container)/1000)
            assert kitchen.getAttribute("smartSwitchStopTimeA").get().getValueAsNumber().get() == Math.floor((double)(threeHoursInFutureMillis/1000))
            assert kitchen.getAttribute("smartSwitchEnabledA").get().getValueAsNumber().get() == 1
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "time advances beyond end of cycle"
        advancePseudoClock(5, HOURS, container)

        then: "mode should be NOW_ON and actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "NOW_ON"
            assert !kitchen.getAttribute("smartSwitchBeginEndA").get().getValue().isPresent()
            assert kitchen.getAttribute("smartSwitchStartTimeA").get().getValueAsNumber().get() == 0
            assert kitchen.getAttribute("smartSwitchStopTimeA").get().getValueAsNumber().get() == 0
            assert kitchen.getAttribute("smartSwitchEnabledA").get().getValueAsNumber().get() == 0
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchBeginEndC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Set mode READY_AT and begin/end cycle time in past"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerDemoSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/demo/rules/DemoResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert !kitchen.getAttribute("smartSwitchModeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "mode is set to READY_AT"
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchModeA", Values.create("READY_AT"))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "READY_AT"
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "begin/end cycle time is set to past"
        long fiveMinutesInPastMillis = getClockTimeOf(container) - (5 * 60 * 1000)
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchBeginEndA", Values.create(fiveMinutesInPastMillis))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "READY_AT"
            assert kitchen.getAttribute("smartSwitchBeginEndA").get().getValueAsNumber().get() == (double)fiveMinutesInPastMillis
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Set mode READY_AT and insufficient begin/end cycle time"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerDemoSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/demo/rules/DemoResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert !kitchen.getAttribute("smartSwitchModeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "mode is set to READY_AT"
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchModeA", Values.create("READY_AT"))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "READY_AT"
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        when: "begin/end cycle time is set insufficient future time"
        long fiveMinutesInFuture = getClockTimeOf(container) + (5 * 60 * 1000)
        assetProcessingService.sendAttributeEvent(
                new AttributeEvent(managerDemoSetup.apartment1KitchenId, "smartSwitchBeginEndA", Values.create(fiveMinutesInFuture))
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerDemoSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValueAsString().get() == "READY_AT"
            assert kitchen.getAttribute("smartSwitchBeginEndA").get().getValueAsNumber().get() == (double)fiveMinutesInFuture
            assert !kitchen.getAttribute("smartSwitchStartTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledA").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledB").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchModeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStartTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchStopTimeC").get().getValue().isPresent()
            assert !kitchen.getAttribute("smartSwitchEnabledC").get().getValue().isPresent()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}
