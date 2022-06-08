package org.openremote.test.rules

import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.TemporaryFact
import org.openremote.model.rules.RealmRuleset
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

import static org.openremote.test.setup.ManagerTestSetup.*
import static org.openremote.model.rules.RulesetStatus.*
import static org.openremote.test.rules.BasicRulesImport.assertRulesFired

class BasicRulesProcessingTest extends Specification implements ManagerContainerTrait {

    def "Check firing when deploying a new ruleset"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def expirationMillis = TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = 500
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakTestSetup, managerTestSetup)

        expect: "the rules engines to be ready"
        conditions.eventually {
            assert rulesImport.assertEnginesReady(rulesService, keycloakTestSetup, managerTestSetup)
            assert noRuleEngineFiringScheduled()
        }

        when: "a LHS filtering test rule definition is loaded into the Smart Building asset"
        def assetRuleset = new AssetRuleset(
            managerTestSetup.smartBuildingId,
            "Some Smart Building asset rules",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicSmartHomeMatchAllAssetStates.groovy").text)
        rulesetStorageService.merge(assetRuleset)
        RulesEngine smartHomeEngine = null

        then: "the Smart Building rule engine should have ben created, loaded the new rule definition and facts and started"
        conditions.eventually {
            smartHomeEngine = rulesService.assetEngines.get(managerTestSetup.smartBuildingId)
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
            assertRulesFired(rulesImport.realmBuildingEngine, 1)
            assertRulesFired(rulesImport.realmBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
            assertRulesFired(smartHomeEngine, 7)
            assertRulesFired(smartHomeEngine, ["Living Room All", "Kitchen All", "Kitchen Number Attributes", "Asset Type Room", "Boolean attributes", "String attributes", "Number value types"])
        }

        when: "an attribute event occurs"
        rulesImport.resetRulesFired(smartHomeEngine)
        def apartment2LivingRoomPresenceDetectedChange = new AttributeEvent(
            managerTestSetup.apartment2LivingroomId, "presenceDetected", true
        )
        assetProcessingService.sendAttributeEvent(apartment2LivingRoomPresenceDetectedChange)

        then: "the engines in scope should have fired"
        conditions.eventually {
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.realmBuildingEngine, 1)
            assertRulesFired(rulesImport.realmBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
            assertRulesFired(smartHomeEngine, 7)
            assertRulesFired(smartHomeEngine, ["Living Room All", "Kitchen All", "Kitchen Number Attributes", "Asset Type Room", "Boolean Attributes", "String attributes", "Number value types"])
        }

        cleanup: "the static rules time variable is reset"
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = expirationMillis
    }

    def "Handle attribute event with no meta, asset create, update, delete"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.2)

        and: "the container is started"
        def expirationMillis = TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = 500
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakTestSetup, managerTestSetup)

        expect: "the rules engines to be ready"
        conditions.eventually {
            assert rulesImport.assertEnginesReady(rulesService, keycloakTestSetup, managerTestSetup)
            assert noRuleEngineFiringScheduled()
        }

        when: "a Kitchen room asset is inserted into apartment that contains a RULE_STATE = true meta flag"
        def apartment2 = assetStorageService.find(managerTestSetup.apartment2Id)
        def asset = new RoomAsset("Kitchen")
            .setParent(apartment2)
            .addOrReplaceAttributes(
                new Attribute<>("testString", ValueType.TEXT, "test")
                    .addOrReplaceMeta(
                        new MetaItem<>(MetaItemType.RULE_STATE, true)
                    )
            )
        asset = assetStorageService.merge(asset)

        then: "the rules engines should have executed at least once"
        conditions.eventually {
            assert rulesImport.globalEngine.lastFireTimestamp > 0
            assert rulesImport.masterEngine.lastFireTimestamp > 0
            assert rulesImport.realmBuildingEngine.lastFireTimestamp > 0
            assert rulesImport.apartment2Engine.lastFireTimestamp > 0
            assert rulesImport.apartment3Engine.lastFireTimestamp > 0
        }

        then: "after a few seconds the engines in scope should have facts and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.realmBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING + 1
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 1
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.masterEngine, 1)
            assertRulesFired(rulesImport.masterEngine, ["All"])
            assertRulesFired(rulesImport.realmBuildingEngine, 1)
            assertRulesFired(rulesImport.realmBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        then: "the rule engines settle down"
        conditions.eventually {
            assert noRuleEngineFiringScheduled()
        }

        and: "an attribute event is pushed into the system for an attribute with no RULE_STATE meta"
        def globalLastFireTimestamp = rulesImport.globalEngine.lastFireTimestamp
        def masterLastFireTimestamp = rulesImport.masterEngine.lastFireTimestamp
        def realmALastFireTimestamp = rulesImport.realmBuildingEngine.lastFireTimestamp
        def apartment2LastFireTimestamp = rulesImport.apartment2Engine.lastFireTimestamp
        def apartment3LastFireTimestamp = rulesImport.apartment3Engine.lastFireTimestamp
        def apartment2LivingRoomWindowOpenChange = new AttributeEvent(
            managerTestSetup.apartment2LivingroomId, "windowOpen", true
        )
        assetProcessingService.sendAttributeEvent(apartment2LivingRoomWindowOpenChange)

        then: "the attribute event should have been processed"
        conditions.eventually {
            def apartment2LivingRoom = assetStorageService.find(managerTestSetup.apartment2LivingroomId, true)
            assert apartment2LivingRoom.getAttribute("windowOpen").flatMap{it.getValueAs(Boolean.class)}.orElse(false)
        }

        then: "the rules engines should not have fired"
        conditions.eventually {
            assert rulesImport.globalEngine.lastFireTimestamp == globalLastFireTimestamp
            assert rulesImport.masterEngine.lastFireTimestamp == masterLastFireTimestamp
            assert rulesImport.realmBuildingEngine.lastFireTimestamp == realmALastFireTimestamp
            assert rulesImport.apartment2Engine.lastFireTimestamp == apartment2LastFireTimestamp
            assert rulesImport.apartment3Engine.lastFireTimestamp == apartment3LastFireTimestamp
        }

        when: "time advances"
        advancePseudoClock(1, TimeUnit.SECONDS, container)

        and: "the Kitchen room asset is modified to add a new attribute but RULE_STATE = true meta is not changed"
        globalLastFireTimestamp = rulesImport.globalEngine.lastFireTimestamp
        masterLastFireTimestamp = rulesImport.masterEngine.lastFireTimestamp
        realmALastFireTimestamp = rulesImport.realmBuildingEngine.lastFireTimestamp
        apartment2LastFireTimestamp = rulesImport.apartment2Engine.lastFireTimestamp
        apartment3LastFireTimestamp = rulesImport.apartment3Engine.lastFireTimestamp
        asset.addOrReplaceAttributes(
            new Attribute<>("testString", ValueType.TEXT, "test")
                .addOrReplaceMeta(
                    new MetaItem<>(MetaItemType.RULE_STATE, true)
                ),
            new Attribute<>("testInteger", ValueType.NUMBER, 0d)
        )
        asset = assetStorageService.merge(asset)

        then: "the rules engines should have executed at least one more time"
        conditions.eventually {
            assert rulesImport.globalEngine.lastFireTimestamp > globalLastFireTimestamp
            assert rulesImport.masterEngine.lastFireTimestamp == masterLastFireTimestamp
            assert rulesImport.realmBuildingEngine.lastFireTimestamp > realmALastFireTimestamp
            assert rulesImport.apartment2Engine.lastFireTimestamp > apartment2LastFireTimestamp
            assert rulesImport.apartment3Engine.lastFireTimestamp == apartment3LastFireTimestamp
        }

        then: "no rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.realmBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING + 1
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 1
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.masterEngine, 1)
            assertRulesFired(rulesImport.realmBuildingEngine, 1)
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to set the RULE_STATE to false"
        rulesImport.resetRulesFired()
        asset.addOrReplaceAttributes(
            new Attribute<>("testString", ValueType.TEXT, "test")
                .addOrReplaceMeta(
                    new MetaItem<>(MetaItemType.RULE_STATE, false)
                ),
            new Attribute<>("testInteger", ValueType.NUMBER, 0d)
        )
        asset = assetStorageService.merge(asset)

        then: "the facts should be removed from the rule engines and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.realmBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.realmBuildingEngine, 1)
            assertRulesFired(rulesImport.realmBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to set all attributes to RULE_STATE = true"
        rulesImport.resetRulesFired()
        asset.addOrReplaceAttributes(
            new Attribute<>("testString", ValueType.TEXT, "test")
                .addOrReplaceMeta(
                    new MetaItem<>(MetaItemType.RULE_STATE, true)
                ),
            new Attribute<>("testInteger", ValueType.NUMBER, 0d)
                .addOrReplaceMeta(
                    new MetaItem<>(MetaItemType.RULE_STATE, true)
                )
        )
        asset = assetStorageService.merge(asset)

        then: "the facts should be added to the rule engines and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 2
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 2
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.realmBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING + 2
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.realmBuildingEngine, 1)
            assertRulesFired(rulesImport.realmBuildingEngine, ["All"])
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
            assert rulesImport.realmBuildingEngine.assetStates.size() == DEMO_RULE_STATES_SMART_BUILDING
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.realmBuildingEngine, 1)
            assertRulesFired(rulesImport.realmBuildingEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        cleanup: "the static rules time variable is reset"
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = expirationMillis
    }

    def "Stop processing when engine in error state"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def expirationMillis = TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = 500
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakTestSetup, managerTestSetup)

        expect: "the rules engines to be ready"
        conditions.eventually {
            assert rulesImport.assertEnginesReady(rulesService, keycloakTestSetup, managerTestSetup)
            assert noRuleEngineFiringScheduled()
        }

        when: "a broken RHS rule is loaded into the building engine"
        def ruleset = new RealmRuleset(
            keycloakTestSetup.realmBuilding.name,
            "Some broken test rules",
            Ruleset.Lang.GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicBrokenRules.groovy").text)
        rulesetStorageService.merge(ruleset)

        then: "the building engine should not run and the rule engine status should indicate the issue"
        conditions.eventually {
            assert rulesImport.realmBuildingEngine.deployments.size() == 2
            assert !rulesImport.realmBuildingEngine.running
            assert rulesImport.realmBuildingEngine.deployments.values().any({
                it.name == "Some building realm demo rules" && it.status == READY
            })
            assert rulesImport.realmBuildingEngine.deployments.values().any({
                it.name == "Some broken test rules" && it.status == COMPILATION_ERROR && it.error instanceof RuntimeException
            })
        }

        then: "the engines should settle"
        conditions.eventually {
            assert noRuleEngineFiringScheduled()
        }

        when: "an attribute event occurs"
        def globalLastFireTimestamp = rulesImport.globalEngine.lastFireTimestamp
        def masterLastFireTimestamp = rulesImport.masterEngine.lastFireTimestamp
        def realmALastFireTimestamp = rulesImport.realmBuildingEngine.lastFireTimestamp
        def apartment2LastFireTimestamp = rulesImport.apartment2Engine.lastFireTimestamp
        def apartment3LastFireTimestamp = rulesImport.apartment3Engine.lastFireTimestamp
        def apartment2LivingRoomPresenceDetectedChange = new AttributeEvent(
            managerTestSetup.apartment2LivingroomId, "presenceDetected", true
        )
        assetProcessingService.sendAttributeEvent(apartment2LivingRoomPresenceDetectedChange)

        then: "the rules engines should have executed at least one more time"
        conditions.eventually {
            assert rulesImport.globalEngine.lastFireTimestamp > globalLastFireTimestamp
            assert rulesImport.masterEngine.lastFireTimestamp == masterLastFireTimestamp
            assert rulesImport.realmBuildingEngine.lastFireTimestamp == realmALastFireTimestamp
            assert rulesImport.apartment2Engine.lastFireTimestamp > apartment2LastFireTimestamp
            assert rulesImport.apartment3Engine.lastFireTimestamp == apartment3LastFireTimestamp
        }

        cleanup: "the static rules time variable is reset"
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = expirationMillis
    }
}
