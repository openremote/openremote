package org.openremote.test.rules

import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.security.ManagerIdentityService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.rules.AssetRuleset
import org.openremote.manager.shared.rules.GlobalRuleset
import org.openremote.manager.shared.rules.Ruleset.DeploymentStatus
import org.openremote.manager.shared.rules.TenantRuleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT
import static org.openremote.manager.server.setup.builtin.BuiltinSetupTasks.SETUP_IMPORT_DEMO_RULES
import static org.openremote.model.Constants.*

class BasicRulesDeploymentTest extends Specification implements ManagerContainerTrait {

    def "Check basic rules engine deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 5)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort) << [(SETUP_IMPORT_DEMO_RULES): "false"], defaultServices())
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
                getString(container.getConfig(), SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT)
        ).token

        expect: "a global rules engine should have been created and be running"
        conditions.eventually {
            assert rulesService.globalDeployment != null
            assert rulesService.globalDeployment.isRunning()
        }

        and: "the global rules engine should contain the global demo ruleset"
        conditions.eventually {
            assert rulesService.globalDeployment.allRulesets.length == 1
            assert rulesService.globalDeployment.allRulesets[0].enabled
            assert rulesService.globalDeployment.allRulesets[0].name == "Some global demo rules"
            assert rulesService.globalDeployment.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        and: "two tenant rules engines should have been created and be running"
        conditions.eventually {
            assert rulesService.tenantDeployments.size() == 2
            def masterEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.masterTenant.id)
            def customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert masterEngine != null
            assert masterEngine.isRunning()
            assert customerAEngine != null
            assert customerAEngine.isRunning()
        }

        and: "the tenant rules engines should have the demo tenant ruleset"
        conditions.eventually {
            def masterEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.masterTenant.id)
            def customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert masterEngine.allRulesets.length == 1
            assert masterEngine.allRulesets[0].enabled
            assert masterEngine.allRulesets[0].name == "Some master tenant demo rules"
            assert masterEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert customerAEngine.allRulesets.length == 1
            assert customerAEngine.allRulesets[0].enabled
            assert customerAEngine.allRulesets[0].name == "Some customerA tenant demo rules"
            assert customerAEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        and: "two asset rules engines should have been created and be running"
        conditions.eventually {
            assert rulesService.assetDeployments.size() == 2
            def apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            def apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            assert apartment3Engine != null
            assert apartment3Engine.isRunning()
        }

        and: "each asset rules engine should have the demo asset ruleset"
        conditions.eventually {
            def apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            def apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
            assert apartment1Engine.allRulesets.length == 1
            assert apartment1Engine.allRulesets[0].enabled
            assert apartment1Engine.allRulesets[0].name == "Some apartment 1 demo rules"
            assert apartment1Engine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert apartment3Engine.allRulesets.length == 1
            assert apartment3Engine.allRulesets[0].enabled
            assert apartment3Engine.allRulesets[0].name == "Some apartment 3 demo rules"
            assert apartment3Engine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "a new global rule definition is added"
        def ruleset = new GlobalRuleset(
                "Some more global rules",
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates2.drl").text
        )
        ruleset = rulesetStorageService.merge(ruleset)

        then: "the global rules engine should load this definition and restart successfully"
        conditions.eventually {
            assert rulesService.globalDeployment != null
            assert rulesService.globalDeployment.isRunning()
            assert rulesService.globalDeployment.allRulesets.length == 2
            assert rulesService.globalDeployment.allRulesets[0].enabled
            assert rulesService.globalDeployment.allRulesets[0].name == "Some global demo rules"
            assert rulesService.globalDeployment.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert rulesService.globalDeployment.allRulesets[1].enabled
            assert rulesService.globalDeployment.allRulesets[1].name == "Some more global rules"
            assert rulesService.globalDeployment.allRulesets[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "a new tenant rule definition is added to customer A"
        ruleset = new TenantRuleset(
                "Some more customerA tenant rules",
                keycloakDemoSetup.customerATenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates2.drl").text
        )
        ruleset = rulesetStorageService.merge(ruleset)

        then: "customer A rules engine should load this definition and restart successfully"
        conditions.eventually {
            def customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            assert customerAEngine.allRulesets.length == 2
            assert customerAEngine.allRulesets[0].enabled
            assert customerAEngine.allRulesets[0].name == "Some customerA tenant demo rules"
            assert customerAEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert customerAEngine.allRulesets[1].enabled
            assert customerAEngine.allRulesets[1].name == "Some more customerA tenant rules"
            assert customerAEngine.allRulesets[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "a new tenant rule definition is added to customer B"
        ruleset = new TenantRuleset(
                "Some more customerB tenant rules",
                keycloakDemoSetup.customerBTenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetUpdates2.drl").text
        )
        ruleset = rulesetStorageService.merge(ruleset)

        then: "a tenant rules engine should be created for customer B and load this definition and start successfully"
        conditions.eventually {
            def customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
            assert rulesService.tenantDeployments.size() == 3
            assert customerBEngine != null
            assert customerBEngine.isRunning()
            assert customerBEngine.allRulesets.length == 1
            assert customerBEngine.allRulesets[0].enabled
            assert customerBEngine.allRulesets[0].name == "Some more customerB tenant rules"
            assert customerBEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the disabled rule definition for customer B is enabled"
        ruleset = rulesetStorageService.findById(TenantRuleset.class, rulesImport.customerBRulesetId)
        ruleset.setEnabled(true)
        rulesetStorageService.merge(ruleset)

        then: "customer B rule engine should load this definition and restart successfully"
        conditions.eventually {
            def customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
            assert rulesService.tenantDeployments.size() == 3
            assert customerBEngine != null
            assert customerBEngine.isRunning()
            assert customerBEngine.allRulesets.length == 2
            assert customerBEngine.allRulesets[0].enabled
            assert customerBEngine.allRulesets[0].name == "Some more customerB tenant rules"
            assert customerBEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert customerBEngine.allRulesets[1].enabled
            assert customerBEngine.allRulesets[1].name == "Some customerB tenant demo rules"
            assert customerBEngine.allRulesets[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the enabled rule definition for customer B is disabled"
        // TODO: Stop instances of rule definitions being passed around as rules deployment nulls the rules property
        ruleset = rulesetStorageService.findById(TenantRuleset.class, rulesImport.customerBRulesetId)
        ruleset.setEnabled(false)
        rulesetStorageService.merge(ruleset)

        then: "customer B rule engine should remove it again"
        conditions.eventually {
            def customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
            assert customerBEngine != null
            assert customerBEngine.isRunning()
            assert customerBEngine.allRulesets.length == 1
            assert customerBEngine.allRulesets[0].enabled
            assert customerBEngine.allRulesets[0].name == "Some more customerB tenant rules"
            assert customerBEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the asset rule definition for apartment 1 is deleted"
        rulesetStorageService.delete(AssetRuleset.class, rulesImport.apartment1RulesetId)

        then: "the apartment rules engine should be removed"
        conditions.eventually {
            assert rulesService.assetDeployments.size() == 1
            def apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            def apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
            assert apartment1Engine == null
            assert apartment3Engine != null
            assert apartment3Engine.isRunning()
        }

        when: "a broken rule definition is added to the global rules engine"
        ruleset = new GlobalRuleset(
                "Some broken global rules",
                getClass().getResource("/org/openremote/test/rules/BasicBrokenRules.drl").text
        )
        ruleset = rulesetStorageService.merge(ruleset)

        then: "the global rules engine should not run and the rule deployment status should indicate the issue"
        conditions.eventually {
            assert rulesService.globalDeployment.allRulesets.length == 3
            assert rulesService.globalDeployment.running == false
            assert rulesService.globalDeployment.error == true
            assert rulesService.globalDeployment.allRulesets[0].enabled
            assert rulesService.globalDeployment.allRulesets[0].name == "Some global demo rules"
            assert rulesService.globalDeployment.allRulesets[0].deploymentStatus == DeploymentStatus.READY
            assert rulesService.globalDeployment.allRulesets[1].enabled
            assert rulesService.globalDeployment.allRulesets[1].name == "Some more global rules"
            assert rulesService.globalDeployment.allRulesets[1].deploymentStatus == DeploymentStatus.READY
            assert rulesService.globalDeployment.allRulesets[2].enabled
            assert rulesService.globalDeployment.allRulesets[2].name == "Some broken global rules"
            assert rulesService.globalDeployment.allRulesets[2].deploymentStatus == DeploymentStatus.FAILED
        }

        when: "the broken rule definition is removed from the global engine"
        rulesetStorageService.delete(GlobalRuleset.class, ruleset.getId())

        then: "the global rules engine should restart"
        conditions.eventually {
            assert rulesService.globalDeployment.allRulesets.length == 2
            assert rulesService.globalDeployment.running == true
            assert rulesService.globalDeployment.error == false
            assert rulesService.globalDeployment.allRulesets[0].enabled
            assert rulesService.globalDeployment.allRulesets[0].name == "Some global demo rules"
            assert rulesService.globalDeployment.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert rulesService.globalDeployment.allRulesets[1].enabled
            assert rulesService.globalDeployment.allRulesets[1].name == "Some more global rules"
            assert rulesService.globalDeployment.allRulesets[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "a tenant is disabled"
        def customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
        def apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
        def customerATenant = keycloakDemoSetup.customerATenant
        customerATenant.setEnabled(false)
        identityService.updateTenant(accessToken, customerATenant.getRealm(), customerATenant)

        then: "the tenants rule engine should stop and all asset rule engines in this realm should also stop"
        conditions.eventually {
            assert customerAEngine.isRunning() == false
            assert customerAEngine.allRulesets.length == 0
            assert rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id) == null
            assert apartment3Engine.isRunning() == false
            assert apartment3Engine.allRulesets.length == 0
            assert rulesService.assetDeployments.get(managerDemoSetup.apartment3Id) == null
        }

        and: "other rule engines should be unaffected"
        conditions.eventually {
            assert rulesService.tenantDeployments.size() == 2
            assert rulesService.assetDeployments.size() == 0
            def masterEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.masterTenant.id)
            def customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
            assert masterEngine != null
            assert masterEngine.isRunning()
            assert customerBEngine != null
            assert customerBEngine.isRunning()
        }

        when: "the disabled tenant is re-enabled"
        customerATenant.setEnabled(true)
        identityService.updateTenant(accessToken, customerATenant.getRealm(), customerATenant)

        then: "the tenants rule engine should start and all asset rule engines from this realm should also start"
        conditions.eventually {
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
            assert rulesService.tenantDeployments.size() == 3
            assert rulesService.assetDeployments.size() == 1
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            assert customerAEngine.allRulesets.length == 2
            assert customerAEngine.allRulesets[0].enabled
            assert customerAEngine.allRulesets[0].name == "Some customerA tenant demo rules"
            assert customerAEngine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert customerAEngine.allRulesets[1].enabled
            assert customerAEngine.allRulesets[1].name == "Some more customerA tenant rules"
            assert customerAEngine.allRulesets[1].deploymentStatus == DeploymentStatus.DEPLOYED
            assert apartment3Engine.allRulesets.length == 1
            assert apartment3Engine.allRulesets[0].enabled
            assert apartment3Engine.allRulesets[0].name == "Some apartment 3 demo rules"
            assert apartment3Engine.allRulesets[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

//TODO: Reinstate the tenant delete test once tenant delete mechanism is finalised
//        when: "a tenant is deleted"
//        identityService.deleteTenant(accessToken, customerATenant.getRealm())
//
//        then: "the tenants rule engine should stop and all asset rule engines in this realm should also stop"
//        conditions.eventually {
//            assert customerAEngine.isRunning() == false
//            assert customerAEngine.allRulesets.length == 0
//            assert rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id) == null
//            assert smartHomeEngine.isRunning() == false
//            assert smartHomeEngine.allRulesets.length == 0
//            assert rulesService.assetDeployments.get(managerDemoSetup.smartHomeId) == null
//            assert apartment3Engine.isRunning() == false
//            assert apartment3Engine.allRulesets.length == 0
//            assert rulesService.assetDeployments.get(managerDemoSetup.apartment3Id) == null
//        }
//
//        and: "other rule engines should be unaffected"
//        conditions.eventually {
//            assert rulesService.tenantDeployments.size() == 2
//            assert rulesService.assetDeployments.size() == 0
//            def masterEngine = rulesService.tenantDeployments.get(Constants.MASTER_REALM)
//            def customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
//            assert masterEngine != null
//            assert masterEngine.isRunning()
//            assert customerBEngine != null
//            assert customerBEngine.isRunning()
//        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
