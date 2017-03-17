package org.openremote.test.rules

import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.security.ManagerIdentityService
import org.openremote.manager.shared.rules.*
import org.openremote.manager.shared.rules.RulesDefinition.DeploymentStatus
import org.openremote.model.Constants
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import org.apache.commons.io.IOUtils

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER;

class RulesDeploymentTest extends Specification implements ManagerContainerTrait {

    def "Check basic rules engine deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1)

        and: "the demo assets and rule definitions are deployed"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def rulesStorageService = container.getService(RulesStorageService.class)
        def customerARealmId = identityService.getActiveTenantRealmId("customerA")
        def customerBRealmId = identityService.getActiveTenantRealmId("customerB")

        and: "an authenticated user"
        def authRealm = MASTER_REALM
        def masterRealmId = getActiveTenantRealmId(container, MASTER_REALM)
        def accessToken = authenticate(
                container,
                authRealm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT)
        ).token

        expect: "a global rules engine should have been created and be running"
        conditions.eventually {
            assert rulesService.globalDeployment != null
            assert rulesService.globalDeployment.isRunning()
        }

        and: "the global rules engine should contain the global demo rules definition"
        conditions.eventually {
            assert rulesService.globalDeployment.allRulesDefinitions.length == 1
            assert rulesService.globalDeployment.allRulesDefinitions[0].enabled
            assert rulesService.globalDeployment.allRulesDefinitions[0].name == "Some global demo rules"
            assert rulesService.globalDeployment.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        and: "two tenant rules engines should have been created and be running"
        conditions.eventually {
            assert rulesService.tenantDeployments.size() == 2
            def masterEngine = rulesService.tenantDeployments.get(Constants.MASTER_REALM)
            def customerAEngine = rulesService.tenantDeployments.get(customerARealmId)
            assert masterEngine != null
            assert masterEngine.isRunning()
            assert customerAEngine != null
            assert customerAEngine.isRunning()
        }

        and: "the tenant rules engines should have the demo tenant rules definition"
        conditions.eventually {
            def masterEngine = rulesService.tenantDeployments.get(Constants.MASTER_REALM)
            def customerAEngine = rulesService.tenantDeployments.get(customerARealmId)
            assert masterEngine.allRulesDefinitions.length == 1
            assert masterEngine.allRulesDefinitions[0].enabled
            assert masterEngine.allRulesDefinitions[0].name == "Some master tenant demo rules"
            assert masterEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert customerAEngine.allRulesDefinitions.length == 1
            assert customerAEngine.allRulesDefinitions[0].enabled
            assert customerAEngine.allRulesDefinitions[0].name == "Some customerA tenant demo rules"
            assert customerAEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
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

        and: "each asset rules engine should have the demo asset rules definition"
        conditions.eventually {
            def apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            def apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
            assert apartment1Engine.allRulesDefinitions.length == 1
            assert apartment1Engine.allRulesDefinitions[0].enabled
            assert apartment1Engine.allRulesDefinitions[0].name == "Some apartment 1 demo rules"
            assert apartment1Engine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert apartment3Engine.allRulesDefinitions.length == 1
            assert apartment3Engine.allRulesDefinitions[0].enabled
            assert apartment3Engine.allRulesDefinitions[0].name == "Some apartment 3 demo rules"
            assert apartment3Engine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "a new global rule definition is added"
        def inputStream = getClass().getResourceAsStream("/org/openremote/test/rules/GlobalRules.drl")
        String rules = IOUtils.toString(inputStream, "UTF-8")
        def rulesDefinition = new GlobalRulesDefinition("Some more global rules", rules)
        rulesStorageService.merge(rulesDefinition)

        then: "the global rules engine should load this definition and restart successfully"
        conditions.eventually {
            assert rulesService.globalDeployment != null
            assert rulesService.globalDeployment.isRunning()
            assert rulesService.globalDeployment.allRulesDefinitions.length == 2
            assert rulesService.globalDeployment.allRulesDefinitions[0].enabled
            assert rulesService.globalDeployment.allRulesDefinitions[0].name == "Some global demo rules"
            assert rulesService.globalDeployment.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert rulesService.globalDeployment.allRulesDefinitions[1].enabled
            assert rulesService.globalDeployment.allRulesDefinitions[1].name == "Some more global rules"
            assert rulesService.globalDeployment.allRulesDefinitions[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "a new tenant rule definition is added to customer A"
        inputStream = getClass().getResourceAsStream("/org/openremote/test/rules/TenantRules.drl")
        rules = IOUtils.toString(inputStream, "UTF-8")
        rulesDefinition = new TenantRulesDefinition("Some more customerA tenant rules", customerARealmId, rules)
        rulesStorageService.merge(rulesDefinition)

        then: "customer A rules engine should load this definition and restart successfully"
        conditions.eventually {
            def customerAEngine = rulesService.tenantDeployments.get(customerARealmId)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            assert customerAEngine.allRulesDefinitions.length == 2
            assert customerAEngine.allRulesDefinitions[0].enabled
            assert customerAEngine.allRulesDefinitions[0].name == "Some customerA tenant demo rules"
            assert customerAEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert customerAEngine.allRulesDefinitions[1].enabled
            assert customerAEngine.allRulesDefinitions[1].name == "Some more customerA tenant rules"
            assert customerAEngine.allRulesDefinitions[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "a new tenant rule definition is added to customer B"
        inputStream = getClass().getResourceAsStream("/org/openremote/test/rules/TenantRules.drl")
        rules = IOUtils.toString(inputStream, "UTF-8")
        rulesDefinition = new TenantRulesDefinition("Some more customerB tenant rules", customerBRealmId, rules)
        rulesStorageService.merge(rulesDefinition)

        then: "a tenant rules engine should be created for customer B and load this definition and start successfully"
        conditions.eventually {
            def customerBEngine = rulesService.tenantDeployments.get(customerBRealmId)
            assert customerBEngine != null
            assert customerBEngine.isRunning()
            assert customerBEngine.allRulesDefinitions.length == 1
            assert customerBEngine.allRulesDefinitions[0].enabled
            assert customerBEngine.allRulesDefinitions[0].name == "Some more customerB tenant rules"
            assert customerBEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the disabled rule definition for customer B is enabled"
        rulesDefinition = rulesStorageService.findById(TenantRulesDefinition.class, managerDemoSetup.customerBRulesDefinitionId)
        rulesDefinition.setEnabled(true)
        rulesDefinition = rulesStorageService.merge(rulesDefinition)

        then: "customer B rule engine should load this definition and restart successfully"
        conditions.eventually {
            def customerBEngine = rulesService.tenantDeployments.get(customerBRealmId)
            assert customerBEngine != null
            assert customerBEngine.isRunning()
            assert customerBEngine.allRulesDefinitions.length == 2
            assert customerBEngine.allRulesDefinitions[0].enabled
            assert customerBEngine.allRulesDefinitions[0].name == "Some more customerB tenant rules"
            assert customerBEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert customerBEngine.allRulesDefinitions[1].enabled
            assert customerBEngine.allRulesDefinitions[1].name == "Some customerB tenant demo rules"
            assert customerBEngine.allRulesDefinitions[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the enabled rule definition for customer B is disabled"
        rulesDefinition.setEnabled(false)
        rulesStorageService.merge(rulesDefinition)

        then: "customer B rule engine should should remove it again"
        conditions.eventually {
            def customerBEngine = rulesService.tenantDeployments.get(customerBRealmId)
            assert customerBEngine != null
            assert customerBEngine.isRunning()
            assert customerBEngine.allRulesDefinitions.length == 1
            assert customerBEngine.allRulesDefinitions[0].enabled
            assert customerBEngine.allRulesDefinitions[0].name == "Some more customerB tenant rules"
            assert customerBEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the rule definition for apartment 1 is deleted"
        rulesStorageService.delete(AssetRulesDefinition.class, managerDemoSetup.apartment1RulesDefinitionId)

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
        inputStream = getClass().getResourceAsStream("/org/openremote/test/rules/BrokenRules.drl")
        rules = IOUtils.toString(inputStream, "UTF-8")
        rulesDefinition = new GlobalRulesDefinition("Some broken global rules", rules)
        rulesDefinition = rulesStorageService.merge(rulesDefinition)

        then: "the global rules engine should not run and the rule deployment status status should indicate the issue"
        conditions.eventually {
            assert rulesService.globalDeployment.allRulesDefinitions.length == 3
            assert rulesService.globalDeployment.running == false
            assert rulesService.globalDeployment.error == true
            assert rulesService.globalDeployment.allRulesDefinitions[0].enabled
            assert rulesService.globalDeployment.allRulesDefinitions[0].name == "Some global demo rules"
            assert rulesService.globalDeployment.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.READY
            assert rulesService.globalDeployment.allRulesDefinitions[1].enabled
            assert rulesService.globalDeployment.allRulesDefinitions[1].name == "Some more global rules"
            assert rulesService.globalDeployment.allRulesDefinitions[1].deploymentStatus == DeploymentStatus.READY
            assert rulesService.globalDeployment.allRulesDefinitions[2].enabled
            assert rulesService.globalDeployment.allRulesDefinitions[2].name == "Some broken global rules"
            assert rulesService.globalDeployment.allRulesDefinitions[2].deploymentStatus == DeploymentStatus.FAILED
        }

        when: "the broken rule definition is removed from the global engine"
        rulesStorageService.delete(GlobalRulesDefinition.class, rulesDefinition.getId())

        then: "the global rules engine should restart"
        conditions.eventually {
            assert rulesService.globalDeployment.allRulesDefinitions.length == 2
            assert rulesService.globalDeployment.running == true
            assert rulesService.globalDeployment.error == false
            assert rulesService.globalDeployment.allRulesDefinitions[0].enabled
            assert rulesService.globalDeployment.allRulesDefinitions[0].name == "Some global demo rules"
            assert rulesService.globalDeployment.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert rulesService.globalDeployment.allRulesDefinitions[1].enabled
            assert rulesService.globalDeployment.allRulesDefinitions[1].name == "Some more global rules"
            assert rulesService.globalDeployment.allRulesDefinitions[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "a tenant is disabled"
        def customerAEngine = rulesService.tenantDeployments.get(customerARealmId)
        def customerATenant = keycloakDemoSetup.customerATenant
        customerATenant.setEnabled(false)
        identityService.updateTenant(accessToken, customerATenant.getRealm(), customerATenant)

        then: "the tenants rule engine should stop and all asset rule engines from this realm should also stop"
        conditions.eventually {
            assert customerAEngine.isRunning() == false
            assert customerAEngine.allRulesDefinitions.length == 0
            assert rulesService.tenantDeployments.get(customerARealmId) == null
        }

        when: "a tenant is enabled"
        customerATenant.setEnabled(true)
        identityService.updateTenant(accessToken, customerATenant.getRealm(), customerATenant)

        then: "the tenants rule engine should start and all asset rule engines from this realm should also start"
        conditions.eventually {
            customerAEngine = rulesService.tenantDeployments.get(customerARealmId)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            assert customerAEngine.allRulesDefinitions.length == 2
            assert customerAEngine.allRulesDefinitions[0].enabled
            assert customerAEngine.allRulesDefinitions[0].name == "Some customerA tenant demo rules"
            assert customerAEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert customerAEngine.allRulesDefinitions[1].enabled
            assert customerAEngine.allRulesDefinitions[1].name == "Some more customerA tenant rules"
            assert customerAEngine.allRulesDefinitions[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Check asset update event fires the correct rules"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1)

    }
}
