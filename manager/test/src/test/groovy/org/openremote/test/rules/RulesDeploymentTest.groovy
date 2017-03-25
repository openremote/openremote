package org.openremote.test.rules

import elemental.json.Json
import org.apache.commons.io.IOUtils
import org.kie.api.event.rule.AfterMatchFiredEvent
import org.kie.api.event.rule.DefaultAgendaEventListener
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.rules.RulesDeployment
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesStorageService
import org.openremote.manager.server.security.ManagerIdentityService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.rules.AssetRulesDefinition
import org.openremote.manager.shared.rules.GlobalRulesDefinition
import org.openremote.manager.shared.rules.RulesDefinition.DeploymentStatus
import org.openremote.manager.shared.rules.TenantRulesDefinition
import org.openremote.model.AttributeEvent
import org.openremote.model.AttributeRef
import org.openremote.model.AttributeState
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class RulesDeploymentTest extends Specification implements ManagerContainerTrait {

    def "Check basic rules engine deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10)

        and: "the demo assets and rule definitions are deployed"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def rulesStorageService = container.getService(RulesStorageService.class)

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
            def masterEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.masterTenant.id)
            def customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert masterEngine != null
            assert masterEngine.isRunning()
            assert customerAEngine != null
            assert customerAEngine.isRunning()
        }

        and: "the tenant rules engines should have the demo tenant rules definition"
        conditions.eventually {
            def masterEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.masterTenant.id)
            def customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
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
        conditions = new PollingConditions(timeout: 10)
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
        conditions = new PollingConditions(timeout: 10)
        inputStream = getClass().getResourceAsStream("/org/openremote/test/rules/TenantRules.drl")
        rules = IOUtils.toString(inputStream, "UTF-8")
        rulesDefinition = new TenantRulesDefinition("Some more customerA tenant rules", keycloakDemoSetup.customerATenant.id, rules)
        rulesStorageService.merge(rulesDefinition)

        then: "customer A rules engine should load this definition and restart successfully"
        conditions.eventually {
            def customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
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
        conditions = new PollingConditions(timeout: 10)
        inputStream = getClass().getResourceAsStream("/org/openremote/test/rules/TenantRules.drl")
        rules = IOUtils.toString(inputStream, "UTF-8")
        rulesDefinition = new TenantRulesDefinition("Some more customerB tenant rules", keycloakDemoSetup.customerBTenant.id, rules)
        rulesStorageService.merge(rulesDefinition)

        then: "a tenant rules engine should be created for customer B and load this definition and start successfully"
        conditions.eventually {
            def customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
            assert rulesService.tenantDeployments.size() == 3
            assert customerBEngine != null
            assert customerBEngine.isRunning()
            assert customerBEngine.allRulesDefinitions.length == 1
            assert customerBEngine.allRulesDefinitions[0].enabled
            assert customerBEngine.allRulesDefinitions[0].name == "Some more customerB tenant rules"
            assert customerBEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the disabled rule definition for customer B is enabled"
        conditions = new PollingConditions(timeout: 10)
        rulesDefinition = rulesStorageService.findById(TenantRulesDefinition.class, managerDemoSetup.customerBRulesDefinitionId)
        rulesDefinition.setEnabled(true)
        rulesStorageService.merge(rulesDefinition)

        then: "customer B rule engine should load this definition and restart successfully"
        conditions.eventually {
            def customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
            assert rulesService.tenantDeployments.size() == 3
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
        conditions = new PollingConditions(timeout: 10)
        // TODO: Stop instances of rule definitions being passed around as rules deployment nulls the rules property
        rulesDefinition = rulesStorageService.findById(TenantRulesDefinition.class, managerDemoSetup.customerBRulesDefinitionId)
        rulesDefinition.setEnabled(false)
        rulesStorageService.merge(rulesDefinition)

        then: "customer B rule engine should should remove it again"
        conditions.eventually {
            def customerBEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerBTenant.id)
            assert customerBEngine != null
            assert customerBEngine.isRunning()
            assert customerBEngine.allRulesDefinitions.length == 1
            assert customerBEngine.allRulesDefinitions[0].enabled
            assert customerBEngine.allRulesDefinitions[0].name == "Some more customerB tenant rules"
            assert customerBEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

        when: "the asset rule definition for apartment 1 is deleted"
        conditions = new PollingConditions(timeout: 10)
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
        conditions = new PollingConditions(timeout: 10)
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
        conditions = new PollingConditions(timeout: 10)
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
        conditions = new PollingConditions(timeout: 10)
        def customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
        def apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
        def customerATenant = keycloakDemoSetup.customerATenant
        customerATenant.setEnabled(false)
        identityService.updateTenant(accessToken, customerATenant.getRealm(), customerATenant)

        then: "the tenants rule engine should stop and all asset rule engines in this realm should also stop"
        conditions.eventually {
            assert customerAEngine.isRunning() == false
            assert customerAEngine.allRulesDefinitions.length == 0
            assert rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id) == null
            assert apartment3Engine.isRunning() == false
            assert apartment3Engine.allRulesDefinitions.length == 0
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
        conditions = new PollingConditions(timeout: 10)
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
            assert customerAEngine.allRulesDefinitions.length == 2
            assert customerAEngine.allRulesDefinitions[0].enabled
            assert customerAEngine.allRulesDefinitions[0].name == "Some customerA tenant demo rules"
            assert customerAEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
            assert customerAEngine.allRulesDefinitions[1].enabled
            assert customerAEngine.allRulesDefinitions[1].name == "Some more customerA tenant rules"
            assert customerAEngine.allRulesDefinitions[1].deploymentStatus == DeploymentStatus.DEPLOYED
            assert apartment3Engine.allRulesDefinitions.length == 1
            assert apartment3Engine.allRulesDefinitions[0].enabled
            assert apartment3Engine.allRulesDefinitions[0].name == "Some apartment 3 demo rules"
            assert apartment3Engine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }

//TODO: Reinstate the tenant delete test once tenant delete mechanism is finalised
//        when: "a tenant is deleted"
//        identityService.deleteTenant(accessToken, customerATenant.getRealm())
//
//        then: "the tenants rule engine should stop and all asset rule engines in this realm should also stop"
//        conditions.eventually {
//            assert customerAEngine.isRunning() == false
//            assert customerAEngine.allRulesDefinitions.length == 0
//            assert rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id) == null
//            assert smartHomeEngine.isRunning() == false
//            assert smartHomeEngine.allRulesDefinitions.length == 0
//            assert rulesService.assetDeployments.get(managerDemoSetup.smartHomeId) == null
//            assert apartment3Engine.isRunning() == false
//            assert apartment3Engine.allRulesDefinitions.length == 0
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

    def "Check firing of rules"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10)

        //region and: "the demo assets and rule definitions are deployed"
        and: "the demo assets and rule definitions are deployed"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def rulesStorageService = container.getService(RulesStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        RulesDeployment globalEngine, masterEngine, customerAEngine, smartHomeEngine, apartment1Engine, apartment3Engine
        List<String> globalEngineFiredRules = []
        List<String> masterEngineFiredRules = []
        List<String> customerAEngineFiredRules = []
        List<String> smartHomeEngineFiredRules = []
        List<String> apartment1EngineFiredRules = []
        List<String> apartment3EngineFiredRules = []
        //endregion

        //region expect: "the rule engines to become available and be running"
        expect: "the rule engines to become available and be running"
        conditions.eventually {
            globalEngine = rulesService.globalDeployment
            assert globalEngine != null
            assert globalEngine.isRunning()
            masterEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.masterTenant.id)
            assert masterEngine != null
            assert masterEngine.isRunning()
            customerAEngine = rulesService.tenantDeployments.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            smartHomeEngine = rulesService.assetDeployments.get(managerDemoSetup.smartHomeId)
            assert smartHomeEngine == null
            apartment1Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment1Id)
            assert apartment1Engine != null
            assert apartment1Engine.isRunning()
            apartment3Engine = rulesService.assetDeployments.get(managerDemoSetup.apartment3Id)
            assert apartment3Engine != null
            assert apartment3Engine.isRunning()
        }
        //endregion

        //region when: "rule execution loggers are attached to the engines"
        when: "rule execution loggers are attached to the engines"
        attachRuleExecutionLogger(globalEngine, globalEngineFiredRules)
        attachRuleExecutionLogger(masterEngine, masterEngineFiredRules)
        attachRuleExecutionLogger(customerAEngine, customerAEngineFiredRules)
        attachRuleExecutionLogger(apartment1Engine, apartment1EngineFiredRules)
        attachRuleExecutionLogger(apartment3Engine, apartment3EngineFiredRules)
        //endregion

        //region and: "an attribute event is pushed into the system for an attribute with RULES_FACT meta set to true"
        and: "an attribute event is pushed into the system for an attribute with RULES_FACT meta set to true"
        def apartment1LivingRoomDemoBooleanChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomId, "demoBoolean"), Json.create(false)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoBooleanChange)
        //endregion

        //region then: "the rule engines in scope should fire the 'All' and 'All changed' rules"
        then: "the rule engines in scope should fire the 'All' and 'All changed' rules"
        conditions.eventually {
            def expectedFiredRules = ["All", "All changed"]
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(expectedFiredRules)
            assert masterEngineFiredRules.size() == 0
            assert customerAEngineFiredRules.size() == 2
            assert customerAEngineFiredRules.containsAll(expectedFiredRules)
            assert smartHomeEngineFiredRules.size() == 0
            assert apartment1EngineFiredRules.size() == 2
            assert apartment1EngineFiredRules.containsAll(expectedFiredRules)
            assert apartment3EngineFiredRules.size() == 0
        }
        //endregion

        //region when: "an attribute event is pushed into the system for an attribute with RULES_FACT meta set to false"
        when: "an attribute event is pushed into the system for an attribute with RULES_FACT meta set to false"
        def apartment1LivingRoomDemoStringChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomId, "demoString"), Json.create("demo2")), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoStringChange)
        //endregion

        //region then: "no rule engines should have fired after a few seconds"
        then: "no rule engines should have fired after a few seconds"
        new PollingConditions(delay: 3, timeout: 5).eventually {
            assert globalEngineFiredRules.size() == 2
            assert masterEngineFiredRules.size() == 0
            assert customerAEngineFiredRules.size() == 2
            assert smartHomeEngineFiredRules.size() == 0
            assert apartment1EngineFiredRules.size() == 2
            assert apartment3EngineFiredRules.size() == 0
        }
        //endregion

        //region when: "an attribute event is pushed into the system for an attribute with no RULES_FACT meta"
        when: "an attribute event is pushed into the system for an attribute with no RULES_FACT meta"
        def apartment1LivingRoomDemoIntegerChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomId, "demoInteger"), Json.create(1)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoIntegerChange)
        //endregion

        //region then: "no rule engines should have fired after a few seconds"
        then: "no rule engines should have fired after a few seconds"
        new PollingConditions(delay: 3, timeout: 5).eventually {
            assert globalEngineFiredRules.size() == 2
            assert masterEngineFiredRules.size() == 0
            assert customerAEngineFiredRules.size() == 2
            assert smartHomeEngineFiredRules.size() == 0
            assert apartment1EngineFiredRules.size() == 2
            assert apartment3EngineFiredRules.size() == 0
        }
        //endregion

        //region when: "an old (stale) attribute event is pushed into the system"
        when: "an old (stale) attribute event is pushed into the system"
        conditions = new PollingConditions(timeout: 10, initialDelay: 5)
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoBooleanChange)
        //endregion

        //region then: "no rule engines should have fired after a few seconds"
        then: "no rule engines should have fired after a few seconds"
        new PollingConditions(delay: 3, timeout: 5).eventually {
            assert globalEngineFiredRules.size() == 2
            assert masterEngineFiredRules.size() == 0
            assert customerAEngineFiredRules.size() == 2
            assert smartHomeEngineFiredRules.size() == 0
            assert apartment1EngineFiredRules.size() == 2
            assert apartment3EngineFiredRules.size() == 0
        }
        //endregion

        //region when: "the engine counters are reset"
        when: "the engine counters are reset"
        globalEngineFiredRules.clear();
        customerAEngineFiredRules.clear();
        smartHomeEngineFiredRules.clear();
        apartment1EngineFiredRules.clear();
        //endregion

        //region and: "an attribute event with the same value as current value is pushed into the system"
        and: "an attribute event with the same value as current value is pushed into the system"
        apartment1LivingRoomDemoBooleanChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomId, "demoBoolean"), Json.create(false)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomDemoBooleanChange)
        //endregion

        //region then: "the rule engines in scope should fire the 'All' rule but not the 'All changed' rule"
        then: "the rule engines in scope should fire the 'All' rule but not the 'All changed' rule"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 1
            assert globalEngineFiredRules[0] == "All"
            assert masterEngineFiredRules.size() == 0
            assert customerAEngineFiredRules.size() == 1
            assert customerAEngineFiredRules[0] == "All"
            assert smartHomeEngineFiredRules.size() == 0
            assert apartment1EngineFiredRules.size() == 1
            assert apartment1EngineFiredRules[0] == "All"
            assert apartment3EngineFiredRules.size() == 0
        }
        //endregion

        //region when: "a LHS filtering test rule definition is loaded into the smart home asset"
        when: "a LHS filtering test rule definition is loaded into the smart home asset"
        def inputStream = getClass().getResourceAsStream("/org/openremote/test/rules/TestLHSRules.drl")
        def rules = IOUtils.toString(inputStream, "UTF-8")
        def smartHomeRulesDefinition = new AssetRulesDefinition("Some smart home asset rules", managerDemoSetup.smartHomeId, rules)
        rulesStorageService.merge(smartHomeRulesDefinition)
        //endregion

        //region then: "the smart home rule engine should have been created, loaded the new rule definition and started"
        then: "the smart home rule engine should have ben created, loaded the new rule definition and started"
        conditions.eventually {
            smartHomeEngine = rulesService.assetDeployments.get(managerDemoSetup.smartHomeId)
            assert smartHomeEngine != null
            assert smartHomeEngine.isRunning()
            assert smartHomeEngine.allRulesDefinitions.length == 1
            assert smartHomeEngine.allRulesDefinitions[0].enabled
            assert smartHomeEngine.allRulesDefinitions[0].name == "Some smart home asset rules"
            assert smartHomeEngine.allRulesDefinitions[0].deploymentStatus == DeploymentStatus.DEPLOYED
        }
        //endregion

        //region when: "the engine counters are reset and the smart home engine logger is attached"
        when: "the engine counters are reset and the smart home engine logger is attached"
        globalEngineFiredRules.clear();
        customerAEngineFiredRules.clear();
        smartHomeEngineFiredRules.clear();
        apartment1EngineFiredRules.clear();
        attachRuleExecutionLogger(smartHomeEngine, smartHomeEngineFiredRules)
        //endregion

        //region and: "an apartment 3 living room attribute event occurs"
        and: "an apartment 3 living room attribute event occurs"
        def apartment3LivingRoomDemoStringChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment3LivingroomId, "demoString"), Json.create("demo2")), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment3LivingRoomDemoStringChange)
        //endregion

        //region then: "the engines in scope should have fired the matched rules"
        then: "the engines in scope should have fired the matched rules"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(["All", "All changed"])
            assert customerAEngineFiredRules.size() == 2
            assert customerAEngineFiredRules.containsAll(["All", "All changed"])
            assert smartHomeEngineFiredRules.size() == 5
            assert smartHomeEngineFiredRules.containsAll(["Living Room All", "Current Asset Update", "Parent Type Residence", "Asset Type Room", "String Attributes"])
            assert apartment3EngineFiredRules.size() == 2
            assert apartment3EngineFiredRules.containsAll(["All", "All changed"])
            assert apartment1EngineFiredRules.size() == 0
        }
        //endregion

        //region when: "the rule counters are reset"
        when: "the rule counters are reset"
        globalEngineFiredRules.clear();
        customerAEngineFiredRules.clear();
        smartHomeEngineFiredRules.clear();
        apartment1EngineFiredRules.clear();
        apartment3EngineFiredRules.clear();
        //endregion

        //region and: "an apartment 1 living room thermostat attribute event occurs"
        and: "an apartment 1 living room thermostat attribute event occurs"
        def apartment1LivingRoomTargetTempChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomThermostatId, "targetTemperature"), Json.create(22.5)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomTargetTempChange)
        //endregion

        //region then: "the engines in scope should have fired the matched rules"
        then: "the engines in scope should have fired the matched rules"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(["All", "All changed"])
            assert customerAEngineFiredRules.size() == 2
            assert customerAEngineFiredRules.containsAll(["All", "All changed"])
            assert smartHomeEngineFiredRules.size() == 5
            assert smartHomeEngineFiredRules.containsAll(
                    [
                            "Living Room Thermostat",
                            "Living Room Target Temp",
                            "Living Room as Parent",
                            "JSON Number value types",
                            "Current Asset Update"
                    ])
            assert apartment1EngineFiredRules.size() == 2
            assert apartment1EngineFiredRules.containsAll(["All", "All changed"])
            assert apartment3EngineFiredRules.size() == 0
        }
        //endregion

        //region when: "a RHS filtering test rule definition is loaded into the global rule engine"
        when: "a RHS filtering test rule definition is loaded into the global rule engine"
        inputStream = getClass().getResourceAsStream("/org/openremote/test/rules/TestRHSRules.drl")
        rules = IOUtils.toString(inputStream, "UTF-8")
        def globalRuleDefinition = new GlobalRulesDefinition("Some global test rules", rules)
        rulesStorageService.merge(globalRuleDefinition)
        //endregion

        //region then: "the global rule engine should have loaded the new rule definition and restarted"
        then: "the global rule engine should have loaded the new rule definition and restarted"
        conditions.eventually {
            globalEngine = rulesService.globalDeployment
            assert globalEngine != null
            assert globalEngine.isRunning()
            assert globalEngine.allRulesDefinitions.length == 2
            assert globalEngine.allRulesDefinitions[1].enabled
            assert globalEngine.allRulesDefinitions[1].name == "Some global test rules"
            assert globalEngine.allRulesDefinitions[1].deploymentStatus == DeploymentStatus.DEPLOYED
        }
        //endregion

        //region when: "the engine counters are reset and the global engine logger is reattached"
        when: "the engine counters are reset and the global engine logger is reattached"
        globalEngineFiredRules.clear();
        customerAEngineFiredRules.clear();
        smartHomeEngineFiredRules.clear();
        apartment1EngineFiredRules.clear();
        attachRuleExecutionLogger(globalEngine, globalEngineFiredRules)
        //endregion

        //region and: "an apartment 1 living room thermostat attribute event occurs"
        and: "an apartment 1 living room thermostat attribute event occurs"
        apartment1LivingRoomTargetTempChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment1LivingroomThermostatId, "targetTemperature"), Json.create(22.5)), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment1LivingRoomTargetTempChange)
        //endregion

        //region then: "only the global engine should have fired the Prevent Livingroom Thermostat Change rule"
        then: "after a few seconds only the global engine should have fired the All, All changed and Prevent Livingroom Thermostat Change rules"
        new PollingConditions(initialDelay: 5, timeout: 10).eventually {
            assert globalEngineFiredRules.size() == 3
            assert globalEngineFiredRules.containsAll(["All", "All changed", "Prevent Livingroom Thermostat Change"])
            assert customerAEngineFiredRules.size() == 0
            assert smartHomeEngineFiredRules.size() == 0
            assert apartment1EngineFiredRules.size() == 0
            assert apartment3EngineFiredRules.size() == 0
        }
        //endregion

        //region when: "the engine counters are reset"
        when: "the engine counters are reset and the global engine logger is reattached"
        globalEngineFiredRules.clear();
        customerAEngineFiredRules.clear();
        smartHomeEngineFiredRules.clear();
        apartment1EngineFiredRules.clear();
        //endregion

        //region and: "an apartment 3 living room attribute event occurs"
        and: "an apartment 3 living room attribute event occurs"
        apartment3LivingRoomDemoStringChange = new AttributeEvent(
                new AttributeState(new AttributeRef(managerDemoSetup.apartment3LivingroomId, "demoString"), Json.create("demo3")), getClass()
        )
        assetProcessingService.updateAttributeValue(apartment3LivingRoomDemoStringChange)
        //endregion

        //region then: "all the engines in scope should have fired the matched rules"
        then: "all the engines in scope should have fired the matched rules"
        conditions.eventually {
            assert globalEngineFiredRules.size() == 2
            assert globalEngineFiredRules.containsAll(["All", "All changed"])
            assert customerAEngineFiredRules.size() == 2
            assert customerAEngineFiredRules.containsAll(["All", "All changed"])
            assert smartHomeEngineFiredRules.size() == 5
            assert smartHomeEngineFiredRules.containsAll(["Living Room All", "Current Asset Update", "Parent Type Residence", "Asset Type Room", "String Attributes"])
            assert apartment3EngineFiredRules.size() == 2
            assert apartment3EngineFiredRules.containsAll(["All", "All changed"])
            assert apartment1EngineFiredRules.size() == 0
        }
        //endregion
    }

    def attachRuleExecutionLogger(RulesDeployment ruleEngine, List<String> executedRules) {
        def session = ruleEngine.getKnowledgeSession()
        if (session == null) {
            return
        }

        session.addEventListener(new DefaultAgendaEventListener() {
            @Override
            void afterMatchFired(AfterMatchFiredEvent event) {
                def rule = event.getMatch().getRule()
                def ruleName = rule.getName()
                executedRules.add(ruleName)
            }
        })
    }
}
