package org.openremote.test.rules

import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.rules.AssetRulesDefinition
import org.openremote.manager.shared.rules.GlobalRulesDefinition
import org.openremote.manager.shared.rules.RulesResource
import org.openremote.manager.shared.rules.TenantRulesDefinition
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class RulesPermissionsTest extends Specification implements ManagerContainerTrait {

    def "Access rules as superuser"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        and: "an authenticated admin user"
        def realm = MASTER_REALM
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the rules resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def rulesResource = getClientTarget(client, serverUri, realm, accessToken).proxy(RulesResource.class)

        /* ############################################## READ ####################################### */

        when: "the global rules are retrieved"
        def ruleDefinitions = rulesResource.getGlobalDefinitions(null)

        then: "result should match"
        ruleDefinitions.length == 2
        ruleDefinitions[0].name == "Some global demo rules"
        ruleDefinitions[0].rules == null // Don't retrieve the (large) rules data when getting a list of rule definitions

        when: "some tenant rules are retrieved"
        ruleDefinitions = rulesResource.getTenantDefinitions(null, MASTER_REALM)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some master tenant demo rules"

        when: "some tenant rules in a non-authenticated realm are retrieved"
        ruleDefinitions = rulesResource.getTenantDefinitions(null, "customerA")

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some customerA tenant demo rules"
        !ruleDefinitions[0].enabled

        when: "some asset rules are retrieved"
        ruleDefinitions = rulesResource.getAssetDefinitions(null, managerDemoSetup.apartment1Id)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some apartment 1 demo rules"

        /* ############################################## WRITE ####################################### */

        when: "global rules definitions is created"
        def globalRulesDefinition = new GlobalRulesDefinition("Test global definition", "ThisShouldBeDRL")
        rulesResource.createGlobalDefinition(null, globalRulesDefinition)
        def rulesDefinitionId = rulesResource.getGlobalDefinitions(null)[2].id
        globalRulesDefinition = rulesResource.getGlobalDefinition(null, rulesDefinitionId)
        def lastModified = globalRulesDefinition.lastModified

        then: "result should match"
        globalRulesDefinition.id == rulesDefinitionId
        globalRulesDefinition.version == 0
        globalRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        globalRulesDefinition.name == "Test global definition"
        globalRulesDefinition.rules == "ThisShouldBeDRL"

        when: "a global rules definition is updated"
        globalRulesDefinition.name = "Renamed test global definition"
        globalRulesDefinition.rules = "ThisShouldBeDRLAsWell"
        rulesResource.updateGlobalDefinition(null, rulesDefinitionId, globalRulesDefinition)
        globalRulesDefinition = rulesResource.getGlobalDefinition(null, rulesDefinitionId)

        then: "result should match"
        globalRulesDefinition.id == rulesDefinitionId
        globalRulesDefinition.version > 0
        globalRulesDefinition.createdOn.time < System.currentTimeMillis()
        globalRulesDefinition.lastModified.time > lastModified.time
        globalRulesDefinition.name == "Renamed test global definition"
        globalRulesDefinition.rules == "ThisShouldBeDRLAsWell"

        when: "a global rules definition is deleted"
        rulesResource.deleteGlobalDefinition(null, rulesDefinitionId)
        globalRulesDefinition = rulesResource.getGlobalDefinition(null, rulesDefinitionId)

        then: "the result should be not found"
        WebApplicationException ex = thrown()
        ex.response.status == 404

        when: "a non-existent global rules definition is updated"
        rulesResource.updateGlobalDefinition(null, 1234567890l, globalRulesDefinition)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a tenant rules definition is created in the authenticated realm"
        def tenantRulesDefinition = new TenantRulesDefinition("Test tenant definition", MASTER_REALM, "ThisShouldBeDRL")
        rulesResource.createTenantDefinition(null, tenantRulesDefinition)
        rulesDefinitionId = rulesResource.getTenantDefinitions(null, MASTER_REALM)[1].id
        tenantRulesDefinition = rulesResource.getTenantDefinition(null, rulesDefinitionId)
        lastModified = tenantRulesDefinition.lastModified

        then: "result should match"
        tenantRulesDefinition.id == rulesDefinitionId
        tenantRulesDefinition.version == 0
        tenantRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        tenantRulesDefinition.name == "Test tenant definition"
        tenantRulesDefinition.rules == "ThisShouldBeDRL"
        tenantRulesDefinition.realm == MASTER_REALM

        when: "a tenant rules definition is updated"
        tenantRulesDefinition.name = "Renamed test tenant definition"
        tenantRulesDefinition.rules = "ThisShouldBeDRLAsWell"
        rulesResource.updateTenantDefinition(null, rulesDefinitionId, tenantRulesDefinition)
        tenantRulesDefinition = rulesResource.getTenantDefinition(null, rulesDefinitionId)

        then: "result should match"
        tenantRulesDefinition.id == rulesDefinitionId
        tenantRulesDefinition.version == 1
        tenantRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        tenantRulesDefinition.name == "Renamed test tenant definition"
        tenantRulesDefinition.rules == "ThisShouldBeDRLAsWell"
        tenantRulesDefinition.realm == MASTER_REALM

        when: "a tenant rules definition is updated with an invalid realm"
        tenantRulesDefinition.realm = "thisdoesnotexist"
        rulesResource.updateTenantDefinition(null, rulesDefinitionId, tenantRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a tenant rules definition is updated with an invalid id"
        tenantRulesDefinition.realm = MASTER_REALM
        tenantRulesDefinition.id = 1234567890l
        rulesResource.updateTenantDefinition(null, rulesDefinitionId, tenantRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent tenant rules definition is updated"
        rulesResource.updateTenantDefinition(null, 1234567890l, tenantRulesDefinition)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a tenant rules definition is deleted"
        rulesResource.deleteTenantDefinition(null, rulesDefinitionId)
        tenantRulesDefinition = rulesResource.getTenantDefinition(null, rulesDefinitionId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a tenant rules definition is created in a non-authenticated realm"
        tenantRulesDefinition = new TenantRulesDefinition("Test tenant definition", "customerA", "ThisShouldBeDRL")
        rulesResource.createTenantDefinition(null, tenantRulesDefinition)
        rulesDefinitionId = rulesResource.getTenantDefinitions(null, "customerA")[1].id
        tenantRulesDefinition = rulesResource.getTenantDefinition(null, rulesDefinitionId)
        lastModified = tenantRulesDefinition.lastModified

        then: "result should match"
        tenantRulesDefinition.id == rulesDefinitionId
        tenantRulesDefinition.version == 0
        tenantRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        tenantRulesDefinition.name == "Test tenant definition"
        tenantRulesDefinition.rules == "ThisShouldBeDRL"
        tenantRulesDefinition.realm == "customerA"

        when: "an asset rules definition is created in the authenticated realm"
        def assetRulesDefinition = new AssetRulesDefinition("Test asset definition", managerDemoSetup.smartOfficeId, "ThisShouldBeDRL")
        rulesResource.createAssetDefinition(null, assetRulesDefinition)
        rulesDefinitionId = rulesResource.getAssetDefinitions(null, managerDemoSetup.smartOfficeId)[0].id
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)
        lastModified = assetRulesDefinition.lastModified

        then: "result should match"
        assetRulesDefinition.id == rulesDefinitionId
        assetRulesDefinition.version == 0
        assetRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRulesDefinition.name == "Test asset definition"
        assetRulesDefinition.rules == "ThisShouldBeDRL"
        assetRulesDefinition.assetId == managerDemoSetup.smartOfficeId

        when: "an asset rules definition is updated"
        assetRulesDefinition.name = "Renamed test asset definition"
        assetRulesDefinition.rules = "ThisShouldBeDRLAsWell"
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)

        then: "result should match"
        assetRulesDefinition.id == rulesDefinitionId
        assetRulesDefinition.version == 1
        assetRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRulesDefinition.name == "Renamed test asset definition"
        assetRulesDefinition.rules == "ThisShouldBeDRLAsWell"
        assetRulesDefinition.assetId == managerDemoSetup.smartOfficeId

        when: "an asset rules definition is updated with an invalid asset ID"
        assetRulesDefinition.assetId = "thisdoesnotexist"
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset rules definition is updated with an invalid id"
        assetRulesDefinition.assetId = managerDemoSetup.smartOfficeId
        assetRulesDefinition.id = 1234567890l
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent asset rules definition is updated"
        rulesResource.updateAssetDefinition(null, 1234567890l, assetRulesDefinition)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset rules definition is deleted"
        rulesResource.deleteAssetDefinition(null, rulesDefinitionId)
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset rules definition is created in a non-authenticated realm"
        assetRulesDefinition = new AssetRulesDefinition("Test asset definition", managerDemoSetup.apartment1Id, "ThisShouldBeDRL")
        rulesResource.createAssetDefinition(null, assetRulesDefinition)
        rulesDefinitionId = rulesResource.getAssetDefinitions(null, managerDemoSetup.apartment1Id)[1].id
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)
        lastModified = assetRulesDefinition.lastModified

        then: "result should match"
        assetRulesDefinition.id == rulesDefinitionId
        assetRulesDefinition.version == 0
        assetRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRulesDefinition.name == "Test asset definition"
        assetRulesDefinition.rules == "ThisShouldBeDRL"
        assetRulesDefinition.assetId == managerDemoSetup.apartment1Id

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Access rules as testuser1"() {

        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        and: "an authenticated test user"
        def realm = MASTER_REALM
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        and: "the asset resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def rulesResource = getClientTarget(client, serverUri, realm, accessToken).proxy(RulesResource.class)

        /* ############################################## READ ####################################### */

        when: "the global rules are retrieved"
        def ruleDefinitions = rulesResource.getGlobalDefinitions(null)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "some tenant rules are retrieved"
        ruleDefinitions = rulesResource.getTenantDefinitions(null, MASTER_REALM)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some master tenant demo rules"

        when: "some tenant rules in a non-authenticated realm are retrieved"
        ruleDefinitions = rulesResource.getTenantDefinitions(null, "customerA")

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "some asset rules in a non-authenticated realm are retrieved"
        ruleDefinitions = rulesResource.getAssetDefinitions(null, managerDemoSetup.apartment1Id)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "a global rule is created"
        def ruleDefinition = new GlobalRulesDefinition("Test definition", "ThisShouldBeDRL")
        rulesResource.createGlobalDefinition(null, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is updated"
        rulesResource.updateGlobalDefinition(null, 1234567890l, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is deleted"
        rulesResource.deleteGlobalDefinition(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is created in the authenticated realm"
        def tenantRulesDefinition = new TenantRulesDefinition("Test tenant definition", MASTER_REALM, "ThisShouldBeDRL")
        rulesResource.createTenantDefinition(null, tenantRulesDefinition)
        def rulesDefinitionId = rulesResource.getTenantDefinitions(null, MASTER_REALM)[1].id
        tenantRulesDefinition = rulesResource.getTenantDefinition(null, rulesDefinitionId)
        def lastModified = tenantRulesDefinition.lastModified

        then: "result should match"
        tenantRulesDefinition.id == rulesDefinitionId
        tenantRulesDefinition.version == 0
        tenantRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        tenantRulesDefinition.name == "Test tenant definition"
        tenantRulesDefinition.rules == "ThisShouldBeDRL"
        tenantRulesDefinition.realm == MASTER_REALM

        when: "a tenant rules definition is updated"
        tenantRulesDefinition.name = "Renamed test tenant definition"
        tenantRulesDefinition.rules = "ThisShouldBeDRLAsWell"
        rulesResource.updateTenantDefinition(null, rulesDefinitionId, tenantRulesDefinition)
        tenantRulesDefinition = rulesResource.getTenantDefinition(null, rulesDefinitionId)

        then: "result should match"
        tenantRulesDefinition.id == rulesDefinitionId
        tenantRulesDefinition.version == 1
        tenantRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        tenantRulesDefinition.name == "Renamed test tenant definition"
        tenantRulesDefinition.rules == "ThisShouldBeDRLAsWell"
        tenantRulesDefinition.realm == MASTER_REALM

        when: "a tenant rules definition is updated with an invalid realm"
        tenantRulesDefinition.realm = "thisdoesnotexist"
        rulesResource.updateTenantDefinition(null, rulesDefinitionId, tenantRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a tenant rules definition is updated with an invalid id"
        tenantRulesDefinition.realm = MASTER_REALM
        tenantRulesDefinition.id = 1234567890l
        rulesResource.updateTenantDefinition(null, rulesDefinitionId, tenantRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent tenant rules definition is updated"
        rulesResource.updateTenantDefinition(null, 1234567890l, tenantRulesDefinition)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a tenant rules definition is deleted"
        rulesResource.deleteTenantDefinition(null, rulesDefinitionId)
        tenantRulesDefinition = rulesResource.getTenantDefinition(null, rulesDefinitionId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a tenant rules definition is created in a non-authenticated realm"
        tenantRulesDefinition = new TenantRulesDefinition("Test tenant definition", "customerA", "ThisShouldBeDRL")
        rulesResource.createTenantDefinition(null, tenantRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset rules definition is created in the authenticated realm"
        def assetRulesDefinition = new AssetRulesDefinition("Test asset definition", managerDemoSetup.smartOfficeId, "ThisShouldBeDRL")
        rulesResource.createAssetDefinition(null, assetRulesDefinition)
        rulesDefinitionId = rulesResource.getAssetDefinitions(null, managerDemoSetup.smartOfficeId)[0].id
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)
        lastModified = assetRulesDefinition.lastModified

        then: "result should match"
        assetRulesDefinition.id == rulesDefinitionId
        assetRulesDefinition.version == 0
        assetRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRulesDefinition.name == "Test asset definition"
        assetRulesDefinition.rules == "ThisShouldBeDRL"
        assetRulesDefinition.assetId == managerDemoSetup.smartOfficeId

        when: "an asset rules definition is updated"
        assetRulesDefinition.name = "Renamed test asset definition"
        assetRulesDefinition.rules = "ThisShouldBeDRLAsWell"
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)

        then: "result should match"
        assetRulesDefinition.id == rulesDefinitionId
        assetRulesDefinition.version == 1
        assetRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRulesDefinition.name == "Renamed test asset definition"
        assetRulesDefinition.rules == "ThisShouldBeDRLAsWell"
        assetRulesDefinition.assetId == managerDemoSetup.smartOfficeId

        when: "an asset rules definition is updated with an invalid asset ID"
        assetRulesDefinition.assetId = "thisdoesnotexist"
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset rules definition is updated with an invalid id"
        assetRulesDefinition.assetId = managerDemoSetup.smartOfficeId
        assetRulesDefinition.id = 1234567890l
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent asset rules definition is updated"
        rulesResource.updateAssetDefinition(null, 1234567890l, assetRulesDefinition)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset rules definition is deleted"
        rulesResource.deleteAssetDefinition(null, rulesDefinitionId)
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset rules definition is created in a non-authenticated realm"
        assetRulesDefinition = new AssetRulesDefinition("Test asset definition", managerDemoSetup.apartment1Id, "ThisShouldBeDRL")
        rulesResource.createAssetDefinition(null, assetRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Access rules as testuser2"() {

        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        and: "an authenticated test user"
        def realm = "customerA"
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser2",
                "testuser2"
        ).token

        and: "the asset resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def rulesResource = getClientTarget(client, serverUri, realm, accessToken).proxy(RulesResource.class)

        /* ############################################## READ ####################################### */

        when: "the global rules are retrieved"
        def ruleDefinitions = rulesResource.getGlobalDefinitions(null)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "some tenant rules in a non-authenticated realm are retrieved"
        ruleDefinitions = rulesResource.getTenantDefinitions(null, MASTER_REALM)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "some tenant rules in the authenticated realm are retrieved"
        ruleDefinitions = rulesResource.getTenantDefinitions(null, "customerA")

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "some asset rules in the authenticated realm are retrieved"
        ruleDefinitions = rulesResource.getAssetDefinitions(null, managerDemoSetup.apartment1Id)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "a global rule is created"
        def ruleDefinition = new GlobalRulesDefinition("Test definition", "ThisShouldBeDRL")
        rulesResource.createGlobalDefinition(null, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is updated"
        rulesResource.updateGlobalDefinition(null, 1234567890l, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is deleted"
        rulesResource.deleteGlobalDefinition(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is created in the authenticated realm"
        def tenantRulesDefinition = new TenantRulesDefinition("Test tenant definition", "customerA", "ThisShouldBeDRL")
        rulesResource.createTenantDefinition(null, tenantRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is updated"
        rulesResource.updateTenantDefinition(null, 1234567890l, tenantRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is deleted"
        rulesResource.deleteTenantDefinition(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is created in a non-authenticated realm"
        tenantRulesDefinition = new TenantRulesDefinition("Test tenant definition", "customerB", "ThisShouldBeDRL")
        rulesResource.createTenantDefinition(null, tenantRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is created in the authenticated realm"
        def assetRulesDefinition = new AssetRulesDefinition("Test asset definition", "customerA", "ThisShouldBeDRL")
        rulesResource.createAssetDefinition(null, assetRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is updated"
        rulesResource.updateAssetDefinition(null, 1234567890l, assetRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is deleted"
        rulesResource.deleteAssetDefinition(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is created in a non-authenticated realm"
        assetRulesDefinition = new AssetRulesDefinition("Test asset definition", "customerB", "ThisShouldBeDRL")
        rulesResource.createAssetDefinition(null, assetRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Access rules as testuser3"() {

        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        and: "an authenticated test user"
        def realm = "customerA"
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "the asset resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def rulesResource = getClientTarget(client, serverUri, realm, accessToken).proxy(RulesResource.class)

        /* ############################################## READ ####################################### */

        when: "the global rules are retrieved"
        def ruleDefinitions = rulesResource.getGlobalDefinitions(null)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "some tenant rules in a non-authenticated realm are retrieved"
        ruleDefinitions = rulesResource.getTenantDefinitions(null, MASTER_REALM)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "some tenant rules in the authenticated realm are retrieved"
        ruleDefinitions = rulesResource.getTenantDefinitions(null, "customerA")

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "some asset rules of a protected assigned asset are retrieved"
        ruleDefinitions = rulesResource.getAssetDefinitions(null, managerDemoSetup.apartment1Id)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some apartment 1 demo rules"

        when: "some asset rules of a protected assigned asset are retrieved"
        ruleDefinitions = rulesResource.getAssetDefinitions(null, managerDemoSetup.apartment2Id)

        then: "result should match"
        ruleDefinitions.length == 1
        ruleDefinitions[0].name == "Some apartment 2 demo rules"

        when: "some asset rules of a protected but not assigned asset are retrieved"
        ruleDefinitions = rulesResource.getAssetDefinitions(null, managerDemoSetup.apartment3Id)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "a global rule is created"
        def ruleDefinition = new GlobalRulesDefinition("Test definition", "ThisShouldBeDRL")
        rulesResource.createGlobalDefinition(null, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is updated"
        rulesResource.updateGlobalDefinition(null, 1234567890l, ruleDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a global rule is deleted"
        rulesResource.deleteGlobalDefinition(null, 1234567890l)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is created in the authenticated realm"
        def tenantRulesDefinition = new TenantRulesDefinition("Test tenant definition", "customerA", "ThisShouldBeDRL")
        rulesResource.createTenantDefinition(null, tenantRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a tenant rules definition is updated"
        rulesResource.updateTenantDefinition(null, 1234567890l, tenantRulesDefinition)

        then: "result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a tenant rules definition is deleted"
        rulesResource.deleteTenantDefinition(null, 1234567890l)
        tenantRulesDefinition = rulesResource.getTenantDefinition(null, 1234567890l)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a tenant rules definition is created in a non-authenticated realm"
        tenantRulesDefinition = new TenantRulesDefinition("Test tenant definition", "customerB", "ThisShouldBeDRL")
        rulesResource.createTenantDefinition(null, tenantRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset rules definition is created in the authenticated realm"
        def assetRulesDefinition = new AssetRulesDefinition("Test asset definition", managerDemoSetup.apartment1Id, "ThisShouldBeDRL")
        rulesResource.createAssetDefinition(null, assetRulesDefinition)
        def rulesDefinitionId = rulesResource.getAssetDefinitions(null, managerDemoSetup.apartment1Id)[1].id
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)
        def lastModified = assetRulesDefinition.lastModified

        then: "result should match"
        assetRulesDefinition.id == rulesDefinitionId
        assetRulesDefinition.version == 0
        assetRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRulesDefinition.name == "Test asset definition"
        assetRulesDefinition.rules == "ThisShouldBeDRL"
        assetRulesDefinition.assetId == managerDemoSetup.apartment1Id

        when: "an asset rules definition is updated"
        assetRulesDefinition.name = "Renamed test asset definition"
        assetRulesDefinition.rules = "ThisShouldBeDRLAsWell"
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)

        then: "result should match"
        assetRulesDefinition.id == rulesDefinitionId
        assetRulesDefinition.version == 1
        assetRulesDefinition.createdOn.time < System.currentTimeMillis()
        lastModified.time < System.currentTimeMillis()
        assetRulesDefinition.name == "Renamed test asset definition"
        assetRulesDefinition.rules == "ThisShouldBeDRLAsWell"
        assetRulesDefinition.assetId == managerDemoSetup.apartment1Id

        when: "an asset rules definition is updated with a changed asset ID"
        assetRulesDefinition.assetId = managerDemoSetup.apartment3Id
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset rules definition is updated with a changed invalid asset ID"
        assetRulesDefinition.assetId = "thisdoesnotexist"
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset rules definition is updated with an invalid id"
        assetRulesDefinition.assetId = managerDemoSetup.apartment1Id
        assetRulesDefinition.id = 1234567890l
        rulesResource.updateAssetDefinition(null, rulesDefinitionId, assetRulesDefinition)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "a non-existent asset rules definition is updated"
        rulesResource.updateAssetDefinition(null, 1234567890l, assetRulesDefinition)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset rules definition is deleted"
        rulesResource.deleteAssetDefinition(null, rulesDefinitionId)
        assetRulesDefinition = rulesResource.getAssetDefinition(null, rulesDefinitionId)

        then: "the result should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset rules definition is created in the authenticated realm but on a forbidden asset "
        assetRulesDefinition = new AssetRulesDefinition("Test asset definition", managerDemoSetup.apartment3Id, "ThisShouldBeDRL")
        rulesResource.createAssetDefinition(null, assetRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset rules definition is created in a non-authenticated realm"
        assetRulesDefinition = new AssetRulesDefinition("Test asset definition", managerDemoSetup.smartOfficeId, "ThisShouldBeDRL")
        rulesResource.createAssetDefinition(null, assetRulesDefinition)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
