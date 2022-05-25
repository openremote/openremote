package org.openremote.test.rules

import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.attribute.MetaItem
import org.openremote.model.rules.*
import org.openremote.model.value.MetaItemType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.rules.Ruleset.Lang.GROOVY
import static org.openremote.model.rules.Ruleset.SHOW_ON_LIST

class BasicRulesetResourceTest extends Specification implements ManagerContainerTrait {

    def "Access rules as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def expirationMillis = TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = 500
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def rulesService = container.getService(RulesService.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakTestSetup, managerTestSetup)

        and: "an authenticated admin user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the ruleset resource"
        def rulesetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(RulesResource.class)

        expect: "the rules engines to be ready"
        conditions.eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakTestSetup, managerTestSetup)
        }

        /* ############################################## READ ####################################### */

        when: "the global rules are retrieved"
        def ruleDefinitions = rulesetResource.getGlobalRulesets(null, null, false)

        then: "result should match"
        ruleDefinitions.length == 2
        ruleDefinitions[0].name == "Some global demo rules"
        ruleDefinitions[0].lang == GROOVY
        ruleDefinitions[0].rules == null // Don't retrieve the (large) rules data when getting a list of rule definitions

        when: "some realm rules are retrieved"
        ruleDefinitions = rulesetResource.getRealmRulesets(null, keycloakTestSetup.realmMaster.name, null, false)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some master realm demo rules"
        ruleDefinitions[0].lang == GROOVY

        when: "some realm rules in a non-authenticated realm are retrieved"
        ruleDefinitions = rulesetResource.getRealmRulesets(null, keycloakTestSetup.realmBuilding.name, null, false)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some building realm demo rules"
        ruleDefinitions[0].enabled

        when: "some asset rules are retrieved"
        ruleDefinitions = rulesetResource.getAssetRulesets(null, managerTestSetup.apartment2Id, null, false)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some apartment 2 demo rules"
        ruleDefinitions[0].lang == GROOVY
        ruleDefinitions[0].meta.getValue(SHOW_ON_LIST).orElse(false)

        /* ############################################## WRITE ####################################### */

        when: "global ruleset is created"
        def globalRuleset = new GlobalRuleset("Test global definition", GROOVY, "SomeRulesCode")
        rulesetResource.createGlobalRuleset(null, globalRuleset)
        def rulesetId = rulesetResource.getGlobalRulesets(null, null, false)[2].id
        globalRuleset = rulesetResource.getGlobalRuleset(null, rulesetId)
        def lastModified = globalRuleset.lastModified

        then: "result should match"
        globalRuleset.id == rulesetId
        globalRuleset.version == 0
        globalRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        globalRuleset.name == "Test global definition"
        globalRuleset.rules == "SomeRulesCode"
        globalRuleset.lang == GROOVY

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.globalEngine.deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == globalRuleset.version
        }

        when: "a global ruleset is updated"
        globalRuleset.name = "Renamed test global definition"
        globalRuleset.rules = "SomeRulesCodeModified"
        rulesetResource.updateGlobalRuleset(null, rulesetId, globalRuleset)
        globalRuleset = rulesetResource.getGlobalRuleset(null, rulesetId)

        then: "result should match"
        globalRuleset.id == rulesetId
        globalRuleset.version > 0
        globalRuleset.createdOn.time < System.currentTimeMillis()
        globalRuleset.lastModified.time >= lastModified.time
        globalRuleset.name == "Renamed test global definition"
        globalRuleset.rules == "SomeRulesCodeModified"

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.globalEngine.deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == globalRuleset.version
        }

        when: "a global ruleset is deleted"
        rulesetResource.deleteGlobalRuleset(null, rulesetId)
        globalRuleset = rulesetResource.getGlobalRuleset(null, rulesetId)

        then: "the result should be not found"
        WebApplicationException ex = thrown()
        ex.response.status == 404

        and: "the ruleset should be removed from the engine"
        conditions.eventually {
            def deployment = rulesService.globalEngine.deployments.get(rulesetId)
            assert deployment == null
        }

        when: "a non-existent global ruleset is updated"
        rulesetResource.updateGlobalRuleset(null, 1234567890l, globalRuleset)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a realm ruleset is created in the authenticated realm"
        def realmRuleset = new RealmRuleset(MASTER_REALM, "Test realm definition", GROOVY, "SomeRulesCode")
        rulesetResource.createRealmRuleset(null, realmRuleset)
        rulesetId = rulesetResource.getRealmRulesets(null, MASTER_REALM, null, false)[1].id
        realmRuleset = rulesetResource.getRealmRuleset(null, rulesetId)
        lastModified = realmRuleset.lastModified

        then: "result should match"
        realmRuleset.id == rulesetId
        realmRuleset.version == 0
        realmRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        realmRuleset.name == "Test realm definition"
        realmRuleset.rules == "SomeRulesCode"
        realmRuleset.realm == keycloakTestSetup.realmMaster.name

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.realmEngines.get(MASTER_REALM).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == realmRuleset.version
        }

        when: "a realm ruleset is updated"
        realmRuleset.name = "Renamed test realm definition"
        realmRuleset.rules = "SomeRulesCodeModified"
        rulesetResource.updateRealmRuleset(null, rulesetId, realmRuleset)
        realmRuleset = rulesetResource.getRealmRuleset(null, rulesetId)

        then: "result should match"
        realmRuleset.id == rulesetId
        realmRuleset.version == 1
        realmRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        realmRuleset.name == "Renamed test realm definition"
        realmRuleset.rules == "SomeRulesCodeModified"
        realmRuleset.realm == keycloakTestSetup.realmMaster.name

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.realmEngines.get(MASTER_REALM).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == realmRuleset.version
        }

        when: "a realm ruleset is updated with an invalid realm"
        realmRuleset.realm = "thisdoesnotexist"
        rulesetResource.updateRealmRuleset(null, rulesetId, realmRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a realm ruleset is updated with an invalid id"
        realmRuleset.realm = MASTER_REALM
        realmRuleset.id = 1234567890l
        rulesetResource.updateRealmRuleset(null, rulesetId, realmRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent realm ruleset is updated"
        rulesetResource.updateRealmRuleset(null, 1234567890l, realmRuleset)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a realm ruleset is deleted"
        rulesetResource.deleteRealmRuleset(null, rulesetId)
        rulesetResource.getRealmRuleset(null, rulesetId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        and: "the ruleset should be removed from the engine"
        conditions.eventually {
            def deployment = rulesService.realmEngines.get(MASTER_REALM).deployments.get(rulesetId)
            assert deployment == null
        }

        when: "a realm ruleset is created in a non-authenticated realm"
        realmRuleset = new RealmRuleset(keycloakTestSetup.realmBuilding.name, "Test realm definition", GROOVY, "SomeRulesCode")
        rulesetResource.createRealmRuleset(null, realmRuleset)
        rulesetId = rulesetResource.getRealmRulesets(null, keycloakTestSetup.realmBuilding.name, null, false)[1].id
        realmRuleset = rulesetResource.getRealmRuleset(null, rulesetId)
        lastModified = realmRuleset.lastModified

        then: "result should match"
        realmRuleset.id == rulesetId
        realmRuleset.version == 0
        realmRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        realmRuleset.name == "Test realm definition"
        realmRuleset.rules == "SomeRulesCode"
        realmRuleset.realm == keycloakTestSetup.realmBuilding.name

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.realmEngines.get(keycloakTestSetup.realmBuilding.name).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == realmRuleset.version
        }

        when: "an asset ruleset is created in the authenticated realm"
        def assetRuleset = new AssetRuleset(managerTestSetup.smartOfficeId, "Test asset definition", GROOVY, "SomeRulesCode")
        rulesetResource.createAssetRuleset(null, assetRuleset)
        rulesetId = rulesetResource.getAssetRulesets(null, managerTestSetup.smartOfficeId, null, false)[0].id
        assetRuleset = rulesetResource.getAssetRuleset(null, rulesetId)
        lastModified = assetRuleset.lastModified

        then: "result should match"
        assetRuleset.id == rulesetId
        assetRuleset.version == 0
        assetRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRuleset.name == "Test asset definition"
        assetRuleset.rules == "SomeRulesCode"
        assetRuleset.assetId == managerTestSetup.smartOfficeId

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.assetEngines.get(managerTestSetup.smartOfficeId).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == assetRuleset.version
        }

        when: "an asset ruleset is updated"
        assetRuleset.name = "Renamed test asset definition"
        assetRuleset.rules = "SomeRulesCodeModified"
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)
        assetRuleset = rulesetResource.getAssetRuleset(null, rulesetId)

        then: "result should match"
        assetRuleset.id == rulesetId
        assetRuleset.version == 1
        assetRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRuleset.name == "Renamed test asset definition"
        assetRuleset.rules == "SomeRulesCodeModified"
        assetRuleset.assetId == managerTestSetup.smartOfficeId

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.assetEngines.get(managerTestSetup.smartOfficeId).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == assetRuleset.version
        }

        when: "an asset ruleset is updated with an invalid asset ID"
        assetRuleset.assetId = "thisdoesnotexist"
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset ruleset is updated with an invalid id"
        assetRuleset.assetId = managerTestSetup.smartOfficeId
        assetRuleset.id = 1234567890l
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent asset ruleset is updated"
        rulesetResource.updateAssetRuleset(null, 1234567890l, assetRuleset)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset ruleset is deleted"
        rulesetResource.deleteAssetRuleset(null, rulesetId)
        rulesetResource.getAssetRuleset(null, rulesetId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        and: "the ruleset should be removed from the engine"
        conditions.eventually {
            assert rulesService.assetEngines.get(managerTestSetup.smartOfficeId) == null
        }

        when: "an asset ruleset is created in a non-authenticated realm"
        assetRuleset = new AssetRuleset(managerTestSetup.apartment2Id, "Test asset definition", GROOVY, "SomeRulesCode")
        rulesetResource.createAssetRuleset(null, assetRuleset)
        rulesetId = rulesetResource.getAssetRulesets(null, managerTestSetup.apartment2Id, null, false)[1].id
        assetRuleset = rulesetResource.getAssetRuleset(null, rulesetId)
        lastModified = assetRuleset.lastModified

        then: "result should match"
        assetRuleset.id == rulesetId
        assetRuleset.version == 0
        assetRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRuleset.name == "Test asset definition"
        assetRuleset.rules == "SomeRulesCode"
        assetRuleset.assetId == managerTestSetup.apartment2Id

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.assetEngines.get(managerTestSetup.apartment2Id).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == assetRuleset.version
        }

        cleanup: "the static rules time variable is reset"
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = expirationMillis
    }

    def "Access rules as testuser1"() {

        given: "the server container is started"
        def expirationMillis = TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = 500
        def container = startContainer(defaultConfig(), defaultServices())
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def rulesService = container.getService(RulesService.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "some imported rulesets"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakTestSetup, managerTestSetup)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        and: "the ruleset resource"
        def rulesetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(RulesResource.class)

        expect: "the rules engines to be ready"
        conditions.eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakTestSetup, managerTestSetup)
        }

        /* ############################################## READ ####################################### */

        when: "the global rules are retrieved"
        rulesetResource.getGlobalRulesets(null, null, false)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "some realm rules are retrieved"
        def ruleDefinitions = rulesetResource.getRealmRulesets(null, keycloakTestSetup.realmMaster.name, null, false)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some master realm demo rules"

        when: "some realm rules in a non-authenticated realm are retrieved"
        rulesetResource.getRealmRulesets(null, keycloakTestSetup.realmBuilding.name, null, false)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "some asset rules in a non-authenticated realm are retrieved"
        rulesetResource.getAssetRulesets(null, managerTestSetup.apartment2Id, null, false)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "a global rule is created"
        def ruleDefinition = new GlobalRuleset("Test definition", GROOVY, "SomeRulesCode")
        rulesetResource.createGlobalRuleset(null, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is updated"
        rulesetResource.updateGlobalRuleset(null, 1234567890l, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is deleted"
        rulesetResource.deleteGlobalRuleset(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is created in the authenticated realm"
        def realmRuleset = new RealmRuleset(MASTER_REALM, "Test realm definition", GROOVY, "SomeRulesCode")
        rulesetResource.createRealmRuleset(null, realmRuleset)
        def rulesetId = rulesetResource.getRealmRulesets(null, MASTER_REALM, null, false)[1].id
        realmRuleset = rulesetResource.getRealmRuleset(null, rulesetId)
        def lastModified = realmRuleset.lastModified

        then: "result should match"
        realmRuleset.id == rulesetId
        realmRuleset.version == 0
        realmRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        realmRuleset.name == "Test realm definition"
        realmRuleset.rules == "SomeRulesCode"
        realmRuleset.realm == MASTER_REALM

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.realmEngines.get(MASTER_REALM).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == realmRuleset.version
        }

        when: "a realm ruleset is updated"
        realmRuleset.name = "Renamed test realm definition"
        realmRuleset.rules = "SomeRulesCodeModified"
        rulesetResource.updateRealmRuleset(null, rulesetId, realmRuleset)
        realmRuleset = rulesetResource.getRealmRuleset(null, rulesetId)

        then: "result should match"
        realmRuleset.id == rulesetId
        realmRuleset.version == 1
        realmRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        realmRuleset.name == "Renamed test realm definition"
        realmRuleset.rules == "SomeRulesCodeModified"
        realmRuleset.realm == keycloakTestSetup.realmMaster.name

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.realmEngines.get(MASTER_REALM).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == realmRuleset.version
        }

        when: "a realm ruleset is updated with an invalid realm"
        realmRuleset.realm = "thisdoesnotexist"
        rulesetResource.updateRealmRuleset(null, rulesetId, realmRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a realm ruleset is updated with an invalid id"
        realmRuleset.realm = keycloakTestSetup.realmMaster.name
        realmRuleset.id = 1234567890l
        rulesetResource.updateRealmRuleset(null, rulesetId, realmRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent realm ruleset is updated"
        rulesetResource.updateRealmRuleset(null, 1234567890l, realmRuleset)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a realm ruleset is deleted"
        rulesetResource.deleteRealmRuleset(null, rulesetId)
        rulesetResource.getRealmRuleset(null, rulesetId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        and: "the ruleset should be removed from the engine"
        conditions.eventually {
            def deployment = rulesService.realmEngines.get(MASTER_REALM).deployments.get(rulesetId)
            assert deployment == null
        }

        when: "a realm ruleset is created in a non-authenticated realm"
        realmRuleset = new RealmRuleset(keycloakTestSetup.realmBuilding.name, "Test realm definition", GROOVY, "SomeRulesCode")
        rulesetResource.createRealmRuleset(null, realmRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset ruleset is created in the authenticated realm"
        def assetRuleset = new AssetRuleset(managerTestSetup.smartOfficeId, "Test asset definition", GROOVY, "SomeRulesCode")
        rulesetResource.createAssetRuleset(null, assetRuleset)
        rulesetId = rulesetResource.getAssetRulesets(null, managerTestSetup.smartOfficeId, null, false)[0].id
        assetRuleset = rulesetResource.getAssetRuleset(null, rulesetId)
        lastModified = assetRuleset.lastModified

        then: "result should match"
        assetRuleset.id == rulesetId
        assetRuleset.version == 0
        assetRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRuleset.name == "Test asset definition"
        assetRuleset.rules == "SomeRulesCode"
        assetRuleset.assetId == managerTestSetup.smartOfficeId

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.assetEngines.get(managerTestSetup.smartOfficeId).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == assetRuleset.version
        }

        when: "an asset ruleset is updated"
        assetRuleset.name = "Renamed test asset definition"
        assetRuleset.rules = "SomeRulesCodeModified"
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)
        assetRuleset = rulesetResource.getAssetRuleset(null, rulesetId)

        then: "result should match"
        assetRuleset.id == rulesetId
        assetRuleset.version == 1
        assetRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRuleset.name == "Renamed test asset definition"
        assetRuleset.rules == "SomeRulesCodeModified"
        assetRuleset.assetId == managerTestSetup.smartOfficeId

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.assetEngines.get(managerTestSetup.smartOfficeId).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == assetRuleset.version
        }

        when: "an asset ruleset is updated with an invalid asset ID"
        assetRuleset.assetId = "thisdoesnotexist"
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset ruleset is updated with an invalid id"
        assetRuleset.assetId = managerTestSetup.smartOfficeId
        assetRuleset.id = 1234567890l
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent asset ruleset is updated"
        rulesetResource.updateAssetRuleset(null, 1234567890l, assetRuleset)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset ruleset is deleted"
        rulesetResource.deleteAssetRuleset(null, rulesetId)
        rulesetResource.getAssetRuleset(null, rulesetId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        and: "the ruleset should reach the engine"
        conditions.eventually {
            assert rulesService.assetEngines.get(managerTestSetup.smartOfficeId) == null
        }

        when: "an asset ruleset is created in a non-authenticated realm"
        assetRuleset = new AssetRuleset(managerTestSetup.apartment2Id, "Test asset definition", GROOVY, "SomeRulesCode")
        rulesetResource.createAssetRuleset(null, assetRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the static rules time variable is reset"
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = expirationMillis
    }

    def "Access rules as testuser2"() {

        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def expirationMillis = TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = 500
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def rulesService = container.getService(RulesService.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "some imported rulesets"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakTestSetup, managerTestSetup)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                "testuser2",
                "testuser2"
        ).token

        and: "the ruleset resource"
        def rulesetResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name, accessToken).proxy(RulesResource.class)

        expect: "the rules engines to be ready"
        conditions.eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakTestSetup, managerTestSetup)
        }

        /* ############################################## READ ####################################### */

        when: "the global rules are retrieved"
        rulesetResource.getGlobalRulesets(null, null, false)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "some realm rules in a non-authenticated realm are retrieved"
        rulesetResource.getRealmRulesets(null, keycloakTestSetup.realmMaster.name, null, false)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "some realm rules in the authenticated realm are retrieved by user without rules read role"
        def rulesets = rulesetResource.getRealmRulesets(null, keycloakTestSetup.realmBuilding.name, null, false)

        then: "no rulesets should be returned"
        rulesets.length == 0

        when: "some asset rules in the authenticated realm are retrieved by the user without rules read role"
        rulesets = rulesetResource.getAssetRulesets(null, managerTestSetup.apartment2Id, null, false)

        then: "no rulesets should be returned"
        rulesets.length == 0

        /* ############################################## WRITE ####################################### */

        when: "a global rule is created"
        def ruleDefinition = new GlobalRuleset("Test definition", GROOVY, "SomeRulesCode")
        rulesetResource.createGlobalRuleset(null, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is updated"
        rulesetResource.updateGlobalRuleset(null, 1234567890l, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is deleted"
        rulesetResource.deleteGlobalRuleset(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is created in the authenticated realm"
        def realmRuleset = new RealmRuleset(keycloakTestSetup.realmBuilding.name, "Test realm definition", GROOVY, "SomeRulesCode")
        rulesetResource.createRealmRuleset(null, realmRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is updated"
        rulesetResource.updateRealmRuleset(null, 1234567890l, realmRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is deleted"
        rulesetResource.deleteRealmRuleset(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is created in a non-authenticated realm"
        realmRuleset = new RealmRuleset(keycloakTestSetup.realmCity.name, "Test realm definition", GROOVY, "SomeRulesCode")
        rulesetResource.createRealmRuleset(null, realmRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is created in the authenticated realm"
        def assetRuleset = new AssetRuleset(keycloakTestSetup.realmBuilding.name, "Test asset definition", GROOVY, "SomeRulesCode")
        rulesetResource.createAssetRuleset(null, assetRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is updated"
        rulesetResource.updateAssetRuleset(null, 1234567890l, assetRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is deleted"
        rulesetResource.deleteAssetRuleset(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is created in a non-authenticated realm"
        assetRuleset = new AssetRuleset(keycloakTestSetup.realmCity.name, "Test asset definition", GROOVY, "SomeRulesCode")
        rulesetResource.createAssetRuleset(null, assetRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the static rules time variable is reset"
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = expirationMillis
    }

    def "Access rules as testuser3"() {

        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def expirationMillis = TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = 500
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def rulesService = container.getService(RulesService.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "some imported rulesets"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakTestSetup, managerTestSetup)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "the ruleset resource"
        def rulesetResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name, accessToken).proxy(RulesResource.class)

        expect: "the rules engines to be ready"
        conditions.eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakTestSetup, managerTestSetup)
        }

        /* ############################################## READ ####################################### */

        when: "the global rules are retrieved"
        rulesetResource.getGlobalRulesets(null, null, false)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "some realm rules in a non-authenticated realm are retrieved"
        rulesetResource.getRealmRulesets(null, keycloakTestSetup.realmMaster.name, null, false)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "some realm rules in the authenticated realm are retrieved by the restricted user"
        def rulesets = rulesetResource.getRealmRulesets(null, keycloakTestSetup.realmBuilding.name, null, false)

        then: "no rulesets should be returned"
        rulesets.length == 0

        when: "some asset rules of a protected assigned asset are retrieved"
        def ruleDefinitions = rulesetResource.getAssetRulesets(null, managerTestSetup.apartment1Id, null, false)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some apartment 1 demo rules"

        when: "some asset rules of a protected but not assigned asset are retrieved"
        rulesetResource.getAssetRulesets(null, managerTestSetup.apartment2Id, null, false)

        then: "no rulesets should be returned"
        rulesets.length == 0

        /* ############################################## WRITE ####################################### */

        when: "a global rule is created"
        def ruleDefinition = new GlobalRuleset("Test definition", GROOVY, "SomeRulesCode")
        rulesetResource.createGlobalRuleset(null, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is updated"
        rulesetResource.updateGlobalRuleset(null, 1234567890l, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is deleted"
        rulesetResource.deleteGlobalRuleset(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is created in the authenticated realm"
        def realmRuleset = new RealmRuleset(keycloakTestSetup.realmBuilding.name, "Test realm definition", GROOVY, "SomeRulesCode")
        rulesetResource.createRealmRuleset(null, realmRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a realm ruleset is updated"
        rulesetResource.updateRealmRuleset(null, 1234567890l, realmRuleset)

        then: "result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a realm ruleset is deleted"
        rulesetResource.deleteRealmRuleset(null, 1234567890l)
        rulesetResource.getRealmRuleset(null, 1234567890l)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a realm ruleset is created in a non-authenticated realm"
        realmRuleset = new RealmRuleset(keycloakTestSetup.realmCity.name, "Test realm definition", GROOVY, "SomeRulesCode")
        rulesetResource.createRealmRuleset(null, realmRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset ruleset is created in the authenticated realm"
        def assetRuleset = new AssetRuleset(
            managerTestSetup.apartment1Id,
            "Test asset definition",
            GROOVY, "SomeRulesCode")
        assetRuleset.getMeta().add(new MetaItem<>(MetaItemType.SHOW_ON_DASHBOARD, true))
        rulesetResource.createAssetRuleset(null, assetRuleset)
        def rulesetId = rulesetResource.getAssetRulesets(null, managerTestSetup.apartment1Id, null, false)[1].id
        assetRuleset = rulesetResource.getAssetRuleset(null, rulesetId)
        def lastModified = assetRuleset.lastModified

        then: "result should match"
        assetRuleset.id == rulesetId
        assetRuleset.version == 0
        assetRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRuleset.name == "Test asset definition"
        assetRuleset.rules == "SomeRulesCode"
        assetRuleset.assetId == managerTestSetup.apartment1Id
        assetRuleset.meta.getValue(MetaItemType.SHOW_ON_DASHBOARD).orElse(false)

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.assetEngines.get(managerTestSetup.apartment1Id).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == assetRuleset.version
        }

        when: "an asset ruleset is updated"
        assetRuleset.name = "Renamed test asset definition"
        assetRuleset.rules = "SomeRulesCodeModified"
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)
        assetRuleset = rulesetResource.getAssetRuleset(null, rulesetId)

        then: "result should match"
        assetRuleset.id == rulesetId
        assetRuleset.version == 1
        assetRuleset.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRuleset.name == "Renamed test asset definition"
        assetRuleset.rules == "SomeRulesCodeModified"
        assetRuleset.assetId == managerTestSetup.apartment1Id

        and: "the ruleset should reach the engine"
        conditions.eventually {
            def deployment = rulesService.assetEngines.get(managerTestSetup.apartment1Id).deployments.get(rulesetId)
            assert deployment != null
            assert deployment.ruleset.version == assetRuleset.version
        }

        when: "an asset ruleset is updated with a changed asset ID"
        assetRuleset.assetId = managerTestSetup.apartment3Id
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset ruleset is updated with a changed invalid asset ID"
        assetRuleset.assetId = "thisdoesnotexist"
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset ruleset is updated with an invalid id"
        assetRuleset.assetId = managerTestSetup.apartment1Id
        assetRuleset.id = 1234567890l
        rulesetResource.updateAssetRuleset(null, rulesetId, assetRuleset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent asset ruleset is updated"
        rulesetResource.updateAssetRuleset(null, 1234567890l, assetRuleset)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset ruleset is deleted"
        rulesetResource.deleteAssetRuleset(null, rulesetId)
        rulesetResource.getAssetRuleset(null, rulesetId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        and: "the ruleset should be removed from the engine"
        conditions.eventually {
            assert rulesService.assetEngines.get(managerTestSetup.apartment1Id) == null
        }

        when: "an asset ruleset is created in the authenticated realm but on a forbidden asset"
        assetRuleset = new AssetRuleset(managerTestSetup.apartment3Id, "Test asset definition", GROOVY, "SomeRulesCode")
        rulesetResource.createAssetRuleset(null, assetRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset ruleset is created in a non-authenticated realm"
        assetRuleset = new AssetRuleset(managerTestSetup.smartOfficeId, "Test asset definition", GROOVY, "SomeRulesCode")
        rulesetResource.createAssetRuleset(null, assetRuleset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the static rules time variable is reset"
        TemporaryFact.GUARANTEED_MIN_EXPIRATION_MILLIS = expirationMillis
    }
}
