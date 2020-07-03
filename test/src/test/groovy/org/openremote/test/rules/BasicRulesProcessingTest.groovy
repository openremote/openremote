package org.openremote.test.rules

import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.attribute.MetaItemType
import org.openremote.model.asset.AssetType
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeValueType
import org.openremote.model.attribute.Meta
import org.openremote.model.attribute.MetaItem
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.TenantRuleset
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

import static org.openremote.model.rules.RulesetStatus.*
import static org.openremote.manager.setup.builtin.ManagerDemoSetup.*
import static org.openremote.test.rules.BasicRulesImport.assertRulesFired

class BasicRulesProcessingTest extends Specification implements ManagerContainerTrait {

    def "Check firing when deploying a new ruleset"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakDemoSetup, managerDemoSetup)

        expect: "the rules engines to be ready"
        conditions.eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakDemoSetup, managerDemoSetup)
        }

        and: "the demo attributes marked with RULE_STATE = true meta should be inserted into the engines"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
        }

        when: "a LHS filtering test rule definition is loaded into the Smart Building asset"
        def assetRuleset = new AssetRuleset(
            managerDemoSetup.smartBuildingId,
            "Some Smart Building asset rules",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicSmartHomeMatchAllAssetStates.groovy").text)
        rulesetStorageService.merge(assetRuleset)
        RulesEngine smartHomeEngine = null

        then: "the Smart Building rule engine should have ben created, loaded the new rule definition and facts and started"
        conditions.eventually {
            smartHomeEngine = rulesService.assetEngines.get(managerDemoSetup.smartBuildingId)
            assert smartHomeEngine != null
            assert smartHomeEngine.isRunning()
            assert smartHomeEngine.deployments.size() == 1
            assert smartHomeEngine.deployments.values().any({
                it.name == "Some Smart Building asset rules" && it.status == DEPLOYED
            })
        }

        and: "the new rule engine is fully initialised"
        conditions.eventually {
            assert smartHomeEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.tenantBuildingEngine, 1)
            assertRulesFired(rulesImport.tenantBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
            assertRulesFired(smartHomeEngine, 8)
            assertRulesFired(smartHomeEngine, ["Living Room All", "Kitchen All", "Kitchen Number Attributes", "Parent Type Residence", "Asset Type Room", "Boolean Attributes", "String attributes", "Number value types"])
        }

        when: "an attribute event occurs"
        rulesImport.resetRulesFired(smartHomeEngine)
        def apartment2LivingRoomPresenceDetectedChange = new AttributeEvent(
            managerDemoSetup.apartment2LivingroomId, "presenceDetected", Values.create(true)
        )
        assetProcessingService.sendAttributeEvent(apartment2LivingRoomPresenceDetectedChange)

        then: "the engines in scope should have fired"
        conditions.eventually {
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.tenantBuildingEngine, 1)
            assertRulesFired(rulesImport.tenantBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
            assertRulesFired(smartHomeEngine, 8)
            assertRulesFired(smartHomeEngine, ["Living Room All", "Kitchen All", "Kitchen Number Attributes", "Parent Type Residence", "Asset Type Room", "Boolean Attributes", "String attributes", "Number value types"])
        }
    }

    def "Handle attribute event with no meta, asset create, update, delete"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakDemoSetup, managerDemoSetup)

        expect: "the rules engines to be ready"
        conditions.eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakDemoSetup, managerDemoSetup)
        }

        when: "a Kitchen room asset is inserted into apartment that contains a RULE_STATE = true meta flag"
        rulesImport.resetRulesFired()
        def apartment2 = assetStorageService.find(managerDemoSetup.apartment2Id)
        def asset = new Asset("Kitchen", AssetType.ROOM, apartment2)
        asset.setRealm(keycloakDemoSetup.tenantBuilding.getRealm())
        def attributes = [
            new AssetAttribute("testString", AttributeValueType.STRING, Values.create("test"))
                .setMeta(
                new Meta(new MetaItem(MetaItemType.RULE_STATE, Values.create(true))
                )
            )
        ]
        asset.setAttributes(attributes)
        asset = assetStorageService.merge(asset)

        then: "the rules engines should have executed at least once"
        conditions.eventually {
            assert rulesImport.globalEngine.lastFireTimestamp > 0
            assert rulesImport.masterEngine.lastFireTimestamp > 0
            assert rulesImport.tenantBuildingEngine.lastFireTimestamp > 0
            assert rulesImport.apartment2Engine.lastFireTimestamp > 0
            assert rulesImport.apartment3Engine.lastFireTimestamp > 0
        }

        then: "after a few seconds the engines in scope should have facts and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING + 1
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 1
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.masterEngine, 1)
            assertRulesFired(rulesImport.masterEngine, ["All"])
            assertRulesFired(rulesImport.tenantBuildingEngine, 1)
            assertRulesFired(rulesImport.tenantBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "an attribute event is pushed into the system for an attribute with no RULE_STATE meta"
        def globalLastFireTimestamp = rulesImport.globalEngine.lastFireTimestamp
        def masterLastFireTimestamp = rulesImport.masterEngine.lastFireTimestamp
        def tenantALastFireTimestamp = rulesImport.tenantBuildingEngine.lastFireTimestamp
        def apartment2LastFireTimestamp = rulesImport.apartment2Engine.lastFireTimestamp
        def apartment3LastFireTimestamp = rulesImport.apartment3Engine.lastFireTimestamp
        def apartment2LivingRoomWindowOpenChange = new AttributeEvent(
            managerDemoSetup.apartment2LivingroomId, "windowOpen", Values.create(true)
        )
        assetProcessingService.sendAttributeEvent(apartment2LivingRoomWindowOpenChange)

        then: "the attribute event should have been processed"
        conditions.eventually {
            def apartment2LivingRoom = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert apartment2LivingRoom.getAttribute("windowOpen").flatMap{it.getValueAsBoolean()}.orElse(false)
        }

        then: "the rules engines should not have executed"
        conditions.eventually {
            assert rulesImport.globalEngine.lastFireTimestamp == globalLastFireTimestamp
            assert rulesImport.masterEngine.lastFireTimestamp == masterLastFireTimestamp
            assert rulesImport.tenantBuildingEngine.lastFireTimestamp == tenantALastFireTimestamp
            assert rulesImport.apartment2Engine.lastFireTimestamp == apartment2LastFireTimestamp
            assert rulesImport.apartment3Engine.lastFireTimestamp == apartment3LastFireTimestamp
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "the Kitchen room asset is modified to add a new attribute but RULE_STATE = true meta is not changed"
        globalLastFireTimestamp = rulesImport.globalEngine.lastFireTimestamp
        masterLastFireTimestamp = rulesImport.masterEngine.lastFireTimestamp
        tenantALastFireTimestamp = rulesImport.tenantBuildingEngine.lastFireTimestamp
        apartment2LastFireTimestamp = rulesImport.apartment2Engine.lastFireTimestamp
        apartment3LastFireTimestamp = rulesImport.apartment3Engine.lastFireTimestamp
        attributes = [
            new AssetAttribute("testString", AttributeValueType.STRING, Values.create("test"))
                .setMeta(
                new MetaItem(MetaItemType.RULE_STATE, Values.create(true))
            ),
            new AssetAttribute("testInteger", AttributeValueType.NUMBER, Values.create(0))
        ]
        asset.setAttributes(attributes)
        asset = assetStorageService.merge(asset)

        then: "the rules engines should have executed at least one more time"
        conditions.eventually {
            assert rulesImport.globalEngine.lastFireTimestamp > globalLastFireTimestamp
            assert rulesImport.masterEngine.lastFireTimestamp == masterLastFireTimestamp
            assert rulesImport.tenantBuildingEngine.lastFireTimestamp > tenantALastFireTimestamp
            assert rulesImport.apartment2Engine.lastFireTimestamp > apartment2LastFireTimestamp
            assert rulesImport.apartment3Engine.lastFireTimestamp == apartment3LastFireTimestamp
        }

        then: "no rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING + 1
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 1
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.masterEngine, 1)
            assertRulesFired(rulesImport.tenantBuildingEngine, 1)
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to set the RULE_STATE to false"
        rulesImport.resetRulesFired()
        attributes = [
            new AssetAttribute("testString", AttributeValueType.STRING, Values.create("test"))
                .setMeta(
                new MetaItem(MetaItemType.RULE_STATE, Values.create(false))
            ),
            new AssetAttribute("testInteger", AttributeValueType.NUMBER, Values.create(0))
        ]
        asset.setAttributes(attributes)
        asset = assetStorageService.merge(asset)

        then: "the facts should be removed from the rule engines and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.tenantBuildingEngine, 1)
            assertRulesFired(rulesImport.tenantBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to set all attributes to RULE_STATE = true"
        rulesImport.resetRulesFired()
        attributes = [
            new AssetAttribute("testString", AttributeValueType.STRING, Values.create("test"))
                .setMeta(
                new MetaItem(MetaItemType.RULE_STATE, Values.create(true))
            ),
            new AssetAttribute("testInteger", AttributeValueType.NUMBER, Values.create(0))
                .setMeta(
                new MetaItem(MetaItemType.RULE_STATE, Values.create(true))
            )
        ]
        asset.setAttributes(attributes)
        asset = assetStorageService.merge(asset)

        then: "the facts should be added to the rule engines and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 2
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 2
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING + 2
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.tenantBuildingEngine, 1)
            assertRulesFired(rulesImport.tenantBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is deleted"
        rulesImport.resetRulesFired()
        assetStorageService.delete([asset.getId()], false)

        then: "the facts should be removed from the rule engines and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.tenantBuildingEngine, 1)
            assertRulesFired(rulesImport.tenantBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }
    }

    def "Stop processing when engine in error state"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakDemoSetup, managerDemoSetup)

        expect: "the rules engines to be ready"
        conditions.eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakDemoSetup, managerDemoSetup)
        }

        when: "a broken RHS rule is loaded into the building engine"
        def ruleset = new TenantRuleset(
            keycloakDemoSetup.tenantBuilding.realm,
            "Some broken test rules",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicBrokenRules.groovy").text)
        rulesetStorageService.merge(ruleset)

        then: "the building engine should not run and the rule engine status should indicate the issue"
        conditions.eventually {
            assert rulesImport.tenantBuildingEngine.deployments.size() == 2
            assert !rulesImport.tenantBuildingEngine.running
            assert rulesImport.tenantBuildingEngine.deployments.values().any({
                it.name == "Some building tenant demo rules" && it.status == READY
            })
            assert rulesImport.tenantBuildingEngine.deployments.values().any({
                it.name == "Some broken test rules" && it.status == COMPILATION_ERROR && it.error instanceof RuntimeException
            })
        }

        when: "an attribute event occurs"
        def globalLastFireTimestamp = rulesImport.globalEngine.lastFireTimestamp
        def masterLastFireTimestamp = rulesImport.masterEngine.lastFireTimestamp
        def tenantALastFireTimestamp = rulesImport.tenantBuildingEngine.lastFireTimestamp
        def apartment2LastFireTimestamp = rulesImport.apartment2Engine.lastFireTimestamp
        def apartment3LastFireTimestamp = rulesImport.apartment3Engine.lastFireTimestamp
        def apartment2LivingRoomPresenceDetectedChange = new AttributeEvent(
            managerDemoSetup.apartment2LivingroomId, "presenceDetected", Values.create(true)
        )
        assetProcessingService.sendAttributeEvent(apartment2LivingRoomPresenceDetectedChange)

        then: "the rules engines should have executed at least one more time"
        conditions.eventually {
            assert rulesImport.globalEngine.lastFireTimestamp > globalLastFireTimestamp
            assert rulesImport.masterEngine.lastFireTimestamp > masterLastFireTimestamp
            assert rulesImport.tenantBuildingEngine.lastFireTimestamp == tenantALastFireTimestamp
            assert rulesImport.apartment2Engine.lastFireTimestamp > apartment2LastFireTimestamp
            assert rulesImport.apartment3Engine.lastFireTimestamp > apartment3LastFireTimestamp
        }
    }
}
