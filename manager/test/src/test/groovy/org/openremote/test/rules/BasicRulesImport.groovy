package org.openremote.test.rules

import org.openremote.manager.server.rules.RulesStorageService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.rules.AssetRulesDefinition
import org.openremote.manager.shared.rules.GlobalRulesDefinition
import org.openremote.manager.shared.rules.RulesDefinition
import org.openremote.manager.shared.rules.TenantRulesDefinition

class BasicRulesImport {

    def customerBRulesDefinitionId
    def apartment1RulesDefinitionId

    BasicRulesImport(RulesStorageService rulesStorageService,
                     KeycloakDemoSetup keycloakDemoSetup,
                     ManagerDemoSetup managerDemoSetup) {

        RulesDefinition rulesDefinition = new GlobalRulesDefinition(
                "Some global demo rules",
                getClass().getResource("/org/openremote/test/rules/MatchAllAssetUpdates.drl").text
        )
        rulesStorageService.merge(rulesDefinition)

        rulesDefinition = new GlobalRulesDefinition(
                "Other global demo rules with a long name that should fill up space in UI",
                getClass().getResource("/org/openremote/test/rules/MatchAllAssetUpdates.drl").text
        )
        rulesDefinition.setEnabled(false)
        rulesStorageService.merge(rulesDefinition)

        rulesDefinition = new TenantRulesDefinition(
                "Some master tenant demo rules",
                keycloakDemoSetup.masterTenant.id,
                getClass().getResource("/org/openremote/test/rules/MatchAllAssetUpdates.drl").text
        )
        rulesStorageService.merge(rulesDefinition)

        rulesDefinition = new TenantRulesDefinition(
                "Some customerA tenant demo rules",
                keycloakDemoSetup.customerATenant.id,
                getClass().getResource("/org/openremote/test/rules/MatchAllAssetUpdates.drl").text
        )
        rulesStorageService.merge(rulesDefinition)

        rulesDefinition = new TenantRulesDefinition(
                "Some customerB tenant demo rules",
                keycloakDemoSetup.customerBTenant.id,
                getClass().getResource("/org/openremote/test/rules/MatchAllAssetUpdates.drl").text
        )
        rulesDefinition.setEnabled(false)
        customerBRulesDefinitionId = rulesStorageService.merge(rulesDefinition).id

        rulesDefinition = new AssetRulesDefinition(
                "Some apartment 1 demo rules",
                managerDemoSetup.apartment1Id,
                getClass().getResource("/org/openremote/test/rules/MatchAllAssetUpdates.drl").text
        )
        apartment1RulesDefinitionId = rulesStorageService.merge(rulesDefinition).id

        rulesDefinition = new AssetRulesDefinition(
                "Some apartment 2 demo rules",
                managerDemoSetup.apartment2Id,
                getClass().getResource("/org/openremote/test/rules/MatchAllAssetUpdates.drl").text
        )
        rulesDefinition.setEnabled(false)
        rulesStorageService.merge(rulesDefinition)

        rulesDefinition = new AssetRulesDefinition(
                "Some apartment 3 demo rules",
                managerDemoSetup.apartment3Id,
                getClass().getResource("/org/openremote/test/rules/MatchAllAssetUpdates.drl").text
        )
        rulesStorageService.merge(rulesDefinition)
    }
}
