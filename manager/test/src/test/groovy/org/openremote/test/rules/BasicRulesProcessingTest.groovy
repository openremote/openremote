package org.openremote.test.rules

import elemental.json.Json
import org.kie.api.event.rule.AfterMatchFiredEvent
import org.kie.api.event.rule.DefaultAgendaEventListener
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.manager.server.rules.RulesDeployment
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.rules.AssetRulesDefinition
import org.openremote.manager.shared.rules.GlobalRulesDefinition
import org.openremote.manager.shared.rules.RulesDefinition.DeploymentStatus
import org.openremote.model.*
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetAttributes
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class BasicRulesProcessingTest extends Specification implements ManagerContainerTrait {

    RulesDeployment globalEngine, masterEngine, customerAEngine, smartHomeEngine, apartment1Engine, apartment3Engine

    List<String> globalEngineFiredRules = []
    List<String> masterEngineFiredRules = []
    List<String> customerAEngineFiredRules = []
    List<String> smartHomeEngineFiredRules = []
    List<String> apartment1EngineFiredRules = []
    List<String> apartment3EngineFiredRules = []

    def resetRuleExecutionLoggers() {
        globalEngineFiredRules.clear()
        customerAEngineFiredRules.clear()
        smartHomeEngineFiredRules.clear()
        apartment1EngineFiredRules.clear()
        apartment3EngineFiredRules.clear()
    }

    def assertNoRulesFired = {
        assert globalEngineFiredRules.size() == 0
        assert masterEngineFiredRules.size() == 0
        assert customerAEngineFiredRules.size() == 0
        assert smartHomeEngineFiredRules.size() == 0
        assert apartment1EngineFiredRules.size() == 0
        assert apartment3EngineFiredRules.size() == 0
    }

    def attachRuleExecutionLogger(RulesDeployment ruleEngine, List<String> executedRules) {
        def session = ruleEngine.getKnowledgeSession()
        if (session == null) {
            return
        }
        session.addEventListener(new DefaultAgendaEventListener() {
            @Override
            void afterMatchFired(AfterMatchFiredEvent event) {
                def rule = event.getMatch().getRule()
                def ruleName = rule.getName()
                executedRules.add(ruleName)
            }
        })
    }

    def "Check firing of rules"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesStorageService = container.getService(RulesStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)

        and: "some test rulesets have been imported"
        new BasicRulesImport(rulesStorageService, keycloakDemoSetup, managerDemoSetup)

        expect: "the rule engines to become available and be running"
        conditions.eventually {
            globalEngine = rulesService.globalDeployment
            assert globalEngine != null
            assert globalEngine.isRunning()
            masterEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.masterTenant.id)
            assert masterEngine != null
            assert masterEngine.isRunning()
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            smartHomeEngine = rulesService.assetDeployments.get(managerDemoSetup.smartHomeId)
            assert smartHomeEngine == null
            apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
            assert apartment3Engine != null
            assert apartment3Engine.isRunning()
        }

        and: "the demo attributes marked with RULES_FACT = true meta should be inserted into the engines"
        conditions.eventually {
            globalEngine.facts.size() == 6
        }

        when: "rule execution loggers are attached to the engines"
        attachRuleExecutionLogger(globalEngine, globalEngineFiredRules)
        attachRuleExecutionLogger(masterEngine, masterEngineFiredRules)
        attachRuleExecutionLogger(customerAEngine, customerAEngineFiredRules)
        attachRuleExecutionLogger(apartment1Engine, apartment1EngineFiredRules)
        attachRuleExecutionLogger(apartment3Engine, apartment3EngineFiredRules)

        and: "an attribute event is pushed into the system for an attribute with RULES_FACT meta set to true"
        def apartment1LivingRoomDemoBooleanChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomId, "demoBoolean"), Json.create(false)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoBooleanChange)

        then: "the rule engines in scope should fire the 'All' and 'All changed' rules"
        conditions.eventually {
            def expectedFiredRules = ["All", "All changed"]
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(expectedFiredRules)
            assert masterEngineFiredRules.size() == 0
            assert customerAEngineFiredRules.size() == 2
            assert customerAEngineFiredRules.containsAll(expectedFiredRules)
            assert smartHomeEngineFiredRules.size() == 0
            assert apartment1EngineFiredRules.size() == 2
            assert apartment1EngineFiredRules.containsAll(expectedFiredRules)
            assert apartment3EngineFiredRules.size() == 0
        }

        when: "an attribute event is pushed into the system for an attribute with RULES_FACT meta set to false"
        resetRuleExecutionLoggers()
        def apartment1LivingRoomDemoStringChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomId, "demoString"), Json.create("demo2")), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoStringChange)

        then: "no rule engines should have fired after a few seconds"
        new PollingConditions(initialDelay: 3).eventually assertNoRulesFired

        when: "an attribute event is pushed into the system for an attribute with no RULES_FACT meta"
        resetRuleExecutionLoggers()
        def apartment1LivingRoomDemoIntegerChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomId, "demoInteger"), Json.create(1)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoIntegerChange)

        then: "no rule engines should have fired after a few seconds"
        new PollingConditions(initialDelay: 3).eventually assertNoRulesFired

        when: "an old (stale) attribute event is pushed into the system"
        resetRuleExecutionLoggers()
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoBooleanChange)

        then: "no rule engines should have fired after a few seconds"
        new PollingConditions(initialDelay: 3).eventually assertNoRulesFired

        when: "an attribute event with the same value as current value is pushed into the system"
        resetRuleExecutionLoggers()
        apartment1LivingRoomDemoBooleanChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomId, "demoBoolean"), Json.create(false)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoBooleanChange)

        then: "the rule engines in scope should fire the 'All' rule but not the 'All changed' rule"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 1
            assert globalEngineFiredRules[0] == "All"
            assert masterEngineFiredRules.size() == 0
            assert customerAEngineFiredRules.size() == 1
            assert customerAEngineFiredRules[0] == "All"
            assert smartHomeEngineFiredRules.size() == 0
            assert apartment1EngineFiredRules.size() == 1
            assert apartment1EngineFiredRules[0] == "All"
            assert apartment3EngineFiredRules.size() == 0
        }

        when: "a LHS filtering test rule definition is loaded into the smart home asset"
        resetRuleExecutionLoggers()
        def rulesDefinition = new AssetRulesDefinition(
                "Some smart home asset rules",
                managerDemoSetup.smartHomeId,
                getClass().getResource("/org/openremote/test/rules/SmartHomeMatchAllAssetUpdates.drl").text
        )
        rulesStorageService.merge(rulesDefinition)

        then: "the smart home rule engine should have ben created, loaded the new rule definition and started"
        conditions.eventually {
            smartHomeEngine = rulesService.assetDeployments.get(managerDemoSetup.smartHomeId)
            assert smartHomeEngine != null
            assert smartHomeEngine.isRunning()
            assert smartHomeEngine.allRulesDefinitions.length == 1
            assert smartHomeEngine.allRulesDefinitions[0].enabled
            assert smartHomeEngine.allRulesDefinitions[0].name == "Some smart home asset rules"
            assert smartHomeEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the engine counters are reset and the smart home engine logger is attached"
        resetRuleExecutionLoggers()
        attachRuleExecutionLogger(smartHomeEngine, smartHomeEngineFiredRules)

        and: "an apartment 3 living room attribute event occurs"
        def apartment3LivingRoomDemoStringChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment3LivingroomId, "demoString"), Json.create("demo2")), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment3LivingRoomDemoStringChange)

        then: "the engines in scope should have fired the matched rules"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(["All", "All changed"])
            assert customerAEngineFiredRules.size() == 2
            assert customerAEngineFiredRules.containsAll(["All", "All changed"])
            assert smartHomeEngineFiredRules.size() == 5
            assert smartHomeEngineFiredRules.containsAll(["Living Room All", "Current Asset Update", "Parent Type Residence", "Asset Type Room", "String Attributes"])
            assert apartment3EngineFiredRules.size() == 2
            assert apartment3EngineFiredRules.containsAll(["All", "All changed"])
            assert apartment1EngineFiredRules.size() == 0
        }

        when: "an apartment 1 living room thermostat attribute event occurs"
        resetRuleExecutionLoggers()
        def apartment1LivingRoomTargetTempChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomThermostatId, "targetTemperature"), Json.create(22.5)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomTargetTempChange)

        then: "the engines in scope should have fired the matched rules"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(["All", "All changed"])
            assert customerAEngineFiredRules.size() == 2
            assert customerAEngineFiredRules.containsAll(["All", "All changed"])
            assert smartHomeEngineFiredRules.size() == 5
            assert smartHomeEngineFiredRules.containsAll(
                    [
                            "Living Room Thermostat",
                            "Living Room Target Temp",
                            "Living Room as Parent",
                            "JSON Number value types",
                            "Current Asset Update"
                    ])
            assert apartment1EngineFiredRules.size() == 2
            assert apartment1EngineFiredRules.containsAll(["All", "All changed"])
            assert apartment3EngineFiredRules.size() == 0
        }

        when: "a RHS filtering test rule definition is loaded into the global rule engine"
        rulesDefinition = new GlobalRulesDefinition(
                "Some global test rules",
                getClass().getResource("/org/openremote/test/rules/SmartHomePreventAssetUpdate.drl").text
        )
        rulesStorageService.merge(rulesDefinition)

        then: "the global rule engine should have loaded the new rule definition and restarted"
        conditions.eventually {
            globalEngine = rulesService.globalDeployment
            assert globalEngine != null
            assert globalEngine.isRunning()
            assert globalEngine.allRulesDefinitions.length == 2
            assert globalEngine.allRulesDefinitions[1].enabled
            assert globalEngine.allRulesDefinitions[1].name == "Some global test rules"
            assert globalEngine.allRulesDefinitions[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the engine counters are reset and the global engine logger is reattached"
        resetRuleExecutionLoggers()
        attachRuleExecutionLogger(globalEngine, globalEngineFiredRules)

        and: "an apartment 1 living room thermostat attribute event occurs"
        apartment1LivingRoomTargetTempChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomThermostatId, "targetTemperature"), Json.create(22.5)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomTargetTempChange)

        then: "after a few seconds only the global engine should have fired the All, All changed and Prevent Livingroom Thermostat Change rules"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 3
            assert globalEngineFiredRules.containsAll(["All", "All changed", "Prevent Livingroom Thermostat Change"])
            assert customerAEngineFiredRules.size() == 0
            assert smartHomeEngineFiredRules.size() == 0
            assert apartment1EngineFiredRules.size() == 0
            assert apartment3EngineFiredRules.size() == 0
        }

        when: "an apartment 3 living room attribute event occurs"
        resetRuleExecutionLoggers()
        apartment3LivingRoomDemoStringChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment3LivingroomId, "demoString"), Json.create("demo3")), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment3LivingRoomDemoStringChange)

        then: "all the engines in scope should have fired the matched rules"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(["All", "All changed"])
            assert customerAEngineFiredRules.size() == 2
            assert customerAEngineFiredRules.containsAll(["All", "All changed"])
            assert smartHomeEngineFiredRules.size() == 5
            assert smartHomeEngineFiredRules.containsAll(["Living Room All", "Current Asset Update", "Parent Type Residence", "Asset Type Room", "String Attributes"])
            assert apartment3EngineFiredRules.size() == 2
            assert apartment3EngineFiredRules.containsAll(["All", "All changed"])
            assert apartment1EngineFiredRules.size() == 0
        }

        when: "a Kitchen room asset is inserted into apartment 1 that contains a RULES_FACT = true meta flag"
        resetRuleExecutionLoggers()
        def apartment1 = assetStorageService.find(managerDemoSetup.apartment1Id)
        def asset = new ServerAsset(apartment1)
        asset.setRealmId(keycloakDemoSetup.customerATenant.getId())
        asset.setType(AssetType.ROOM)
        asset.setName("Kitchen")
        AssetAttributes attributes = new AssetAttributes()
        attributes.put(
                new AssetAttribute("testString", AttributeType.STRING, Json.create("test"))
                        .setMeta(
                        new Meta()
                                .add(new MetaItem(AssetMeta.RULES_FACT, Json.create(true)))
                )
        )
        asset.setAttributes(attributes.getJsonObject())
        asset = assetStorageService.merge(asset)

        then: "the engines in scope should have fired the matched rules (Kitchen All should fire)"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(["All", "All changed"])
            assert customerAEngineFiredRules.size() == 2
            assert customerAEngineFiredRules.containsAll(["All", "All changed"])
            assert smartHomeEngineFiredRules.size() == 4
            assert smartHomeEngineFiredRules.containsAll(["Kitchen All", "Parent Type Residence", "Asset Type Room", "String Attributes"])
            assert apartment1EngineFiredRules.size() == 2
            assert apartment1EngineFiredRules.containsAll(["All", "All changed"])
            assert apartment3EngineFiredRules.size() == 0
        }

        when: "the Kitchen room asset is modified to add a new attribute but RULES_FACT = true meta is not changed"
        resetRuleExecutionLoggers()
        attributes = new AssetAttributes()
        attributes.put(
                new AssetAttribute("testString", AttributeType.STRING, Json.create("test"))
                        .setMeta(
                        new Meta()
                                .add(new MetaItem(AssetMeta.RULES_FACT, Json.create(true)))
                ),
                new AssetAttribute("testInteger", AttributeType.INTEGER, Json.create(0))
        )
        asset.setAttributes(attributes.getJsonObject())
        def factCount = smartHomeEngine.facts.size()
        asset = assetStorageService.merge(asset)

        then: "after a few seconds the fact count shouldn't change"
        new PollingConditions(initialDelay: 3).eventually {
            assert smartHomeEngine.facts.size() == factCount
        }

        when: "the Kitchen room asset is modified to set the RULES_FACT to false"
        attributes = new AssetAttributes()
        attributes.put(
                new AssetAttribute("testString", AttributeType.STRING, Json.create("test"))
                        .setMeta(
                        new Meta()
                                .add(new MetaItem(AssetMeta.RULES_FACT, Json.create(false)))
                ),
                new AssetAttribute("testInteger", AttributeType.INTEGER, Json.create(0))
        )
        asset.setAttributes(attributes.getJsonObject())
        factCount = smartHomeEngine.facts.size()
        asset = assetStorageService.merge(asset)

        then: "the facts should be removed from the rule engines"
        conditions.eventually {
            assert smartHomeEngine.facts.size() == factCount - 1
        }

        when: "the Kitchen room asset is modified to set all attributes to RULES_FACT = true"
        resetRuleExecutionLoggers()
        attributes = new AssetAttributes()
        attributes.put(
                new AssetAttribute("testString", AttributeType.STRING, Json.create("test"))
                        .setMeta(
                        new Meta()
                                .add(new MetaItem(AssetMeta.RULES_FACT, Json.create(true)))
                ),
                new AssetAttribute("testInteger", AttributeType.INTEGER, Json.create(0))
                        .setMeta(
                        new Meta()
                                .add(new MetaItem(AssetMeta.RULES_FACT, Json.create(true)))
                )
        )
        asset.setAttributes(attributes.getJsonObject())
        asset = assetStorageService.merge(asset)

        then: "the engines in scope should have fired the matched rules twice (Kitchen All should fire twice Kitchen Integer should have fired once)"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 4
            assert globalEngineFiredRules.containsAll(["All", "All changed"])
            assert customerAEngineFiredRules.size() == 4
            assert customerAEngineFiredRules.containsAll(["All", "All changed"])
            assert smartHomeEngineFiredRules.size() == 9
            assert smartHomeEngineFiredRules.containsAll(["Kitchen All", "Parent Type Residence", "Asset Type Room", "String Attributes", "Kitchen Integer Attributes"])
            assert apartment1EngineFiredRules.size() == 4
            assert apartment1EngineFiredRules.containsAll(["All", "All changed"])
            assert apartment3EngineFiredRules.size() == 0
        }

        when: "the Kitchen room asset is deleted"
        resetRuleExecutionLoggers()
        factCount = smartHomeEngine.facts.size()
        assetStorageService.delete(asset.getId())

        then: "the facts should be removed from the rule engines"
        conditions.eventually {
            assert smartHomeEngine.facts.size() == factCount - 2
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
