package org.openremote.test.rules

import org.openremote.manager.server.rules.RulesDeployment
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.rules.AssetRuleset
import org.openremote.manager.shared.rules.GlobalRuleset
import org.openremote.manager.shared.rules.Ruleset
import org.openremote.manager.shared.rules.Ruleset.DeploymentStatus
import org.openremote.manager.shared.rules.TenantRuleset

class BasicRulesImport {

    Long customerBRulesetId
    Long apartment1RulesetId

    RulesDeployment globalEngine
    RulesDeployment masterEngine
    RulesDeployment customerAEngine
    RulesDeployment apartment1Engine
    RulesDeployment apartment3Engine

    BasicRulesImport(RulesetStorageService rulesetStorageService,
                     KeycloakDemoSetup keycloakDemoSetup,
                     ManagerDemoSetup managerDemoSetup) {

        Ruleset ruleset = new GlobalRuleset(
                "Some global demo rules",
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates.drl").text
        )
        rulesetStorageService.merge(ruleset)

        ruleset = new GlobalRuleset(
                "Other global demo rules with a long name that should fill up space in UI",
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates.drl").text
        )
        ruleset.setEnabled(false)
        rulesetStorageService.merge(ruleset)

        ruleset = new TenantRuleset(
                "Some master tenant demo rules",
                keycloakDemoSetup.masterTenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates.drl").text
        )
        rulesetStorageService.merge(ruleset)

        ruleset = new TenantRuleset(
                "Some customerA tenant demo rules",
                keycloakDemoSetup.customerATenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates.drl").text
        )
        rulesetStorageService.merge(ruleset)

        ruleset = new TenantRuleset(
                "Some customerB tenant demo rules",
                keycloakDemoSetup.customerBTenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates.drl").text
        )
        ruleset.setEnabled(false)
        customerBRulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
                "Some apartment 1 demo rules",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates.drl").text
        )
        apartment1RulesetId = rulesetStorageService.merge(ruleset).id

        ruleset = new AssetRuleset(
                "Some apartment 2 demo rules",
                managerDemoSetup.apartment2Id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates.drl").text
        )
        ruleset.setEnabled(false)
        rulesetStorageService.merge(ruleset)

        ruleset = new AssetRuleset(
                "Some apartment 3 demo rules",
                managerDemoSetup.apartment3Id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates.drl").text
        )
        rulesetStorageService.merge(ruleset)
    }

    boolean assertEnginesReady(RulesService rulesService,
                               KeycloakDemoSetup keycloakDemoSetup,
                               ManagerDemoSetup managerDemoSetup) {

        globalEngine = rulesService.globalDeployment
        assert globalEngine != null
        assert globalEngine.isRunning()
        assert globalEngine != null
        assert globalEngine.isRunning()
        assert globalEngine.allRulesets.length == 1
        assert globalEngine.allRulesets[0].name == "Some global demo rules"
        assert globalEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED

        assert rulesService.tenantDeployments.size() == 2
        masterEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.masterTenant.id)
        assert masterEngine != null
        assert masterEngine.isRunning()
        assert masterEngine.allRulesets.length == 1
        assert masterEngine.allRulesets[0].enabled
        assert masterEngine.allRulesets[0].name == "Some master tenant demo rules"
        assert masterEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
        assert customerAEngine != null
        assert customerAEngine.isRunning()
        assert customerAEngine.allRulesets.length == 1
        assert customerAEngine.allRulesets[0].enabled
        assert customerAEngine.allRulesets[0].name == "Some customerA tenant demo rules"
        assert customerAEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED

        assert rulesService.assetDeployments.size() == 2
        apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
        apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
        assert apartment1Engine != null
        assert apartment1Engine.isRunning()
        assert apartment1Engine.allRulesets.length == 1
        assert apartment1Engine.allRulesets[0].enabled
        assert apartment1Engine.allRulesets[0].name == "Some apartment 1 demo rules"
        assert apartment1Engine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        assert apartment3Engine != null
        assert apartment3Engine.isRunning()
        assert apartment3Engine.allRulesets.length == 1
        assert apartment3Engine.allRulesets[0].enabled
        assert apartment3Engine.allRulesets[0].name == "Some apartment 3 demo rules"
        assert apartment3Engine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED

        return true
    }
}
