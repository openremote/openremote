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

import static org.openremote.model.rules.RulesetStatus.DEPLOYED
import static org.openremote.model.rules.Ruleset.Lang.GROOVY

class BasicRulesImport {

    final Long globalRulesetId
    final Long globalRuleset2Id
    final Long masterRulesetId
    final Long tenantARulesetId
    final Long tenantBRulesetId
    final Long apartment1RulesetId
    final Long apartment2RulesetId
    final Long apartment3RulesetId

    RulesEngine globalEngine
    RulesEngine masterEngine
    RulesEngine tenantAEngine
    RulesEngine apartment1Engine
    RulesEngine apartment2Engine
    RulesEngine apartment3Engine

    BasicRulesImport(RulesetStorageService rulesetStorageService,
                     KeycloakDemoSetup keycloakDemoSetup,
                     ManagerDemoSetup managerDemoSetup) {

        Ruleset ruleset = new GlobalRuleset(
                "Some global demo rules",
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text,
                GROOVY
        )
        globalRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new GlobalRuleset(
                "Other global demo rules with a long name that should fill up space in UI",
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text,
                GROOVY
        )
        ruleset.setEnabled(false)
        globalRuleset2Id = rulesetStorageService.merge(ruleset).id

        ruleset = new TenantRuleset(
                "Some master tenant demo rules",
                keycloakDemoSetup.masterTenant.realm,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text,
                GROOVY, false
        )
        masterRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new TenantRuleset(
                "Some tenantA tenant demo rules",
                keycloakDemoSetup.tenantA.realm,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text,
                GROOVY, false
        )
        tenantARulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new TenantRuleset(
                "Some tenantB tenant demo rules",
                keycloakDemoSetup.tenantB.realm,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text,
                GROOVY, false
        )
        ruleset.setEnabled(false)
        tenantBRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
                "Some apartment 1 demo rules",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text,
                GROOVY, false
        )
        ruleset.setEnabled(false)
        apartment1RulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
                "Some apartment 2 demo rules",
                managerDemoSetup.apartment2Id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text,
                GROOVY, false
        )
        apartment2RulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
                "Some apartment 3 demo rules",
                managerDemoSetup.apartment3Id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text,
                GROOVY, false
        )
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
        tenantAEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantA.realm)
        assert tenantAEngine != null
        tenantAEngine.disableTemporaryFactExpiration = true
        assert tenantAEngine.isRunning()
        assert tenantAEngine.deployments.size() == 1
        assert tenantAEngine.deployments.values().iterator().next().name == "Some tenantA tenant demo rules"
        assert tenantAEngine.deployments.values().iterator().next().status == DEPLOYED
        def tenantBEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantB.realm)
        assert tenantBEngine == null

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
        Collection<RulesEngine> engines = [globalEngine, masterEngine, tenantAEngine, apartment2Engine, apartment3Engine]
        engines.addAll(engine)
        engines.forEach({ e ->
            e.facts.namedFacts.keySet().forEach({ factName ->
                e.facts.remove(factName)
            })
        })
    }

    void assertNoRulesFired(RulesEngine... engine) {
        Collection<RulesEngine> engines = [globalEngine, masterEngine, tenantAEngine, apartment2Engine, apartment3Engine]
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
