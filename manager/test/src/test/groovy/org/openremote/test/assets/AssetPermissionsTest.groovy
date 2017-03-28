package org.openremote.test.assets

import org.openremote.container.util.IdentifierUtil
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.asset.AssetResource
import org.openremote.model.AttributeType
import org.openremote.model.Meta
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttributes
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AssetPermissionsTest extends Specification implements ManagerContainerTrait {

    def "Access assets as superuser"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        and: "an authenticated admin user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def assetResource = getClientTarget(client, serverUri, MASTER_REALM, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 0

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.getRoot(null, keycloakDemoSetup.masterTenant.id)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartOfficeId

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assets = assetResource.getChildren(null, assets[0].id)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.groundFloorId

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.getRoot(null, null)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartOfficeId

        when: "the root assets of the given realm are retrieved"
        assets = assetResource.getRoot(null, keycloakDemoSetup.customerATenant.id)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartHomeId

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.getChildren(null, assets[0].id)

        then: "result should match"
        assets.length == 3
        // Should be ordered by creation time
        assets[0].id == managerDemoSetup.apartment1Id
        assets[1].id == managerDemoSetup.apartment2Id
        assets[2].id == managerDemoSetup.apartment3Id

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoThing = assetResource.get(null, managerDemoSetup.thingId)

        then: "result should match"
        demoThing.id == managerDemoSetup.thingId

        when: "an asset is retrieved by ID in a foreign realm"
        def demoSmartHome = assetResource.get(null, managerDemoSetup.smartHomeId)

        then: "result should match"
        demoSmartHome.id == managerDemoSetup.smartHomeId

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in the authenticated realm"
        def testAsset = new Asset(keycloakDemoSetup.masterTenant.id, "Test Room", AssetType.ROOM)
        testAsset.setId(IdentifierUtil.generateGlobalUniqueId())
        assetResource.create(null, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.wellKnownType == AssetType.ROOM
        testAsset.realmId == keycloakDemoSetup.masterTenant.id
        testAsset.parentId == null

        when: "an asset is updated with a new parent in the authenticated realm"
        testAsset.setParentId(managerDemoSetup.groundFloorId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == managerDemoSetup.groundFloorId

        when: "an asset is moved to a foreign realm and made a root asset"
        testAsset.setRealmId(keycloakDemoSetup.customerATenant.id)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.realmId == keycloakDemoSetup.customerATenant.id
        testAsset.parentId == null

        when: "an asset is updated with a new parent in a foreign realm"
        testAsset.setParentId(managerDemoSetup.smartHomeId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.realmId == keycloakDemoSetup.customerATenant.id
        testAsset.parentId == managerDemoSetup.smartHomeId

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, managerDemoSetup.thingId)
        assetResource.get(null, managerDemoSetup.thingId)

        then: "the asset should not be found"
        WebApplicationException ex = thrown()
        ex.response.status == 404

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, managerDemoSetup.apartment1LivingroomThermostatId)
        assetResource.get(null, managerDemoSetup.apartment1LivingroomThermostatId)

        then: "the asset should be not found"
        ex = thrown()
        ex.response.status == 404

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Access assets as testuser1"() {

        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        and: "the asset resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def assetResource = getClientTarget(client, serverUri, MASTER_REALM, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 0

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.getRoot(null, keycloakDemoSetup.masterTenant.id)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartOfficeId

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assets = assetResource.getChildren(null, assets[0].id)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.groundFloorId

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.getRoot(null, null)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartOfficeId
        assets[0].realmId == keycloakDemoSetup.masterTenant.id

        when: "the root assets of the given realm are retrieved"
        assets = assetResource.getRoot(null, keycloakDemoSetup.customerATenant.id)

        then: "result should match"
        assets.length == 0

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.getChildren(null, managerDemoSetup.smartHomeId)

        then: "result should be empty"
        assets.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoThing = assetResource.get(null, managerDemoSetup.thingId)

        then: "result should match"
        demoThing.id == managerDemoSetup.thingId

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerDemoSetup.smartHomeId)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in the authenticated realm"
        def testAsset = new Asset("Test Room", AssetType.ROOM)  // Note: no realm means auth realm
        testAsset.setId(IdentifierUtil.generateGlobalUniqueId())
        assetResource.create(null, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.wellKnownType == AssetType.ROOM
        testAsset.realmId == keycloakDemoSetup.masterTenant.id
        testAsset.parentId == null

        when: "an asset is updated with a new parent in the authenticated realm"
        testAsset.setParentId(managerDemoSetup.groundFloorId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == managerDemoSetup.groundFloorId

        when: "an asset is moved to a foreign realm and made a root asset"
        testAsset.setRealmId(keycloakDemoSetup.customerATenant.id)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is updated with a new parent in a foreign realm"
        testAsset.setParentId(managerDemoSetup.smartHomeId)
        assetResource.update(null, testAsset.id, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, managerDemoSetup.thingId)
        assetResource.get(null, managerDemoSetup.thingId)

        then: "the asset should not be found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, managerDemoSetup.apartment1LivingroomThermostatId)
        assetResource.get(null, managerDemoSetup.apartment1LivingroomThermostatId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Access assets as testuser2"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                keycloakDemoSetup.customerATenant.realm,
                KEYCLOAK_CLIENT_ID,
                "testuser2",
                "testuser2"
        ).token

        and: "the asset resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def assetResource = getClientTarget(client, serverUri, keycloakDemoSetup.customerATenant.realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 0

        when: "the root assets of a foreign realm are retrieved"
        assets = assetResource.getRoot(null, keycloakDemoSetup.masterTenant.id)

        then: "result should match"
        assets.length == 0

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.getRoot(null, null)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartHomeId
        assets[0].realmId == keycloakDemoSetup.customerATenant.id

        when: "the root assets of the given realm are retrieved"
        assets = assetResource.getRoot(null, keycloakDemoSetup.customerBTenant.id)

        then: "result should match"
        assets.length == 0

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.getChildren(null, managerDemoSetup.thingId)

        then: "result should be empty"
        assets.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoSmartHome = assetResource.get(null, managerDemoSetup.smartHomeId)

        then: "result should match"
        demoSmartHome.id == managerDemoSetup.smartHomeId

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerDemoSetup.thingId)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in a foreign realm"
        def testAsset = new Asset(keycloakDemoSetup.masterTenant.id, "Test Room", AssetType.ROOM)
        testAsset.setId(IdentifierUtil.generateGlobalUniqueId())
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is made a root asset in the authenticated realm"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1Id)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is created in the authenticated realm"
        testAsset = new Asset(keycloakDemoSetup.customerATenant.id, "Test Room", AssetType.ROOM)
        testAsset.setId(IdentifierUtil.generateGlobalUniqueId())
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, managerDemoSetup.apartment1LivingroomThermostatId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, managerDemoSetup.thingId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Access assets as testuser3"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                keycloakDemoSetup.customerATenant.realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "the asset resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def assetResource = getClientTarget(client, serverUri, keycloakDemoSetup.customerATenant.realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 4
        Asset apartment1 = assets[0]
        apartment1.id == managerDemoSetup.apartment1Id
        apartment1.name == "Apartment 1"
        apartment1.createdOn.getTime() < System.currentTimeMillis()
        apartment1.realmId == keycloakDemoSetup.customerATenant.id
        apartment1.type == AssetType.RESIDENCE.value
        apartment1.parentId == managerDemoSetup.smartHomeId
        apartment1.coordinates[0] == 5.470945d
        apartment1.coordinates[1] == 51.438d

        Asset apartment1Livingroom = assets[1]
        apartment1Livingroom.id == managerDemoSetup.apartment1LivingroomId
        apartment1Livingroom.name == "Livingroom"

        Asset apartment1LivingroomThermostat = assets[2]
        apartment1LivingroomThermostat.id == managerDemoSetup.apartment1LivingroomThermostatId
        apartment1LivingroomThermostat.name == "Livingroom Thermostat"

        AssetAttributes protectedAttributes = new AssetAttributes(apartment1LivingroomThermostat.attributes)
        protectedAttributes.size() == 2
        protectedAttributes.get("currentTemperature")
        protectedAttributes.get("currentTemperature").getType() == AttributeType.DECIMAL
        protectedAttributes.get("currentTemperature").getValueAsDecimal() == null
        Meta protectedMeta = protectedAttributes.get("currentTemperature").getMeta()
        protectedMeta.all().length == 3
        protectedMeta.first(AssetMeta.LABEL).getValueAsString() == "Current Temperature"
        protectedMeta.first(AssetMeta.READ_ONLY).getValueAsBoolean()
        protectedMeta.first(AssetMeta.RULES_FACT).getValueAsBoolean()

        Asset apartment2 = assets[3]
        apartment2.id == managerDemoSetup.apartment2Id
        apartment2.name == "Apartment 2"

        when: "the root assets of a foreign realm are retrieved"
        assets = assetResource.getRoot(null, keycloakDemoSetup.masterTenant.id)

        then: "result should match"
        assets.length == 0

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.getRoot(null, null)

        then: "result should match"
        assets.length == 0

        when: "the root assets of the given realm are retrieved"
        assets = assetResource.getRoot(null, keycloakDemoSetup.customerBTenant.id)

        then: "result should match"
        assets.length == 0

        when: "the child assets of linked asset are retrieved"
        assets = assetResource.getChildren(null, managerDemoSetup.apartment1Id)

        then: "result should match"
        assets.length == 0

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assets = assetResource.getChildren(null, managerDemoSetup.smartHomeId)

        then: "result should match"
        assets.length == 0

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.getChildren(null, managerDemoSetup.thingId)

        then: "result should be empty"
        assets.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        assetResource.get(null, managerDemoSetup.smartHomeId)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerDemoSetup.thingId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in a foreign realm"
        def testAsset = new Asset(keycloakDemoSetup.masterTenant.id, "Test Room", AssetType.ROOM)
        testAsset.setId(IdentifierUtil.generateGlobalUniqueId())
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is made a root asset in the authenticated realm"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is created in the authenticated realm"
        testAsset = new Asset(keycloakDemoSetup.customerATenant.id, "Test Room", AssetType.ROOM)
        testAsset.setId(IdentifierUtil.generateGlobalUniqueId())
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, managerDemoSetup.apartment1LivingroomId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, managerDemoSetup.thingId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
