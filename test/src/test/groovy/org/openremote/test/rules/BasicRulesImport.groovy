package org.openremote.test.rules

import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.TenantRuleset
import org.openremote.model.value.Values

import static org.openremote.model.rules.Ruleset.Lang.GROOVY
import static org.openremote.model.rules.RulesetStatus.DEPLOYED

class BasicRulesImport {

    final Long globalRulesetId
    final Long globalRuleset2Id
    final Long masterRulesetId
    final Long tenantBuildingRulesetId
    final Long tenantCityRulesetId
    final Long apartment1RulesetId
    final Long apartment2RulesetId
    final Long apartment3RulesetId

    RulesEngine globalEngine
    RulesEngine masterEngine
    RulesEngine tenantBuildingEngine
    RulesEngine apartment1Engine
    RulesEngine apartment2Engine
    RulesEngine apartment3Engine

    BasicRulesImport(RulesetStorageService rulesetStorageService,
                     KeycloakDemoSetup keycloakDemoSetup,
                     ManagerDemoSetup managerDemoSetup) {

        Ruleset ruleset = new GlobalRuleset(
                "Some global demo rules", GROOVY,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        globalRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new GlobalRuleset(
                "Other global demo rules with a long name that should fill up space in UI", GROOVY,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        ruleset.setEnabled(false)
        globalRuleset2Id = rulesetStorageService.merge(ruleset).id

        ruleset = new TenantRuleset(
            keycloakDemoSetup.masterTenant.realm,
            "Some master tenant demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        masterRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new TenantRuleset(
            keycloakDemoSetup.tenantBuilding.realm,
            "Some building tenant demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        tenantBuildingRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new TenantRuleset(
            keycloakDemoSetup.tenantCity.realm,
            "Some smartcity tenant demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        ruleset.setEnabled(false)
        tenantCityRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
            managerDemoSetup.apartment1Id,
            "Some apartment 1 demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        ruleset.setEnabled(false)
        apartment1RulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
            managerDemoSetup.apartment2Id,
            "Some apartment 2 demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
            .addMeta("visible", Values.create(true))
        apartment2RulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
            managerDemoSetup.apartment3Id,
            "Some apartment 3 demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        apartment3RulesetId = rulesetStorageService.merge(ruleset).id
    }

    boolean assertEnginesReady(RulesService rulesService,
                               KeycloakDemoSetup keycloakDemoSetup,
                               ManagerDemoSetup managerDemoSetup) {

        globalEngine = rulesService.globalEngine
        globalEngine.disableTemporaryFactExpiration = true
        assert globalEngine != null
        assert globalEngine.isRunning()
        assert globalEngine.deployments.size() == 1
        assert globalEngine.deployments.values().any { it -> it.name == "Some global demo rules" && it.status == DEPLOYED }

        assert rulesService.tenantEngines.size() == 2
        masterEngine = rulesService.tenantEngines.get(keycloakDemoSetup.masterTenant.realm)
        masterEngine.disableTemporaryFactExpiration = true
        assert masterEngine != null
        assert masterEngine.isRunning()
        assert masterEngine.deployments.size() == 1
        assert masterEngine.deployments.values().iterator().next().name == "Some master tenant demo rules"
        assert masterEngine.deployments.values().iterator().next().status == DEPLOYED
        tenantBuildingEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantBuilding.realm)
        assert tenantBuildingEngine != null
        tenantBuildingEngine.disableTemporaryFactExpiration = true
        assert tenantBuildingEngine.isRunning()
        assert tenantBuildingEngine.deployments.size() == 1
        assert tenantBuildingEngine.deployments.values().iterator().next().name == "Some building tenant demo rules"
        assert tenantBuildingEngine.deployments.values().iterator().next().status == DEPLOYED
        def tenantCityEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantCity.realm)
        assert tenantCityEngine == null

        assert rulesService.assetEngines.size() == 3
        apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
        assert apartment1Engine == null
        apartment2Engine = rulesService.assetEngines.get(managerDemoSetup.apartment2Id)
        assert apartment2Engine != null
        apartment2Engine.disableTemporaryFactExpiration = true
        assert apartment2Engine.isRunning()
        assert apartment2Engine.deployments.size() == 1
        assert apartment2Engine.deployments.values().iterator().next().name == "Some apartment 2 demo rules"
        assert apartment2Engine.deployments.values().iterator().next().status == DEPLOYED
        apartment3Engine = rulesService.assetEngines.get(managerDemoSetup.apartment3Id)
        assert apartment3Engine != null
        apartment3Engine.disableTemporaryFactExpiration = true
        assert apartment3Engine.isRunning()
        assert apartment3Engine.deployments.size() == 1
        assert apartment3Engine.deployments.values().iterator().next().name == "Some apartment 3 demo rules"
        assert apartment3Engine.deployments.values().iterator().next().status == DEPLOYED

        return true
    }

    void resetRulesFired(RulesEngine... engine) {
        // Remove all named facts (those are the only ones inserted by basic test rules)
        Collection<RulesEngine> engines = [globalEngine, masterEngine, tenantBuildingEngine, apartment2Engine, apartment3Engine]
        engines.addAll(engine)
        engines.forEach({ e ->
            e.facts.namedFacts.keySet().forEach({ factName ->
                e.facts.remove(factName)
            })
        })
    }

    void assertNoRulesFired(RulesEngine... engine) {
        Collection<RulesEngine> engines = [globalEngine, masterEngine, tenantBuildingEngine, apartment2Engine, apartment3Engine]
        engines.addAll(engine)
        engines.forEach({ e ->
            assert e.facts.namedFacts.size() == 0
        })
    }

    static void assertRulesFired(RulesEngine engine, int fired) {
        assert engine.facts.namedFacts.keySet().size() == fired
    }

    static void assertRulesFired(RulesEngine engine, Collection<String> facts) {
        engine.facts.namedFacts.keySet().containsAll(facts)
    }
}
