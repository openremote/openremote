package org.openremote.test.rules

import org.openremote.container.web.ClientRequestInfo
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.TenantRuleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.rules.RulesetStatus.*
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.rules.Ruleset.Lang.GROOVY

class BasicRulesDeploymentTest extends Specification implements ManagerContainerTrait {

    def "Check basic rules engine deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakDemoSetup, managerDemoSetup)

        and: "an authenticated user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
        ).token

        expect: "the rules engines to be ready"
        new PollingConditions(initialDelay: 3, timeout: 10, delay: 1).eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakDemoSetup, managerDemoSetup)
        }

        when: "a new global rule definition is added"
        def ruleset = new GlobalRuleset(
                "Some more global rules",
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates2.groovy").text,
                GROOVY
        )
        rulesetStorageService.merge(ruleset)

        then: "the global rules engine should load this definition and restart successfully"
        conditions.eventually {
            assert rulesService.globalEngine != null
            assert rulesService.globalEngine.isRunning()
            assert rulesService.globalEngine.deployments.size() == 2
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some global demo rules" && it.status == DEPLOYED})
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some more global rules" && it.status == DEPLOYED})
        }

        when: "a new tenant rule definition is added to Tenant A"
        ruleset = new TenantRuleset(
                "Some more tenantA tenant rules",
                keycloakDemoSetup.tenantA.realm,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates2.groovy").text,
                GROOVY, false
        )
        rulesetStorageService.merge(ruleset)

        then: "Tenant A rules engine should load this definition and restart successfully"
        conditions.eventually {
            def tenantAEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantA.realm)
            assert tenantAEngine != null
            assert tenantAEngine.isRunning()
            assert tenantAEngine.deployments.size() == 2
            assert tenantAEngine.deployments.values().any({ it.name == "Some tenantA tenant demo rules" && it.status == DEPLOYED})
            assert tenantAEngine.deployments.values().any({ it.name == "Some more tenantA tenant rules" && it.status == DEPLOYED})
        }

        when: "a new tenant rule definition is added to Tenant B"
        ruleset = new TenantRuleset(
                "Some more tenantB tenant rules",
                keycloakDemoSetup.tenantB.realm,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates2.groovy").text,
                GROOVY, false
        )
        rulesetStorageService.merge(ruleset)

        then: "a tenant rules engine should be created for Tenant B and load this definition and start successfully"
        conditions.eventually {
            def tenantBEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantB.realm)
            assert rulesService.tenantEngines.size() == 3
            assert tenantBEngine != null
            assert tenantBEngine.isRunning()
            assert tenantBEngine.deployments.size() == 1
            assert tenantBEngine.deployments.values().any({ it.name == "Some more tenantB tenant rules" && it.status == DEPLOYED})
        }

        when: "the disabled rule definition for Tenant B is enabled"
        ruleset = rulesetStorageService.findById(TenantRuleset.class, rulesImport.tenantBRulesetId)
        ruleset.setEnabled(true)
        rulesetStorageService.merge(ruleset)

        then: "Tenant B rule engine should load this definition and restart successfully"
        conditions.eventually {
            def tenantBEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantB.realm)
            assert rulesService.tenantEngines.size() == 3
            assert tenantBEngine != null
            assert tenantBEngine.isRunning()
            assert tenantBEngine.deployments.size() == 2
            assert tenantBEngine.deployments.values().any({ it.name == "Some more tenantB tenant rules" && it.status == DEPLOYED})
            assert tenantBEngine.deployments.values().any({ it.name == "Some tenantB tenant demo rules" && it.status == DEPLOYED})
        }

        when: "the enabled rule definition for Tenant B is disabled"
        // TODO: Stop instances of rule definitions being passed around as rules engine nulls the rules property
        ruleset = rulesetStorageService.findById(TenantRuleset.class, rulesImport.tenantBRulesetId)
        ruleset.setEnabled(false)
        rulesetStorageService.merge(ruleset)

        then: "Tenant B rule engine should remove it again"
        conditions.eventually {
            def tenantBEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantB.realm)
            assert tenantBEngine != null
            assert tenantBEngine.isRunning()
            assert tenantBEngine.deployments.size() == 1
            assert tenantBEngine.deployments.values().any({ it.name == "Some more tenantB tenant rules" && it.status == DEPLOYED})
        }

        when: "the asset rule definition for apartment 2 is deleted"
        rulesetStorageService.delete(AssetRuleset.class, rulesImport.apartment2RulesetId)

        then: "the apartment rules engine should be removed"
        conditions.eventually {
            assert rulesService.assetEngines.size() == 2
            def apartment2Engine = rulesService.assetEngines.get(managerDemoSetup.apartment2Id)
            def apartment3Engine = rulesService.assetEngines.get(managerDemoSetup.apartment3Id)
            assert apartment2Engine == null
            assert apartment3Engine != null
            assert apartment3Engine.isRunning()
        }

        when: "a broken rule definition is added to the global rules engine"
        ruleset = new GlobalRuleset(
                "Some broken global rules",
                getClass().getResource("/org/openremote/test/rules/BasicBrokenRules.groovy").text,
                GROOVY
        )
        ruleset = rulesetStorageService.merge(ruleset)

        then: "the global rules engine should not run and the rule engine status should indicate the issue"
        conditions.eventually {
            assert rulesService.globalEngine.deployments.size() == 3
            assert rulesService.globalEngine.running == false
            assert rulesService.globalEngine.isError()
            assert rulesService.globalEngine.error instanceof RuntimeException
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some global demo rules" && it.status == READY})
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some more global rules" && it.status == READY})
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some broken global rules" && it.status == COMPILATION_ERROR})
        }

        when: "the broken rule definition is removed from the global engine"
        rulesetStorageService.delete(GlobalRuleset.class, ruleset.getId())

        then: "the global rules engine should restart"
        conditions.eventually {
            assert rulesService.globalEngine.deployments.size() == 2
            assert rulesService.globalEngine.running == true
            assert rulesService.globalEngine.isError() == false
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some global demo rules" && it.status == DEPLOYED })
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some more global rules" && it.status == DEPLOYED })
        }

        when: "a tenant is disabled"
        def tenantAEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantA.realm)
        def apartment3Engine = rulesService.assetEngines.get(managerDemoSetup.apartment3Id)
        def tenantATenant = keycloakDemoSetup.tenantA
        tenantATenant.setEnabled(false)
        identityService.getIdentityProvider().updateTenant(new ClientRequestInfo(null, accessToken), tenantATenant.getRealm(), tenantATenant)

        then: "the tenants rule engine should stop and all asset rule engines in this realm should also stop"
        conditions.eventually {
            assert tenantAEngine.isRunning() == false
            assert tenantAEngine.deployments.size() == 2
            assert rulesService.tenantEngines.get(keycloakDemoSetup.tenantA.realm) == null
            assert apartment3Engine.isRunning() == false
            assert apartment3Engine.deployments.size() == 1
            assert rulesService.assetEngines.get(managerDemoSetup.apartment3Id) == null
        }

        and: "other rule engines should be unaffected"
        conditions.eventually {
            assert rulesService.tenantEngines.size() == 2
            assert rulesService.assetEngines.size() == 1
            def masterEngine = rulesService.tenantEngines.get(keycloakDemoSetup.masterTenant.realm)
            def tenantBEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantB.realm)
            assert masterEngine != null
            assert masterEngine.isRunning()
            assert tenantBEngine != null
            assert tenantBEngine.isRunning()
        }

        when: "the disabled tenant is re-enabled"
        tenantATenant.setEnabled(true)
        identityService.getIdentityProvider().updateTenant(new ClientRequestInfo(null, accessToken), tenantATenant.getRealm(), tenantATenant)

        then: "the tenants rule engine should start and all asset rule engines from this realm should also start"
        conditions.eventually {
            tenantAEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantA.realm)
            apartment3Engine = rulesService.assetEngines.get(managerDemoSetup.apartment3Id)
            assert rulesService.tenantEngines.size() == 3
            assert rulesService.assetEngines.size() == 2
            assert tenantAEngine != null
            assert tenantAEngine.isRunning()
            assert tenantAEngine.deployments.size() == 2
            assert tenantAEngine.deployments.values().any({ it.name == "Some tenantA tenant demo rules" && it.status == DEPLOYED})
            assert tenantAEngine.deployments.values().any({ it.name == "Some more tenantA tenant rules" && it.status == DEPLOYED})
            assert apartment3Engine.deployments.size() == 1
            assert apartment3Engine.deployments.values().any({ it.name == "Some apartment 3 demo rules" && it.status == DEPLOYED})
        }

//TODO: Reinstate the tenant delete test once tenant delete mechanism is finalised
//        when: "a tenant is deleted"
//        identityService.deleteTenant(accessToken, tenantATenant.getRealm())
//
//        then: "the tenants rule engine should stop and all asset rule engines in this realm should also stop"
//        conditions.eventually {
//            assert tenantAEngine.isRunning() == false
//            assert tenantAEngine.allRulesets.length == 0
//            assert rulesService.tenantEngines.get(keycloakDemoSetup.tenantATenant.id) == null
//            assert smartHomeEngine.isRunning() == false
//            assert smartHomeEngine.allRulesets.length == 0
//            assert rulesService.assetEngines.get(managerDemoSetup.smartBuildingId) == null
//            assert apartment3Engine.isRunning() == false
//            assert apartment3Engine.allRulesets.length == 0
//            assert rulesService.assetEngines.get(managerDemoSetup.apartment3Id) == null
//        }
//
//        and: "other rule engines should be unaffected"
//        conditions.eventually {
//            assert rulesService.tenantEngines.size() == 2
//            assert rulesService.assetEngines.size() == 0
//            def masterEngine = rulesService.tenantEngines.get(Constants.MASTER_REALM)
//            def tenantBEngine = rulesService.tenantEngines.get(keycloakDemoSetup.tenantBTenant.id)
//            assert masterEngine != null
//            assert masterEngine.isRunning()
//            assert tenantBEngine != null
//            assert tenantBEngine.isRunning()
//        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
