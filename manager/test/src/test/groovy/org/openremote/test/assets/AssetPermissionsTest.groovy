package org.openremote.test.assets

import com.fasterxml.uuid.Generators
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.asset.AssetResource
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetType
import org.openremote.model.attribute.AttributeType
import org.openremote.model.attribute.Meta
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo

class AssetPermissionsTest extends Specification implements ManagerContainerTrait {

    def "Access assets as superuser"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def conditions = new PollingConditions(delay: 1, timeout: 5)

        and: "an authenticated admin user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource"
        def assetResource = getClientTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)

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
        def testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.masterTenant.id)
        testAsset.setId(Generators.randomBasedGenerator().generate().toString())
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
        assetResource.delete(null, managerDemoSetup.apartment2LivingroomId)
        assetResource.get(null, managerDemoSetup.apartment2LivingroomId)

        then: "the asset should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, "geoStreet", Values.create("Teststreet 123").toJson())

        then: "result should match"
        def asset
        conditions.eventually {
            asset = assetResource.get(null, managerDemoSetup.smartOfficeId)
            assert asset.getAttribute("geoStreet").get().getValue().isPresent()
            assert asset.getAttribute("geoStreet").get().getValue().get().toJson() == Values.create("Teststreet 123").toJson()
        }

        when: "an non-existent assets attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, "doesnotexist", "geoStreet", Values.create("Teststreet 123").toJson())

        then: "the attribute should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an non-existent attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, "doesnotexist", Values.create("Teststreet 123").toJson())

        then: "the attribute should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset attribute is written in a foreign realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartHomeId, "geoStreet", Values.create("Teststreet 456").toJson())

        then: "result should match"
        conditions.eventually {
            asset = assetResource.get(null, managerDemoSetup.smartHomeId)
            assert asset.getAttribute("geoStreet").get().getValue().isPresent()
            assert asset.getAttribute("geoStreet").get().getValue().get().toJson() == Values.create("Teststreet 456").toJson()
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

    def "Access assets as testuser1"() {

        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def conditions = new PollingConditions(delay: 1, timeout: 10)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        and: "the asset resource"
        def assetResource = getClientTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartOfficeId
        // Assets should not be completely loaded
        assets[0].path == null
        assets[0].attributesList.size() == 0

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
        testAsset.setId(Generators.randomBasedGenerator().generate().toString())
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
        assetResource.delete(null, managerDemoSetup.apartment2LivingroomId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, "geoStreet", Values.create("Teststreet 123").toJson())

        then: "result should match"
        conditions.eventually {
            def asset = assetResource.get(null, managerDemoSetup.smartOfficeId)
            assert asset.getAttribute("geoStreet").get().getValue().isPresent()
            assert asset.getAttribute("geoStreet").get().getValue().get().toJson() == Values.create("Teststreet 123").toJson()
        }

        when: "an asset attribute is written in a foreign realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartHomeId, "geoStreet", Values.create("Teststreet 456").toJson())

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
        def assetResource = getClientTarget(serverUri(serverPort), keycloakDemoSetup.customerATenant.realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartHomeId
        // Assets should not be completely loaded
        assets[0].path == null
        assets[0].attributesList.size() == 0

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
        def testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.masterTenant.id)
        testAsset.setId(Generators.randomBasedGenerator().generate().toString())
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
        testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.customerATenant.id)
        testAsset.setId(Generators.randomBasedGenerator().generate().toString())
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, managerDemoSetup.apartment2LivingroomId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, managerDemoSetup.thingId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartHomeId, "geoStreet", Values.create("Teststreet 123").toJson())

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset attribute is written in a foreign realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, "geoStreet", Values.create("Teststreet 456").toJson())

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
        def conditions = new PollingConditions(delay: 1, timeout: 5)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                keycloakDemoSetup.customerATenant.realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "the asset resource"
        def assetResource = getClientTarget(serverUri(serverPort), keycloakDemoSetup.customerATenant.realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the assets of the authenticated user are retrieved"
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
        apartment1.path == null
        apartment1.attributesList.size() == 0

        Asset apartment1Livingroom = assets[1]
        apartment1Livingroom.id == managerDemoSetup.apartment1LivingroomId
        apartment1Livingroom.name == "Living Room"

        Asset apartment1Kitchen = assets[2]
        apartment1Kitchen.id == managerDemoSetup.apartment1KitchenId
        apartment1Kitchen.name == "Kitchen"

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

        when: "a user asset is retrieved by ID in the authenticated realm"
        apartment1Livingroom = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)

        then: "the protected asset details should be available"
        apartment1Livingroom.id == managerDemoSetup.apartment1LivingroomId
        apartment1Livingroom.name == "Living Room"
        def protectedAttributes = apartment1Livingroom.getAttributesList()
        protectedAttributes.size() == 6
        def currentTemperature = apartment1Livingroom.getAttribute("currentTemperature").get()
        currentTemperature.getType().get() == AttributeType.TEMPERATURE_CELCIUS
        !currentTemperature.getValue().isPresent()
        Meta protectedMeta = currentTemperature.getMeta()
        protectedMeta.size() == 6
        protectedMeta.stream().filter(isMetaNameEqualTo(AssetMeta.LABEL)).findFirst().get().getValueAsString().get() == "Current temperature"
        protectedMeta.stream().filter(isMetaNameEqualTo(AssetMeta.READ_ONLY)).findFirst().get().getValueAsBoolean().get()
        protectedMeta.stream().filter(isMetaNameEqualTo(AssetMeta.RULE_STATE)).findFirst().get().getValueAsBoolean().get()
        protectedMeta.stream().filter(isMetaNameEqualTo(AssetMeta.STORE_DATA_POINTS)).findFirst().get().getValueAsBoolean().get()
        protectedMeta.stream().filter(isMetaNameEqualTo(AssetMeta.SHOW_ON_DASHBOARD)).findFirst().get().getValueAsBoolean().get()
        protectedMeta.stream().filter(isMetaNameEqualTo(AssetMeta.FORMAT)).findFirst().get().getValueAsString().get() == "%0.1f C"

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerDemoSetup.thingId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in a foreign realm"
        def testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.masterTenant.id)
        testAsset.setId(Generators.randomBasedGenerator().generate().toString())
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is made a root asset in the authenticated realm"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)

        then: "a TODO error should occur"
        ex = thrown()
        ex.response.status == 500

        when: "an asset is created in the authenticated realm"
        testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.customerATenant.id)
        testAsset.setId(Generators.randomBasedGenerator().generate().toString())
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

        when: "a private asset attribute is written on a user asset"
        assetResource.writeAttributeValue(null, managerDemoSetup.apartment1LivingroomId, "lightSwitch", Values.create(false).toJson())

        then: "the attribute should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a protected read-only asset attribute is written on a user asset"
        assetResource.writeAttributeValue(null, managerDemoSetup.apartment1LivingroomId, "currentTemperature", Values.create(22.123).toJson())

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a protected asset attribute is written on a user asset"
        assetResource.writeAttributeValue(null, managerDemoSetup.apartment1LivingroomId, "targetTemperature", Values.create(22.123).toJson())

        then: "result should match"
        conditions.eventually {
            def asset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)
            assert asset.getAttribute("targetTemperature").get().getValue().isPresent()
            assert asset.getAttribute("targetTemperature").get().getValue().get().toJson() == Values.create(22.123).toJson()
        }

        when: "an attribute is written on a non-existent user asset"
        assetResource.writeAttributeValue(null, "doesnotexist", "lightSwitch", Values.create(false).toJson())

        then: "the attribute should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an non-existent attribute is written on a user asset"
        assetResource.writeAttributeValue(null, managerDemoSetup.apartment1LivingroomId, "doesnotexist", Values.create("foo").toJson())

        then: "the attribute should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset attribute is written on a non-user asset"
        assetResource.writeAttributeValue(null, managerDemoSetup.apartment3LivingroomId, "lightSwitch", Values.create(false).toJson())

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset attribute is written in a foreign realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, "geoStreet", Values.create("Teststreet 123").toJson())

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
