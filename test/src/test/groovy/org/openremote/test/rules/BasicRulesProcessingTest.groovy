package org.openremote.test.rules

import org.openremote.container.Container
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.ServerAsset
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.rules.geofence.GeofenceAssetAdapter
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.BaseAssetQuery
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeType
import org.openremote.model.attribute.Meta
import org.openremote.model.attribute.MetaItem
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.AssetState
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.TenantRuleset
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.rules.RulesetDeployment.Status.*
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
            assert rulesImport.customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
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
            assertRulesFired(rulesImport.customerAEngine, 1)
            assertRulesFired(rulesImport.customerAEngine, expectedFiredRules)
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
            assert rulesImport.customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
        }

        when: "a LHS filtering test rule definition is loaded into the smart home asset"
        def assetRuleset = new AssetRuleset(
                "Some smart home asset rules",
                managerDemoSetup.smartHomeId,
                getClass().getResource("/org/openremote/test/rules/BasicSmartHomeMatchAllAssetStates.groovy").text,
                Ruleset.Lang.GROOVY
        )
        rulesetStorageService.merge(assetRuleset)
        RulesEngine smartHomeEngine = null

        then: "the smart home rule engine should have ben created, loaded the new rule definition and facts and started"
        conditions.eventually {
            smartHomeEngine = rulesService.assetEngines.get(managerDemoSetup.smartHomeId)
            assert smartHomeEngine != null
            assert smartHomeEngine.isRunning()
            assert smartHomeEngine.deployments.size() == 1
            assert smartHomeEngine.deployments.values().any({
                it.name == "Some smart home asset rules" && it.status == DEPLOYED
            })
        }

        and: "the new rule engine is fully initialised"
        conditions.eventually {
            assert smartHomeEngine.assetStates.size() == DEMO_RULE_STATES_SMART_HOME
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.customerAEngine, 1)
            assertRulesFired(rulesImport.customerAEngine, ["All"])
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
            assertRulesFired(rulesImport.customerAEngine, 1)
            assertRulesFired(rulesImport.customerAEngine, ["All"])
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
        def asset = new ServerAsset("Kitchen", AssetType.ROOM, apartment2)
        asset.setRealmId(keycloakDemoSetup.customerATenant.getId())
        def attributes = [
                new AssetAttribute("testString", AttributeType.STRING, Values.create("test"))
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
            assert rulesImport.customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A + 1
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 1
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.masterEngine, 1)
            assertRulesFired(rulesImport.masterEngine, ["All"])
            assertRulesFired(rulesImport.customerAEngine, 1)
            assertRulesFired(rulesImport.customerAEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to add a new attribute but RULE_STATE = true meta is not changed"
        rulesImport.resetRulesFired()
        attributes = [
                new AssetAttribute("testString", AttributeType.STRING, Values.create("test"))
                        .setMeta(
                        new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
                new AssetAttribute("testInteger", AttributeType.NUMBER, Values.create(0))
        ]
        asset.setAttributes(attributes)
        asset = assetStorageService.merge(asset)

        then: "after a few seconds the fact count shouldn't change and no rules should have fired"
        new PollingConditions(initialDelay: 3).eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL + 1
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A + 1
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 1
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 0)
            assertRulesFired(rulesImport.customerAEngine, 0)
            assertRulesFired(rulesImport.apartment2Engine, 0)
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to set the RULE_STATE to false"
        rulesImport.resetRulesFired()
        attributes = [
                new AssetAttribute("testString", AttributeType.STRING, Values.create("test"))
                        .setMeta(
                        new MetaItem(AssetMeta.RULE_STATE, Values.create(false))
                ),
                new AssetAttribute("testInteger", AttributeType.NUMBER, Values.create(0))
        ]
        asset.setAttributes(attributes)
        asset = assetStorageService.merge(asset)

        then: "the facts should be removed from the rule engines and rules should have fired"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.customerAEngine, 1)
            assertRulesFired(rulesImport.customerAEngine, ["All"])
            assertRulesFired(rulesImport.apartment2Engine, 1)
            assertRulesFired(rulesImport.apartment2Engine, ["All"])
            assertRulesFired(rulesImport.apartment3Engine, 0)
        }

        when: "the Kitchen room asset is modified to set all attributes to RULE_STATE = true"
        rulesImport.resetRulesFired()
        attributes = [
                new AssetAttribute("testString", AttributeType.STRING, Values.create("test"))
                        .setMeta(
                        new MetaItem(AssetMeta.RULE_STATE, Values.create(true))
                ),
                new AssetAttribute("testInteger", AttributeType.NUMBER, Values.create(0))
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
            assert rulesImport.customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A + 2
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2 + 2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.customerAEngine, 1)
            assertRulesFired(rulesImport.customerAEngine, ["All"])
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
            assert rulesImport.customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
            assertRulesFired(rulesImport.globalEngine, 1)
            assertRulesFired(rulesImport.globalEngine, ["All"])
            assertRulesFired(rulesImport.customerAEngine, 1)
            assertRulesFired(rulesImport.customerAEngine, ["All"])
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

        when: "a broken RHS rule is loaded into the customerA engine"
        def ruleset = new TenantRuleset(
                "Some broken test rules",
                keycloakDemoSetup.customerATenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicBrokenRules.groovy").text,
                Ruleset.Lang.GROOVY
        )
        rulesetStorageService.merge(ruleset)

        then: "the customerA engine should not run and the rule engine status should indicate the issue"
        conditions.eventually {
            assert rulesImport.customerAEngine.deployments.size() == 2
            assert !rulesImport.customerAEngine.running
            assert rulesImport.customerAEngine.deployments.values().any({
                it.name == "Some customerA tenant demo rules" && it.status == READY
            })
            assert rulesImport.customerAEngine.deployments.values().any({
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

    def "Check tracking of location predicate rules"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.5, delay: 0.5)

        and: "a mock geofence adapter"
        Map<AssetState, Set<BaseAssetQuery.LocationPredicate>> geofenceChangeMap = null
        def geofenceInit = false;
        def testGeofenceAdapter = new GeofenceAssetAdapter() {

            @Override
            int getPriority() {
                return Integer.MAX_VALUE
            }

            @Override
            void processLocationPredicates(Map<AssetState, Set<BaseAssetQuery.LocationPredicate>> assetLocationPredicateMap, boolean initialising) {
                geofenceChangeMap = assetLocationPredicateMap
                geofenceInit = initialising
            }

            @Override
            void init(Container container) throws Exception {

            }

            @Override
            void start(Container container) throws Exception {

            }

            @Override
            void stop(Container container) throws Exception {

            }
        }

        and: "the container is started"
        def serverPort = findEphemeralPort()
        RulesService.GEOFENCE_PROCESSING_DEBOUNCE_MILLIS = 50
        def container = startContainer(defaultConfig(serverPort), defaultServices(testGeofenceAdapter))
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

        and: "the demo attributes marked with RULE_STATE = true meta should be inserted into the engines"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
        }

        and: "the mock geofence adapter should have been initialised with the demo thing assets location predicate"
        conditions.eventually {
            assert geofenceChangeMap != null
            assert geofenceInit
            assert geofenceChangeMap.size() == 1
            def thingGeofences = geofenceChangeMap.find({ it.key.id == managerDemoSetup.thingId }).value
            assert thingGeofences.size() == 1
            BaseAssetQuery.RadialLocationPredicate locPredicate = thingGeofences.find { it instanceof BaseAssetQuery.RadialLocationPredicate && it.centrePoint[0] == 100 && it.centrePoint[1] == 50 && it.radius == 100 }
            assert locPredicate != null
        }

        when: "another asset's location attribute is marked as RULE_STATE"
        def lobby = assetStorageService.find(managerDemoSetup.lobbyId, true)
        lobby.getAttribute("location").get().addMeta(new MetaItem(AssetMeta.RULE_STATE))
        lobby = assetStorageService.merge(lobby)

        then: "the mock geofence adapter should be notified of the new lobby asset with a location predicate"
        conditions.eventually {
            assert geofenceChangeMap != null
            assert !geofenceInit
            assert geofenceChangeMap.size() == 1
            def lobbyGeofences = geofenceChangeMap.find({ it.key.id == managerDemoSetup.lobbyId }).value
            assert lobbyGeofences.size() == 1
            BaseAssetQuery.RadialLocationPredicate locPredicate = lobbyGeofences.find { it instanceof BaseAssetQuery.RadialLocationPredicate && it.centrePoint[0] == 100 && it.centrePoint[1] == 50 && it.radius == 100 }
            assert locPredicate != null
        }

        when: "a new ruleset is deployed with multiple location predicate rules on the lobby asset"
        def lobbyRuleset = new AssetRuleset(
            "Lobby location predicates",
            managerDemoSetup.lobbyId,
            getClass().getResource("/org/openremote/test/rules/BasicLocationPredicate2.groovy").text,
            Ruleset.Lang.GROOVY
        )
        rulesetStorageService.merge(lobbyRuleset)
        RulesEngine lobbyEngine = null

        then: "the new rule engine should be created and be running"
        conditions.eventually {
            lobbyEngine = rulesService.assetEngines.get(managerDemoSetup.lobbyId)
            assert lobbyEngine != null
            assert lobbyEngine.isRunning()
            assert lobbyEngine.deployments.size() == 1
            assert lobbyEngine.deployments.values().any({
                it.name == "Lobby location predicates" && it.status == DEPLOYED
            })
        }

        then: "the mock geofence adapter should have been notified of the new location predicates for the demo thing and lobby assets"
        conditions.eventually {
            assert geofenceChangeMap != null
            assert !geofenceInit
            assert geofenceChangeMap.size() == 2
            Set<BaseAssetQuery.LocationPredicate> thingGeofences = geofenceChangeMap.find{ it.key.id == managerDemoSetup.thingId }.value
            assert thingGeofences.size() == 3
            BaseAssetQuery.RadialLocationPredicate rad1Predicate = thingGeofences.find { it instanceof BaseAssetQuery.RadialLocationPredicate && it.centrePoint[0] == 100 && it.centrePoint[1] == 50 && it.radius == 100 }
            BaseAssetQuery.RadialLocationPredicate rad2Predicate = thingGeofences.find { it instanceof BaseAssetQuery.RadialLocationPredicate && it.centrePoint[0] == 50 && it.centrePoint[1] == 0 && it.radius == 200 }
            BaseAssetQuery.RectangularLocationPredicate rect1Predicate = thingGeofences.find { it instanceof BaseAssetQuery.RectangularLocationPredicate && it.centrePoint[0] == 75 && it.centrePoint[1] == 25 && it.lngMax == 100 && it.latMax == 50 }
            assert rad1Predicate != null
            assert rad2Predicate != null
            assert rect1Predicate != null
            def lobbyGeofences = geofenceChangeMap.find({ it.key.id == managerDemoSetup.lobbyId }).value
            assert lobbyGeofences.size() == 3
            assert lobbyGeofences.any { it == rad1Predicate }
            assert lobbyGeofences.any { it == rad2Predicate }
            assert lobbyGeofences.any { it == rect1Predicate }
        }

        when: "a location predicate ruleset is removed"
        rulesetStorageService.delete(GlobalRuleset.class, rulesImport.globalRuleset3Id)

        then: "the mock geofence adapter should be notified that the demo thing and lobby asset's rules with location predicates have changed"
        conditions.eventually {
            assert geofenceChangeMap != null
            assert geofenceChangeMap.size() == 2
            Set<BaseAssetQuery.LocationPredicate> thingGeofences = geofenceChangeMap.find{ it.key.id == managerDemoSetup.thingId }.value
            assert thingGeofences.size() == 2
            BaseAssetQuery.RadialLocationPredicate rad2Predicate = thingGeofences.find { it instanceof BaseAssetQuery.RadialLocationPredicate && it.centrePoint[0] == 50 && it.centrePoint[1] == 0 && it.radius == 200 }
            BaseAssetQuery.RectangularLocationPredicate rect1Predicate = thingGeofences.find { it instanceof BaseAssetQuery.RectangularLocationPredicate && it.centrePoint[0] == 75 && it.centrePoint[1] == 25 && it.lngMax == 100 && it.latMax == 50 }
            assert rad2Predicate != null
            assert rect1Predicate != null
            def lobbyGeofences = geofenceChangeMap.find({ it.key.id == managerDemoSetup.lobbyId }).value
            assert lobbyGeofences.size() == 2
            assert lobbyGeofences.any { it == rad2Predicate }
            assert lobbyGeofences.any { it == rect1Predicate }
        }

        when: "the RULE_STATE meta is removed from the lobby asset's location attribute"
        lobby.getAttribute("location").get().getMeta().removeIf({ it.name.orElse(null) == AssetMeta.RULE_STATE.urn })
        lobby = assetStorageService.merge(lobby)

        then: "the mock geofence adapter should be notified that the lobby asset no longer has any rules with location predicates"
        conditions.eventually {
            assert geofenceChangeMap != null
            assert !geofenceInit
            assert geofenceChangeMap.size() == 1
            def lobbyGeofences = geofenceChangeMap.find({ it.key.id == managerDemoSetup.lobbyId }).value
            assert lobbyGeofences.size() == 0
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
