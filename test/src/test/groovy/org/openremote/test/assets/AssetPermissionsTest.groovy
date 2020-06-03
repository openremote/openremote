package org.openremote.test.assets

import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.attribute.AttributeType
import org.openremote.model.query.AssetQuery
import org.openremote.model.asset.AssetType
import org.openremote.model.attribute.AttributeValueType
import org.openremote.model.attribute.Meta
import org.openremote.model.attribute.MetaItem
import org.openremote.model.query.filter.ParentPredicate
import org.openremote.model.query.filter.TenantPredicate
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.attribute.MetaItemType.*
import static org.openremote.model.attribute.AttributeValueType.BOOLEAN
import static org.openremote.model.attribute.AttributeValueType.NUMBER
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
                getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 0

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartOfficeId

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(assets[0].id))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.groundFloorId

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartOfficeId

        when: "the root assets of the given realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.tenantBuilding.realm))
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartBuildingId

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(assets[0].id))
        )

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
        def demoSmartBuilding = assetResource.get(null, managerDemoSetup.smartBuildingId)

        then: "result should match"
        demoSmartBuilding.id == managerDemoSetup.smartBuildingId

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in the authenticated realm"
        def testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.masterTenant.realm)
        testAsset = assetResource.create(null, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.wellKnownType == AssetType.ROOM
        testAsset.realm == keycloakDemoSetup.masterTenant.realm
        testAsset.parentId == null

        when: "an asset is made public"
        testAsset.setAccessPublicRead(true)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.accessPublicRead

        when: "an asset is updated with a new parent in the authenticated realm"
        testAsset.setParentId(managerDemoSetup.groundFloorId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == managerDemoSetup.groundFloorId

        when: "an asset is made a root asset"
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == null

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, [managerDemoSetup.thingId])
        assetResource.get(null, managerDemoSetup.thingId)

        then: "the asset should not be found"
        WebApplicationException ex = thrown()
        ex.response.status == 404

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, [managerDemoSetup.apartment2LivingroomId])
        assetResource.get(null, managerDemoSetup.apartment2LivingroomId)

        then: "the asset should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, AttributeType.GEO_STREET.attributeName, Values.create("Teststreet 123").toJson())

        then: "result should match"
        def asset
        conditions.eventually {
            asset = assetResource.get(null, managerDemoSetup.smartOfficeId)
            assert asset.getAttribute(AttributeType.GEO_STREET).get().getValue().isPresent()
            assert asset.getAttribute(AttributeType.GEO_STREET).get().getValue().get().toJson() == Values.create("Teststreet 123").toJson()
        }

        when: "an non-existent assets attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, "doesnotexist", AttributeType.GEO_STREET.attributeName, Values.create("Teststreet 123").toJson())

        then: "the attribute should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an non-existent attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, "doesnotexist", Values.create("Teststreet 123").toJson())

        then: "the attribute should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset attribute is written in a foreign realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartBuildingId, AttributeType.GEO_STREET.attributeName, Values.create("Teststreet 456").toJson())

        then: "result should match"
        conditions.eventually {
            asset = assetResource.get(null, managerDemoSetup.smartBuildingId)
            assert asset.getAttribute(AttributeType.GEO_STREET).get().getValue().isPresent()
            assert asset.getAttribute(AttributeType.GEO_STREET).get().getValue().get().toJson() == Values.create("Teststreet 456").toJson()
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
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)

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
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartOfficeId

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(assets[0].id))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.groundFloorId

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartOfficeId
        assets[0].realm == keycloakDemoSetup.masterTenant.realm

        when: "the root assets of the given realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.tenantBuilding.realm))
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 0

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerDemoSetup.smartBuildingId))
        )

        then: "result should be empty"
        assets.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoThing = assetResource.get(null, managerDemoSetup.thingId)

        then: "result should match"
        demoThing.id == managerDemoSetup.thingId

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerDemoSetup.smartBuildingId)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in the authenticated realm"
        def testAsset = new Asset("Test Room", AssetType.ROOM)  // Note: no realm means auth realm
        testAsset = assetResource.create(null, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.wellKnownType == AssetType.ROOM
        testAsset.realm == keycloakDemoSetup.masterTenant.realm
        testAsset.parentId == null

        when: "an asset is updated with a new parent in the authenticated realm"
        testAsset.setParentId(managerDemoSetup.groundFloorId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == managerDemoSetup.groundFloorId

        when: "an asset is moved to a foreign realm and made a root asset"
        testAsset.setRealm(keycloakDemoSetup.tenantBuilding.realm)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is updated with a new parent in a foreign realm"
        testAsset.setParentId(managerDemoSetup.smartBuildingId)
        assetResource.update(null, testAsset.id, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, [managerDemoSetup.thingId])
        assetResource.get(null, managerDemoSetup.thingId)

        then: "the asset should not be found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, [managerDemoSetup.apartment2LivingroomId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, AttributeType.GEO_STREET.attributeName, Values.create("Teststreet 123").toJson())

        then: "result should match"
        conditions.eventually {
            def asset = assetResource.get(null, managerDemoSetup.smartOfficeId)
            assert asset.getAttribute(AttributeType.GEO_STREET).get().getValue().isPresent()
            assert asset.getAttribute(AttributeType.GEO_STREET).get().getValue().get().toJson() == Values.create("Teststreet 123").toJson()
        }

        when: "an asset attribute is written in a foreign realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartBuildingId, AttributeType.GEO_STREET.attributeName, Values.create("Teststreet 456").toJson())

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
                keycloakDemoSetup.tenantBuilding.realm,
                KEYCLOAK_CLIENT_ID,
                "testuser2",
                "testuser2"
        ).token

        and: "the asset resource"
        def assetResource = getClientApiTarget(serverUri(serverPort), keycloakDemoSetup.tenantBuilding.realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartBuildingId
        // Assets should not be completely loaded
        assets[0].path == null
        assets[0].attributesList.size() == 0

        when: "the root assets of a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 0

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerDemoSetup.smartBuildingId
        assets[0].realm == keycloakDemoSetup.tenantBuilding.realm

        when: "the root assets of the given realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.tenantCity.realm))
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 0

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerDemoSetup.thingId))
        )

        then: "result should be empty"
        assets.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoSmartBuilding = assetResource.get(null, managerDemoSetup.smartBuildingId)

        then: "result should match"
        demoSmartBuilding.id == managerDemoSetup.smartBuildingId

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerDemoSetup.thingId)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in a foreign realm"
        def testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.masterTenant.realm)
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
        testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.tenantBuilding.realm)
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, [managerDemoSetup.apartment2LivingroomId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, [managerDemoSetup.thingId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartBuildingId, AttributeType.GEO_STREET.attributeName, Values.create("Teststreet 123").toJson())

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset attribute is written in a foreign realm"
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, AttributeType.GEO_STREET.attributeName, Values.create("Teststreet 456").toJson())

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
                keycloakDemoSetup.tenantBuilding.realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "the asset resource"
        def assetResource = getClientApiTarget(serverUri(serverPort), keycloakDemoSetup.tenantBuilding.realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 6
        Asset apartment1 = assets[0]
        apartment1.id == managerDemoSetup.apartment1Id
        apartment1.name == "Apartment 1"
        apartment1.createdOn.getTime() < System.currentTimeMillis()
        apartment1.realm == keycloakDemoSetup.tenantBuilding.realm
        apartment1.type == AssetType.RESIDENCE.type
        apartment1.parentId == managerDemoSetup.smartBuildingId
        apartment1.path == null
        apartment1.attributesList.size() == 0

        Asset apartment1Livingroom = assets[1]
        apartment1Livingroom.id == managerDemoSetup.apartment1LivingroomId
        apartment1Livingroom.name == "Living Room 1"

        Asset apartment1Kitchen = assets[2]
        apartment1Kitchen.id == managerDemoSetup.apartment1KitchenId
        apartment1Kitchen.name == "Kitchen 1"

        Asset apartment1Hallway = assets[3]
        apartment1Hallway.id == managerDemoSetup.apartment1HallwayId
        apartment1Hallway.name == "Hallway 1"

        Asset apartment1Bedroom1 = assets[4]
        apartment1Bedroom1.id == managerDemoSetup.apartment1Bedroom1Id
        apartment1Bedroom1.name == "Bedroom 1"

        Asset apartment1Bathroom = assets[5]
        apartment1Bathroom.id == managerDemoSetup.apartment1BathroomId
        apartment1Bathroom.name == "Bathroom 1"

        when: "the root assets of a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 0

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 0

        when: "the root assets of the given realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.tenantCity.realm))
                        .parents(new ParentPredicate(true))
        )

        then: "result should match"
        assets.length == 0

        when: "the child assets of linked asset are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerDemoSetup.apartment1Id))
        )

        then: "result should match"
        assets.length == 5

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerDemoSetup.smartBuildingId))
        )

        then: "result should match"
        assets.length == 1

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerDemoSetup.thingId))
        )

        then: "result should be empty"
        assets.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        assetResource.get(null, managerDemoSetup.smartBuildingId)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "a user asset is retrieved by ID in the authenticated realm"
        apartment1Livingroom = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)

        then: "the restricted asset details should be available"
        apartment1Livingroom.id == managerDemoSetup.apartment1LivingroomId
        apartment1Livingroom.name == "Living Room 1"
        def resultAttributes = apartment1Livingroom.getAttributesList()
        resultAttributes.size() == 7
        def currentTemperature = apartment1Livingroom.getAttribute("currentTemperature").get()
        currentTemperature.getType().get() == AttributeValueType.TEMPERATURE
        !currentTemperature.getValue().isPresent()
        Meta resultMeta = currentTemperature.getMeta()
        resultMeta.size() == 7
        resultMeta.stream().filter(isMetaNameEqualTo(LABEL)).findFirst().get().getValueAsString().get() == "Current temperature"
        resultMeta.stream().filter(isMetaNameEqualTo(READ_ONLY)).findFirst().get().getValueAsBoolean().get()
        resultMeta.stream().filter(isMetaNameEqualTo(RULE_STATE)).findFirst().get().getValueAsBoolean().get()
        resultMeta.stream().filter(isMetaNameEqualTo(STORE_DATA_POINTS)).findFirst().get().getValueAsBoolean().get()
        resultMeta.stream().filter(isMetaNameEqualTo(SHOW_ON_DASHBOARD)).findFirst().get().getValueAsBoolean().get()
        resultMeta.stream().filter(isMetaNameEqualTo(FORMAT)).findFirst().get().getValueAsString().get() == "%0.1f° C"
        resultMeta.stream().filter(isMetaNameEqualTo(UNIT_TYPE)).findFirst().get().getValueAsString().get() == "CELSIUS"

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerDemoSetup.thingId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in a foreign realm"
        def testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.masterTenant.realm)
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is made a root asset in the authenticated realm"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)

        then: "the update should be ignored"
        assert testAsset.getParentId() == managerDemoSetup.apartment1Id

        when: "an asset is moved to a new parent in the authenticated realm"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)
        testAsset.setParentId(managerDemoSetup.apartment2Id)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)

        then: "the update should be ignored"
        assert testAsset.getParentId() == managerDemoSetup.apartment1Id

        when: "an asset is made public"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)
        testAsset.setAccessPublicRead(true)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)

        then: "the update should be ignored"
        assert !testAsset.isAccessPublicRead()

        when: "an asset is renamed"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)
        testAsset.setName("Ignored")
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)

        then: "the update should be ignored"
        assert testAsset.getName() == "Living Room 1"

        when: "an asset is created in the authenticated realm"
        testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.tenantBuilding.realm)
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, [managerDemoSetup.apartment1LivingroomId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, [managerDemoSetup.thingId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a private asset attribute is written on a user asset"
        assetResource.writeAttributeValue(null, managerDemoSetup.apartment1LivingroomId, "lightSwitch", Values.create(false).toJson())

        then: "the attribute should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "a restricted read-only asset attribute is written on a user asset"
        assetResource.writeAttributeValue(null, managerDemoSetup.apartment1LivingroomId, "currentTemperature", Values.create(22.123d).toJson())

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.status == 403

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
        assetResource.writeAttributeValue(null, managerDemoSetup.smartOfficeId, AttributeType.GEO_STREET.attributeName, Values.create("Teststreet 123").toJson())

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a non-writable attribute value is written on a user asset"
        assetResource.writeAttributeValue(null, managerDemoSetup.apartment1KitchenId, "presenceDetected", Values.create(true).toJson())

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a non-writable attribute value is updated on a user asset"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        testAsset.getAttribute("presenceDetected").get().setValue(Values.create(true))
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)

        then: "the update should be ignored"
        assert !testAsset.getAttribute("presenceDetected").get().getValue().isPresent()

        when: "a non-writable attribute is updated on a user asset"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        testAsset.replaceAttribute(new AssetAttribute("presenceDetected", BOOLEAN, Values.create(true)))
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)

        then: "the update should be ignored"
        assert !testAsset.getAttribute("presenceDetected").get().getValue().isPresent()

        when: "a non-writable attribute's meta item value is updated"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        testAsset.getAttribute("presenceDetected").orElse(null).getMetaItem(STORE_DATA_POINTS).orElse(null).setValue(Values.create(false))
        assetResource.update(null, testAsset.id, testAsset)

        then: "the update should be ignored"
        assert testAsset.getAttribute("presenceDetected").get().getMetaItem(STORE_DATA_POINTS).get().getValue().get()

        when: "a non-writable attribute's meta item value is added"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        testAsset.getAttribute("presenceDetected").orElse(null).addMeta(new MetaItem(ABOUT, Values.create("Ignored")))
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)

        then: "the update should be ignored"
        assert !testAsset.getAttribute("presenceDetected").get().getMetaItem(ABOUT).isPresent()

        when: "a new attribute is added on a user asset"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        testAsset.addAttributes(new AssetAttribute("myCustomAttribute", NUMBER, Values.create(123)))
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)

        then: "result should match"
        assert testAsset.getAttribute("myCustomAttribute").get().getValue().get() == Values.create(123)

        when: "a writable attribute value is written on a user asset"
        assetResource.writeAttributeValue(null, managerDemoSetup.apartment1KitchenId, "myCustomAttribute", Values.create(456).toJson())

        then: "result should match"
        conditions.eventually {
            testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
            assert testAsset.getAttribute("myCustomAttribute").get().getValue().get() == Values.create(456)
        }

        when: "a writable attribute has a non-writable meta item"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        testAsset.getAttribute("myCustomAttribute").get().addMeta(new MetaItem(PROTOCOL_CONFIGURATION, Values.create("Ignored")))
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        assetResource.update(null, testAsset.id, testAsset)

        then: "the update should be ignored"
        assert !testAsset.getAttribute("presenceDetected").get().getMetaItem(PROTOCOL_CONFIGURATION).isPresent()

        when: "a writable attribute has a writable meta item"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        testAsset.getAttribute("myCustomAttribute").get().addMeta(new MetaItem(LABEL, Values.create("My label")))
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)

        then: "the result should match"
        assert testAsset.getAttribute("myCustomAttribute").get().getMetaItem(LABEL).get().getValue().get() == Values.create("My label")

        when: "a writable attribute has a writable meta item value update"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        testAsset.getAttribute("myCustomAttribute").get().getMetaItem(LABEL).get().setValue(Values.create("My label update"))
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)

        then: "the result should match"
        assert testAsset.getAttribute("myCustomAttribute").get().getMetaItem(LABEL).get().getValue().get() == Values.create("My label update")

        when: "a writable attribute replaces a writable meta item with several new items"
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)
        testAsset.getAttribute("myCustomAttribute").get().getMeta().removeIf(isMetaNameEqualTo(LABEL))
        testAsset.getAttribute("myCustomAttribute").get().addMeta(
                new MetaItem(LABEL, Values.create("Label1")),
                new MetaItem(LABEL, Values.create("Label2")),
                new MetaItem(LABEL, Values.create("Label3")),
        )
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1KitchenId)

        then: "the result should match"
        assert testAsset.getAttribute("myCustomAttribute").get().getMetaItems(LABEL.getUrn()).length == 3
        assert testAsset.getAttribute("myCustomAttribute").get().getMetaItems(LABEL.getUrn())[0].getValue().get() == Values.create("Label1")
        assert testAsset.getAttribute("myCustomAttribute").get().getMetaItems(LABEL.getUrn())[1].getValue().get() == Values.create("Label2")
        assert testAsset.getAttribute("myCustomAttribute").get().getMetaItems(LABEL.getUrn())[2].getValue().get() == Values.create("Label3")

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
