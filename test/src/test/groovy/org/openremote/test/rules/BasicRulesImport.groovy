package org.openremote.test.rules

import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.Ruleset.DeploymentStatus
import org.openremote.model.rules.TenantRuleset

class BasicRulesImport {

    final Long globalRulesetId
    final Long globalRuleset2Id
    final Long masterRulesetId
    final Long customerARulesetId
    final Long customerBRulesetId
    final Long apartment1RulesetId
    final Long apartment2RulesetId
    final Long apartment3RulesetId

    RulesEngine globalEngine
    RulesEngine masterEngine
    RulesEngine customerAEngine
    RulesEngine apartment1Engine
    RulesEngine apartment2Engine
    RulesEngine apartment3Engine

    BasicRulesImport(RulesetStorageService rulesetStorageService,
                     KeycloakDemoSetup keycloakDemoSetup,
                     ManagerDemoSetup managerDemoSetup) {

        Ruleset ruleset = new GlobalRuleset(
                "Some global demo rules",
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.drl").text
        )
        globalRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new GlobalRuleset(
                "Other global demo rules with a long name that should fill up space in UI",
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.drl").text
        )
        ruleset.setEnabled(false)
        globalRuleset2Id = rulesetStorageService.merge(ruleset).id

        ruleset = new TenantRuleset(
                "Some master tenant demo rules",
                keycloakDemoSetup.masterTenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.drl").text
        )
        masterRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new TenantRuleset(
                "Some customerA tenant demo rules",
                keycloakDemoSetup.customerATenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.drl").text
        )
        customerARulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new TenantRuleset(
                "Some customerB tenant demo rules",
                keycloakDemoSetup.customerBTenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.drl").text
        )
        ruleset.setEnabled(false)
        customerBRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
                "Some apartment 1 demo rules",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.drl").text
        )
        ruleset.setEnabled(false)
        apartment1RulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
                "Some apartment 2 demo rules",
                managerDemoSetup.apartment2Id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.drl").text
        )
        apartment2RulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
                "Some apartment 3 demo rules",
                managerDemoSetup.apartment3Id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates.drl").text
        )
        apartment3RulesetId = rulesetStorageService.merge(ruleset).id
    }

    boolean assertEnginesReady(RulesService rulesService,
                               KeycloakDemoSetup keycloakDemoSetup,
                               ManagerDemoSetup managerDemoSetup) {

        globalEngine = rulesService.globalEngine
        assert globalEngine != null
        assert globalEngine.isRunning()
        assert globalEngine != null
        assert globalEngine.isRunning()
        assert globalEngine.allRulesets.length == 1
        assert globalEngine.allRulesets[0].name == "Some global demo rules"
        assert globalEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED

        assert rulesService.tenantEngines.size() == 2
        masterEngine = rulesService.tenantEngines.get(keycloakDemoSetup.masterTenant.id)
        assert masterEngine != null
        assert masterEngine.isRunning()
        assert masterEngine.allRulesets.length == 1
        assert masterEngine.allRulesets[0].enabled
        assert masterEngine.allRulesets[0].name == "Some master tenant demo rules"
        assert masterEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        customerAEngine = rulesService.tenantEngines.get(keycloakDemoSetup.customerATenant.id)
        assert customerAEngine != null
        assert customerAEngine.isRunning()
        assert customerAEngine.allRulesets.length == 1
        assert customerAEngine.allRulesets[0].enabled
        assert customerAEngine.allRulesets[0].name == "Some customerA tenant demo rules"
        assert customerAEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        def customerBEngine = rulesService.tenantEngines.get(keycloakDemoSetup.customerBTenant.id)
        assert customerBEngine == null

        assert rulesService.assetEngines.size() == 2
        apartment1Engine = rulesService.assetEngines.get(managerDemoSetup.apartment1Id)
        assert apartment1Engine == null
        apartment2Engine = rulesService.assetEngines.get(managerDemoSetup.apartment2Id)
        assert apartment2Engine != null
        assert apartment2Engine.isRunning()
        assert apartment2Engine.allRulesets.length == 1
        assert apartment2Engine.allRulesets[0].enabled
        assert apartment2Engine.allRulesets[0].name == "Some apartment 2 demo rules"
        assert apartment2Engine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        apartment3Engine = rulesService.assetEngines.get(managerDemoSetup.apartment3Id)
        assert apartment3Engine != null
        assert apartment3Engine.isRunning()
        assert apartment3Engine.allRulesets.length == 1
        assert apartment3Engine.allRulesets[0].enabled
        assert apartment3Engine.allRulesets[0].name == "Some apartment 3 demo rules"
        assert apartment3Engine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED

        return true
    }
}
