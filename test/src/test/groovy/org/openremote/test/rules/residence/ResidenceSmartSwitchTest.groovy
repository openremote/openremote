package org.openremote.test.rules.residence

import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static java.util.concurrent.TimeUnit.HOURS
import static org.openremote.setup.integration.ManagerTestSetup.DEMO_RULE_STATES_APARTMENT_1

@Ignore
class ResidenceSmartSwitchTest extends Specification implements ManagerContainerTrait {

    static final double CYCLE_TIME_MILLISECONDS = 2.5 * 60 * 60 * 1000

    def "Set mode ON_AT and begin/end cycle time in future"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerTestSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/ResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerTestSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchModeA", "ON_AT")
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be ON_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "ON_AT"
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchBeginEndA", oneMinuteInFutureMillis)
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be ON_AT and actuator enabled with correct start/stop time"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "ON_AT"
            assert kitchen.getAttribute("smartSwitchBeginEndA").flatMap{it.value}.orElse(0l) == oneMinuteInFutureMillis
            assert kitchen.getAttribute("smartSwitchStartTimeA").flatMap{it.value}.orElse(0l) == oneMinuteInFutureMillis
            assert kitchen.getAttribute("smartSwitchStopTimeA").flatMap{it.value}.orElse(0l) == oneMinuteInFutureMillis + CYCLE_TIME_MILLISECONDS
            assert kitchen.getAttribute("smartSwitchEnabledA").flatMap{it.value}.orElse(0l) == 1
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
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "NOW_ON"
            assert !kitchen.getAttribute("smartSwitchBeginEndA").get().getValue().isPresent()
            assert kitchen.getAttribute("smartSwitchStartTimeA").flatMap{it.value}.orElse(0l) == 0
            assert kitchen.getAttribute("smartSwitchStopTimeA").flatMap{it.value}.orElse(0l) == 0
            assert kitchen.getAttribute("smartSwitchEnabledA").flatMap{it.value}.orElse(0l) == 0
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

            }

    def "Set mode ON_AT and begin/end cycle time in past"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerTestSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/ResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerTestSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchModeA", "ON_AT")
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be ON_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "ON_AT"
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchBeginEndA", fiveMinutesInPastMillis)
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be ON_AT and actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "ON_AT"
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

            }

    def "Set mode READY_AT and begin/end cycle time in future"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerTestSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/ResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerTestSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchModeA", "READY_AT")
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "READY_AT"
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchBeginEndA", threeHoursInFutureMillis)
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT and actuator enabled with correct start/stop time"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "READY_AT"
            assert kitchen.getAttribute("smartSwitchBeginEndA").flatMap{it.value}.orElse(0l) == threeHoursInFutureMillis
            assert kitchen.getAttribute("smartSwitchStartTimeA").flatMap{it.value}.orElse(0l) == getClockTimeOf(container)
            assert kitchen.getAttribute("smartSwitchStopTimeA").flatMap{it.value}.orElse(0l) == threeHoursInFutureMillis
            assert kitchen.getAttribute("smartSwitchEnabledA").flatMap{it.value}.orElse(0l) == 1
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
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "NOW_ON"
            assert !kitchen.getAttribute("smartSwitchBeginEndA").get().getValue().isPresent()
            assert kitchen.getAttribute("smartSwitchStartTimeA").flatMap{it.value}.orElse(0l) == 0
            assert kitchen.getAttribute("smartSwitchStopTimeA").flatMap{it.value}.orElse(0l) == 0
            assert kitchen.getAttribute("smartSwitchEnabledA").flatMap{it.value}.orElse(0l) == 0
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

            }

    def "Set mode READY_AT and begin/end cycle time in past"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerTestSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/ResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerTestSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchModeA", "READY_AT")
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "READY_AT"
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchBeginEndA", fiveMinutesInPastMillis)
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "READY_AT"
            assert kitchen.getAttribute("smartSwitchBeginEndA").flatMap{it.value}.orElse(0l) == fiveMinutesInPastMillis
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

            }

    def "Set mode READY_AT and insufficient begin/end cycle time"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        RulesEngine apartment1Engine = null

        and: "some rules"
        Ruleset ruleset = new AssetRuleset(
            managerTestSetup.apartment1Id,
            "Demo Apartment - Smart Start",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/ResidenceSmartSwitch.groovy").text)
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            apartment1Engine = rulesService.assetEngines.get(managerTestSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment1Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_1
        }

        and: "smart switches should be off"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchModeA", "READY_AT")
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "READY_AT"
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
                new AttributeEvent(managerTestSetup.apartment1KitchenId, "smartSwitchBeginEndA", fiveMinutesInFuture)
        )
        noEventProcessedIn(assetProcessingService, 500)

        then: "mode should be READY_AT but actuator not enabled"
        conditions.eventually {
            def kitchen = assetStorageService.find(managerTestSetup.apartment1KitchenId, true)
            assert kitchen.getAttribute("smartSwitchModeA").get().getValue().get() == "READY_AT"
            assert kitchen.getAttribute("smartSwitchBeginEndA").flatMap{it.value}.orElse(0l) == fiveMinutesInFuture
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

            }

}
