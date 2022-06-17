package org.openremote.test.rules

import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.RealmRuleset

import static org.openremote.model.rules.Ruleset.Lang.GROOVY
import static org.openremote.model.rules.Ruleset.SHOW_ON_LIST
import static org.openremote.model.rules.RulesetStatus.DEPLOYED

class BasicRulesImport {

    final Long globalRulesetId
    final Long globalRuleset2Id
    final Long masterRulesetId
    final Long realmBuildingRulesetId
    final Long realmCityRulesetId
    final Long apartment1RulesetId
    final Long apartment2RulesetId
    final Long apartment3RulesetId

    RulesEngine globalEngine
    RulesEngine masterEngine
    RulesEngine realmBuildingEngine
    RulesEngine apartment1Engine
    RulesEngine apartment2Engine
    RulesEngine apartment3Engine

    BasicRulesImport(RulesetStorageService rulesetStorageService,
                     KeycloakTestSetup keycloakTestSetup,
                     ManagerTestSetup managerTestSetup) {

        Ruleset ruleset = new GlobalRuleset(
                "Some global demo rules", GROOVY,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        globalRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new GlobalRuleset(
                "Other global demo rules with a long name that should fill up space in UI", GROOVY,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        ruleset.setEnabled(false)
        globalRuleset2Id = rulesetStorageService.merge(ruleset).id

        ruleset = new RealmRuleset(
            keycloakTestSetup.realmMaster.name,
            "Some master realm demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        masterRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new RealmRuleset(
            keycloakTestSetup.realmBuilding.name,
            "Some building realm demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        realmBuildingRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new RealmRuleset(
            keycloakTestSetup.realmCity.name,
            "Some smartcity realm demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        ruleset.setEnabled(false)
        realmCityRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
            managerTestSetup.apartment1Id,
            "Some apartment 1 demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        ruleset.setEnabled(false)
        apartment1RulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
            managerTestSetup.apartment2Id,
            "Some apartment 2 demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        .setMeta(new MetaMap(Collections.singletonList(new MetaItem<>(SHOW_ON_LIST))))
        apartment2RulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
            managerTestSetup.apartment3Id,
            "Some apartment 3 demo rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.groovy").text)
        apartment3RulesetId = rulesetStorageService.merge(ruleset).id
    }

    @SuppressWarnings("GroovyAccessibility")
    boolean assertEnginesReady(RulesService rulesService,
                               KeycloakTestSetup keycloakTestSetup,
                               ManagerTestSetup managerTestSetup) {

        globalEngine = rulesService.globalEngine
        assert globalEngine != null
        globalEngine.disableTemporaryFactExpiration = true
        assert globalEngine.isRunning()
        assert globalEngine.deployments.size() == 1
        assert globalEngine.deployments.values().any { it -> it.name == "Some global demo rules" && it.status == DEPLOYED }

        assert rulesService.realmEngines.size() == 2
        masterEngine = rulesService.realmEngines.get(keycloakTestSetup.realmMaster.name)
        masterEngine.disableTemporaryFactExpiration = true
        assert masterEngine != null
        assert masterEngine.isRunning()
        assert masterEngine.deployments.size() == 1
        assert masterEngine.deployments.values().iterator().next().name == "Some master realm demo rules"
        assert masterEngine.deployments.values().iterator().next().status == DEPLOYED
        realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
        assert realmBuildingEngine != null
        realmBuildingEngine.disableTemporaryFactExpiration = true
        assert realmBuildingEngine.isRunning()
        assert realmBuildingEngine.deployments.size() == 1
        assert realmBuildingEngine.deployments.values().iterator().next().name == "Some building realm demo rules"
        assert realmBuildingEngine.deployments.values().iterator().next().status == DEPLOYED
        def realmCityEngine = rulesService.realmEngines.get(keycloakTestSetup.realmCity.name)
        assert realmCityEngine == null

        assert rulesService.assetEngines.size() == 2
        apartment1Engine = rulesService.assetEngines.get(managerTestSetup.apartment1Id)
        assert apartment1Engine == null
        apartment2Engine = rulesService.assetEngines.get(managerTestSetup.apartment2Id)
        assert apartment2Engine != null
        apartment2Engine.disableTemporaryFactExpiration = true
        assert apartment2Engine.isRunning()
        assert apartment2Engine.deployments.size() == 1
        assert apartment2Engine.deployments.values().iterator().next().name == "Some apartment 2 demo rules"
        assert apartment2Engine.deployments.values().iterator().next().status == DEPLOYED
        apartment3Engine = rulesService.assetEngines.get(managerTestSetup.apartment3Id)
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
        Collection<RulesEngine> engines = [globalEngine, masterEngine, realmBuildingEngine, apartment2Engine, apartment3Engine]
        engines.addAll(engine)
        engines.forEach({ e ->
            e.facts.namedFacts.keySet().forEach({ factName ->
                e.facts.remove(factName)
            })
        })
    }

    void assertNoRulesFired(RulesEngine... engine) {
        Collection<RulesEngine> engines = [globalEngine, masterEngine, realmBuildingEngine, apartment2Engine, apartment3Engine]
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
