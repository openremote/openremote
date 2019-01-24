package org.openremote.test.rules

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
import org.openremote.model.asset.AssetMeta
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

import static org.openremote.model.rules.RulesetStatus.*
import static org.openremote.manager.setup.builtin.ManagerDemoSetup.*
import static org.openremote.test.rules.BasicRulesImport.assertRulesFired

class BasicRulesProcessingTest extends Specification implements ManagerContainerTrait {

    def "Check scoped firing of rules"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.5, delay: 0.5)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
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
            assert rulesImport.tenantAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
        }

        when: "an attribute event is pushed into the system for an attribute with RULE_STATE meta set to true"
        rulesImport.resetRulesFired()
        def apartment2LivingRoomPresenceDetectedChange = new AttributeEvent(
            managerDemoSetup.apartment2LivingroomId, "presenceDetected", Values.create(true)
        )
        assetProcessingService.sendAttributeEvent(apartment2LivingRoomPresenceDetectedChange)

        then: "the rule engines in scope should fire the 'All' rule"
        conditions.eventually {
            def expectedFiredRules = ["All"]
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, expectedFiredRules)
            assertRulesFired(rulesImport.tenantAEngine, 1)
            assertRulesFired(rulesImport.tenantAEngine, expectedFiredRules)
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, expectedFiredRules)
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "an attribute event is pushed into the system for an attribute with no RULE_STATE meta"
        rulesImport.resetRulesFired()
        def apartment2LivingRoomWindowOpenChange = new AttributeEvent(
            managerDemoSetup.apartment2LivingroomId, "windowOpen", Values.create(true)
        )
        assetProcessingService.sendAttributeEvent(apartment2LivingRoomWindowOpenChange)

        then: "no rule engines should have fired after a few seconds"
        new PollingConditions(initialDelay: 3).eventually {
            rulesImport.assertNoRulesFired()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Check firing when deploying a new ruleset"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.5, delay: 0.5)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
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
            assert rulesImport.tenantAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
        }

        when: "a LHS filtering test rule definition is loaded into the Smart Building asset"
        def assetRuleset = new AssetRuleset(
            "Some Smart Building asset rules",
            managerDemoSetup.smartHomeId,
            getClass().getResource("/org/openremote/test/rules/BasicSmartHomeMatchAllAssetStates.groovy").text,
            Ruleset.Lang.GROOVY
        )
        rulesetStorageService.merge(assetRuleset)
        RulesEngine smartHomeEngine = null

        then: "the Smart Building rule engine should have ben created, loaded the new rule definition and facts and started"
        conditions.eventually {
            smartHomeEngine = rulesService.assetEngines.get(managerDemoSetup.smartHomeId)
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
            assertRulesFired(rulesImport.tenantAEngine, 1)
            assertRulesFired(rulesImport.tenantAEngine, ["All"])
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
            assertRulesFired(rulesImport.tenantAEngine, 1)
            assertRulesFired(rulesImport.tenantAEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
            assertRulesFired(smartHomeEngine, 8)
            assertRulesFired(smartHomeEngine, ["Living Room All", "Kitchen All", "Kitchen Number Attributes", "Parent Type Residence", "Asset Type Room", "Boolean Attributes", "String attributes", "Number value types"])
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Handle asset create, update, delete"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.5, delay: 0.5)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetStorageService = container.getService(AssetStorageService.class)

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
        asset.setRealmId(keycloakDemoSetup.tenantA.getId())
        def attributes = [
            new AssetAttribute("testString", AttributeValueType.STRING, Values.create("test"))
                .setMeta(
                new Meta(new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                )
            )
        ]
        asset.setAttributes(attributes)
        asset = assetStorageService.merge(asset)

        then: "after a few seconds the engines in scope should have facts and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A + 1
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 1
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.masterEngine, 1)
            assertRulesFired(rulesImport.masterEngine, ["All"])
            assertRulesFired(rulesImport.tenantAEngine, 1)
            assertRulesFired(rulesImport.tenantAEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to add a new attribute but RULE_STATE = true meta is not changed"
        rulesImport.resetRulesFired()
        attributes = [
            new AssetAttribute("testString", AttributeValueType.STRING, Values.create("test"))
                .setMeta(
                new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
            ),
            new AssetAttribute("testInteger", AttributeValueType.NUMBER, Values.create(0))
        ]
        asset.setAttributes(attributes)
        asset = assetStorageService.merge(asset)

        then: "after a few seconds the fact count shouldn't change and no rules should have fired"
        new PollingConditions(initialDelay: 3).eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A + 1
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 1
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 0)
            assertRulesFired(rulesImport.tenantAEngine, 0)
            assertRulesFired(rulesImport.apartment2Engine, 0)
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to set the RULE_STATE to false"
        rulesImport.resetRulesFired()
        attributes = [
            new AssetAttribute("testString", AttributeValueType.STRING, Values.create("test"))
                .setMeta(
                new MetaItem(AssetMeta.RULE_STATE, Values.create(false))
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
            assert rulesImport.tenantAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.tenantAEngine, 1)
            assertRulesFired(rulesImport.tenantAEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to set all attributes to RULE_STATE = true"
        rulesImport.resetRulesFired()
        attributes = [
            new AssetAttribute("testString", AttributeValueType.STRING, Values.create("test"))
                .setMeta(
                new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
            ),
            new AssetAttribute("testInteger", AttributeValueType.NUMBER, Values.create(0))
                .setMeta(
                new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
            )
        ]
        asset.setAttributes(attributes)
        asset = assetStorageService.merge(asset)

        then: "the facts should be added to the rule engines and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 2
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 2
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A + 2
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.tenantAEngine, 1)
            assertRulesFired(rulesImport.tenantAEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is deleted"
        rulesImport.resetRulesFired()
        assetStorageService.delete(asset.getId())

        then: "the facts should be removed from the rule engines and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.tenantAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.tenantAEngine, 1)
            assertRulesFired(rulesImport.tenantAEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Stop processing when engine in error state"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.5)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
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

        when: "a broken RHS rule is loaded into the tenantA engine"
        def ruleset = new TenantRuleset(
            "Some broken test rules",
            keycloakDemoSetup.tenantA.id,
            getClass().getResource("/org/openremote/test/rules/BasicBrokenRules.groovy").text,
            Ruleset.Lang.GROOVY
        )
        rulesetStorageService.merge(ruleset)

        then: "the tenantA engine should not run and the rule engine status should indicate the issue"
        conditions.eventually {
            assert rulesImport.tenantAEngine.deployments.size() == 2
            assert !rulesImport.tenantAEngine.running
            assert rulesImport.tenantAEngine.deployments.values().any({
                it.name == "Some tenantA tenant demo rules" && it.status == READY
            })
            assert rulesImport.tenantAEngine.deployments.values().any({
                it.name == "Some broken test rules" && it.status == COMPILATION_ERROR && it.error instanceof RuntimeException
            })
        }

        when: "an attribute event occurs"
        def apartment2LivingRoomPresenceDetectedChange = new AttributeEvent(
            managerDemoSetup.apartment2LivingroomId, "presenceDetected", Values.create(true)
        )
        assetProcessingService.sendAttributeEvent(apartment2LivingRoomPresenceDetectedChange)

        then: "the broken rules engine should prevent a database update"
        new PollingConditions(initialDelay: 3).eventually {
            def asset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert asset.getAttribute("presenceDetected").get().getValueAsBoolean().isPresent()
            assert !asset.getAttribute("presenceDetected").get().getValueAsBoolean().get()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    //TODO add test for location Predicates in AssetQueryPredicate
}
