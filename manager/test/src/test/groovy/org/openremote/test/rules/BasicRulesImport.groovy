package org.openremote.test.rules

import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.rules.AssetRuleset
import org.openremote.manager.shared.rules.GlobalRuleset
import org.openremote.manager.shared.rules.Ruleset
import org.openremote.manager.shared.rules.TenantRuleset

class BasicRulesImport {

    def customerBRulesetId
    def apartment1RulesetId

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
}
