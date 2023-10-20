package org.openremote.test.rules

import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.RealmRuleset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.rules.Ruleset.Lang.GROOVY
import static org.openremote.model.rules.RulesetStatus.*

class BasicRulesDeploymentTest extends Specification implements ManagerContainerTrait {

    @SuppressWarnings("GroovyAccessibility")
    def "Check basic rules engine deployment"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def rulesService = container.getService(RulesService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakTestSetup, managerTestSetup)

        and: "an authenticated user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        expect: "the rules engines to be ready"
        conditions.eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakTestSetup, managerTestSetup)
        }

        when: "a new global rule definition is added"
        def ruleset = new GlobalRuleset(
                "Some more global rules", GROOVY,
                getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates2.groovy").text
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

        when: "a new realm rule definition is added to Building"
        ruleset = new RealmRuleset(
            keycloakTestSetup.realmBuilding.name,
            "Some more building realm rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates2.groovy").text)
        rulesetStorageService.merge(ruleset)

        then: "Building rules engine should load this definition and restart successfully"
        conditions.eventually {
            def realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            assert realmBuildingEngine != null
            assert realmBuildingEngine.isRunning()
            assert realmBuildingEngine.deployments.size() == 2
            assert realmBuildingEngine.deployments.values().any({ it.name == "Some building realm demo rules" && it.status == DEPLOYED})
            assert realmBuildingEngine.deployments.values().any({ it.name == "Some more building realm rules" && it.status == DEPLOYED})
        }

        when: "a new realm rule definition is added to City"
        ruleset = new RealmRuleset(
            keycloakTestSetup.realmCity.name,
            "Some more smartcity realm rules",
            GROOVY,
            getClass().getResource("/org/openremote/test/rules/BasicMatchAllAssetStates2.groovy").text)
        rulesetStorageService.merge(ruleset)

        then: "a realm rules engine should be created for City and load this definition and start successfully"
        conditions.eventually {
            def realmCity = rulesService.realmEngines.get(keycloakTestSetup.realmCity.name)
            assert rulesService.realmEngines.size() == 3
            assert realmCity != null
            assert realmCity.isRunning()
            assert realmCity.deployments.size() == 1
            assert realmCity.deployments.values().any({ it.name == "Some more smartcity realm rules" && it.status == DEPLOYED})
        }

        when: "the disabled rule definition for Realm B is enabled"
        ruleset = rulesetStorageService.find(RealmRuleset.class, rulesImport.realmCityRulesetId)
        ruleset.setEnabled(true)
        rulesetStorageService.merge(ruleset)

        then: "City rule engine should load this definition and restart successfully"
        conditions.eventually {
            def realmCity = rulesService.realmEngines.get(keycloakTestSetup.realmCity.name)
            assert rulesService.realmEngines.size() == 3
            assert realmCity != null
            assert realmCity.isRunning()
            assert realmCity.deployments.size() == 2
            assert realmCity.deployments.values().any({ it.name == "Some more smartcity realm rules" && it.status == DEPLOYED})
            assert realmCity.deployments.values().any({ it.name == "Some smartcity realm demo rules" && it.status == DEPLOYED})
        }

        when: "the enabled rule definition for City is disabled"
        // TODO: Stop instances of rule definitions being passed around as rules engine nulls the rules property
        ruleset = rulesetStorageService.find(RealmRuleset.class, rulesImport.realmCityRulesetId)
        ruleset.setEnabled(false)
        rulesetStorageService.merge(ruleset)

        then: "City rule engine should remove it again"
        conditions.eventually {
            def realmCity = rulesService.realmEngines.get(keycloakTestSetup.realmCity.name)
            assert realmCity != null
            assert realmCity.isRunning()
            assert realmCity.deployments.size() == 1
            assert realmCity.deployments.values().any({ it.name == "Some more smartcity realm rules" && it.status == DEPLOYED})
        }

        when: "the asset rule definition for apartment 2 is deleted"
        rulesetStorageService.delete(AssetRuleset.class, rulesImport.apartment2RulesetId)

        then: "the apartment rules engine should be removed"
        conditions.eventually {
            assert rulesService.assetEngines.size() == 1
            def apartment2Engine = rulesService.assetEngines.get(managerTestSetup.apartment2Id)
            def apartment3Engine = rulesService.assetEngines.get(managerTestSetup.apartment3Id)
            assert apartment2Engine == null
            assert apartment3Engine != null
            assert apartment3Engine.isRunning()
        }

        when: "a broken rule definition is added to the global rules engine"
        ruleset = new GlobalRuleset(
                "Some broken global rules", GROOVY,
                getClass().getResource("/org/openremote/test/rules/BasicBrokenRules.groovy").text
        )
        ruleset = rulesetStorageService.merge(ruleset)

        then: "the global rules engine should still run and the rule engine status should indicate the issue"
        conditions.eventually {
            assert rulesService.globalEngine.deployments.size() == 3
            assert rulesService.globalEngine.running
            assert !rulesService.globalEngine.isError()
            assert rulesService.globalEngine.error instanceof RuntimeException
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some global demo rules" && it.status == DEPLOYED})
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some more global rules" && it.status == DEPLOYED})
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some broken global rules" && it.status == COMPILATION_ERROR})
        }

        when: "the broken rule definition is removed from the global engine"
        rulesetStorageService.delete(GlobalRuleset.class, ruleset.getId())

        then: "the global rules engine should restart"
        conditions.eventually {
            assert rulesService.globalEngine.deployments.size() == 2
            assert rulesService.globalEngine.running
            assert !rulesService.globalEngine.isError()
            assert rulesService.globalEngine.error == null
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some global demo rules" && it.status == DEPLOYED })
            assert rulesService.globalEngine.deployments.values().any({ it.name == "Some more global rules" && it.status == DEPLOYED })
        }

        when: "a realm is disabled"
        RulesEngine realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
        RulesEngine apartment3Engine = rulesService.assetEngines.get(managerTestSetup.apartment3Id)
        def realmBuilding = keycloakTestSetup.realmBuilding
        realmBuilding.setEnabled(false)
        identityService.getIdentityProvider().updateRealm(realmBuilding)

        then: "the realms rule engine should stop and all asset rule engines in this realm should also stop"
        conditions.eventually {
            assert !realmBuildingEngine.isRunning()
            assert rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name) == null
            assert !apartment3Engine.isRunning()
            assert rulesService.assetEngines.get(managerTestSetup.apartment3Id) == null
        }

        and: "other rule engines should be unaffected"
        conditions.eventually {
            assert rulesService.realmEngines.size() == 2
            assert rulesService.assetEngines.size() == 0
            def masterEngine = rulesService.realmEngines.get(keycloakTestSetup.realmMaster.name)
            def cityEngine = rulesService.realmEngines.get(keycloakTestSetup.realmCity.name)
            assert masterEngine != null
            assert masterEngine.isRunning()
            assert cityEngine != null
            assert cityEngine.isRunning()
        }

        when: "the disabled realm is re-enabled"
        realmBuilding.setEnabled(true)
        identityService.getIdentityProvider().updateRealm(realmBuilding)

        then: "the realms rule engine should start and all asset rule engines from this realm should also start"
        conditions.eventually {
            realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            apartment3Engine = rulesService.assetEngines.get(managerTestSetup.apartment3Id)
            assert rulesService.realmEngines.size() == 3
            assert rulesService.assetEngines.size() == 1
            assert realmBuildingEngine != null
            assert realmBuildingEngine.isRunning()
            assert realmBuildingEngine.deployments.size() == 2
            assert realmBuildingEngine.deployments.values().any({ it.name == "Some building realm demo rules" && it.status == DEPLOYED})
            assert realmBuildingEngine.deployments.values().any({ it.name == "Some more building realm rules" && it.status == DEPLOYED})
            assert apartment3Engine.deployments.size() == 1
            assert apartment3Engine.deployments.values().any({ it.name == "Some apartment 3 demo rules" && it.status == DEPLOYED})
        }

        when: "a new realm rule definition is added to Building"
        ruleset = new RealmRuleset(
            keycloakTestSetup.realmBuilding.name,
            "Throw Failure Exception",
            GROOVY,
            getClass().getResource("/org/openremote/test/failure/RulesFailureActionThrowsException.groovy").text)
            .setContinueOnError(true)
        ruleset = rulesetStorageService.merge(ruleset)

        then: "the realms A rule engine should run with one deployment as error"
        conditions.eventually {
            realmBuildingEngine = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name)
            assert realmBuildingEngine != null
            assert realmBuildingEngine.isRunning()
            assert realmBuildingEngine.deployments.size() == 3
            assert realmBuildingEngine.deployments[ruleset.id].status == DEPLOYED
            assert realmBuildingEngine.deployments[ruleset.id].error != null
            assert !realmBuildingEngine.isError()
        }

        when: "a realm is deleted"
        identityService.getIdentityProvider().deleteRealm(realmBuilding.getName())

        then: "the realms rule engine should stop and all asset rule engines in this realm should also stop"
        conditions.eventually {
            assert rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name) == null
            assert !realmBuildingEngine.isRunning()
            assert rulesService.assetEngines.get(managerTestSetup.apartment3Id) == null
            assert !apartment3Engine.isRunning()
        }

        and: "other rule engines should be unaffected"
        conditions.eventually {
            assert rulesService.realmEngines.size() == 2
            assert rulesService.assetEngines.size() == 0
            def masterEngine = rulesService.realmEngines.get(MASTER_REALM)
            def realmCity = rulesService.realmEngines.get(keycloakTestSetup.realmCity.name)
            assert masterEngine != null
            assert masterEngine.isRunning()
            assert realmCity != null
            assert realmCity.isRunning()
        }
    }
}
