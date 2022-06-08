package org.openremote.test.assets

import org.openremote.manager.setup.SetupService
import org.openremote.model.attribute.AttributeState
import org.openremote.model.attribute.AttributeWriteFailure
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.ParentPredicate
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.BadRequestException
import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.value.MetaItemType.*
import static org.openremote.model.value.ValueType.BOOLEAN
import static org.openremote.model.value.ValueType.NUMBER

class AssetPermissionsTest extends Specification implements ManagerContainerTrait {

    def "Access assets as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def conditions = new PollingConditions(delay: 0.2, timeout: 5)

        and: "an authenticated admin user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
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
                        .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                        .parents(new ParentPredicate(null))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerTestSetup.smartOfficeId

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(assets[0].id))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerTestSetup.groundFloorId

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                        .parents(new ParentPredicate(null))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerTestSetup.smartOfficeId

        when: "the root assets of the given realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name))
                        .parents(new ParentPredicate(null))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerTestSetup.smartBuildingId

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(assets[0].id))
        )

        then: "result should match"
        assets.length == 3
        // Should be ordered by creation time
        assets[0].id == managerTestSetup.apartment1Id
        assets[1].id == managerTestSetup.apartment2Id
        assets[2].id == managerTestSetup.apartment3Id

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoThing = assetResource.get(null, managerTestSetup.thingId)

        then: "result should match"
        demoThing.id == managerTestSetup.thingId

        when: "an asset is retrieved by ID in a foreign realm"
        def demoSmartBuilding = assetResource.get(null, managerTestSetup.smartBuildingId)

        then: "result should match"
        demoSmartBuilding.id == managerTestSetup.smartBuildingId

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in the authenticated realm"
        def testAsset = new RoomAsset("Test Room")
            .setRealm(keycloakTestSetup.realmMaster.name)

        testAsset = assetResource.create(null, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.type == RoomAsset.DESCRIPTOR.getName()
        testAsset.realm == keycloakTestSetup.realmMaster.name
        testAsset.parentId == null

        when: "an asset is made public"
        testAsset.setAccessPublicRead(true)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.accessPublicRead

        when: "an asset is updated with a new parent in the authenticated realm"
        testAsset.setParentId(managerTestSetup.groundFloorId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == managerTestSetup.groundFloorId

        when: "an asset is made a root asset"
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == null

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, [managerTestSetup.thingId])
        assetResource.get(null, managerTestSetup.thingId)

        then: "the asset should not be found"
        WebApplicationException ex = thrown()
        ex.response.status == 404

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, [managerTestSetup.apartment2LivingroomId])
        assetResource.get(null, managerTestSetup.apartment2LivingroomId)

        then: "the asset should be not found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerTestSetup.smartOfficeId, BuildingAsset.STREET.name, '"Teststreet 123"')

        then: "result should match"
        BuildingAsset asset
        conditions.eventually {
            asset = assetResource.get(null, managerTestSetup.smartOfficeId) as BuildingAsset
            assert asset.getStreet().isPresent()
            assert asset.getStreet().get() == "Teststreet 123"
        }

        when: "an non-existent assets attribute is written in the authenticated realm"
        def response = assetResource.writeAttributeValue(null, "doesnotexist", BuildingAsset.STREET.name, '"Teststreet 123"')

        then: "the attribute should be not found"
        response.status == 404

        when: "an non-existent attribute is written in the authenticated realm"
        response = assetResource.writeAttributeValue(null, managerTestSetup.smartOfficeId, "doesnotexist", '"Teststreet 123"')

        then: "the attribute should be not found"
        response.status == 404

        when: "an asset attribute is written in a foreign realm"
        assetResource.writeAttributeValue(null, managerTestSetup.smartBuildingId, BuildingAsset.STREET.name, '"Teststreet 456"')

        then: "result should match"
        conditions.eventually {
            asset = assetResource.get(null, managerTestSetup.smartBuildingId) as BuildingAsset
            assert asset.getStreet().isPresent()
            assert asset.getStreet().get() == "Teststreet 456"
        }
    }

    def "Access assets as testuser1"() {

        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def conditions = new PollingConditions(delay: 0.2, timeout: 10)

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

        when: "the assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match (all assets in the master realm as user is not restricted)"
        assets.length == 5
        assets.find {it.id == managerTestSetup.smartOfficeId}.attributes.size() == 7
        // Assets should not be completely loaded (no path or parent info)
        assets.find {it.id == managerTestSetup.smartOfficeId}.path == null
        assets.find {it.id == managerTestSetup.smartOfficeId}.parentId == null

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                        .parents(new ParentPredicate(null))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerTestSetup.smartOfficeId

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(assets[0].id))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerTestSetup.groundFloorId

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(null))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerTestSetup.smartOfficeId
        assets[0].realm == keycloakTestSetup.realmMaster.name

        when: "the root assets of a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name))
                        .parents(new ParentPredicate(null))
        )

        then: "a bad request exception should be thrown"
        WebApplicationException ex = thrown()
        assert ex instanceof BadRequestException

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerTestSetup.smartBuildingId))
        )

        then: "result should be empty"
        assets.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoThing = assetResource.get(null, managerTestSetup.thingId)

        then: "result should match"
        demoThing.id == managerTestSetup.thingId

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerTestSetup.smartBuildingId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in the authenticated realm"
        def testAsset = new RoomAsset("Test Room").setRealm(MASTER_REALM)
        testAsset = assetResource.create(null, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.type == RoomAsset.DESCRIPTOR.getName()
        testAsset.realm == keycloakTestSetup.realmMaster.name
        testAsset.parentId == null

        when: "an asset is updated with a new parent in the authenticated realm"
        testAsset.setParentId(managerTestSetup.groundFloorId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == managerTestSetup.groundFloorId

        when: "an asset is moved to a foreign realm and made a root asset"
        testAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is updated with a new parent in a foreign realm"
        testAsset.setParentId(managerTestSetup.smartBuildingId)
        assetResource.update(null, testAsset.id, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, [managerTestSetup.thingId])
        assetResource.get(null, managerTestSetup.thingId)

        then: "the asset should not be found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, [managerTestSetup.apartment2LivingroomId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset attribute is written in the authenticated realm"
        assetResource.writeAttributeValue(null, managerTestSetup.smartOfficeId, BuildingAsset.STREET.name, '"Teststreet 123"')

        then: "result should match"
        conditions.eventually {
            def asset = assetResource.get(null, managerTestSetup.smartOfficeId) as BuildingAsset
            assert asset.getStreet().orElse(null) == "Teststreet 123"
        }

        when: "an asset attribute is written in a foreign realm"
        def response = assetResource.writeAttributeValue(null, managerTestSetup.smartBuildingId, BuildingAsset.STREET.name, '"Teststreet 456"')

        then: "access should be forbidden"
        response.status == 403
    }

    def "Access assets as testuser2"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                "testuser2",
                "testuser2"
        ).token

        and: "the asset resource"
        def assetResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match (all assets in the building realm as user is not restricted)"
        assets.length == 13
        assets.find {it.id == managerTestSetup.smartBuildingId}.attributes.size() == 7
        // Assets should not be completely loaded (no path or parent info)
        assets.find {it.id == managerTestSetup.smartBuildingId}.path == null
        assets.find {it.id == managerTestSetup.smartBuildingId}.parentId == null

        when: "the root assets of a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                        .parents(new ParentPredicate(null))
        )

        then: "a bad request exception should be thrown"
        thrown(BadRequestException)

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(null))
        )

        then: "result should match"
        assets.length == 1
        assets[0].id == managerTestSetup.smartBuildingId
        assets[0].realm == keycloakTestSetup.realmBuilding.name

        when: "the root assets of a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmCity.name))
                        .parents(new ParentPredicate(null))
        )

        then: "result should match"
        thrown(BadRequestException)

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerTestSetup.thingId))
        )

        then: "result should be empty"
        assets.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoSmartBuilding = assetResource.get(null, managerTestSetup.smartBuildingId)

        then: "result should match"
        demoSmartBuilding.id == managerTestSetup.smartBuildingId

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerTestSetup.thingId)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in a foreign realm"
        def testAsset = new RoomAsset("Test Room")
            .setRealm(keycloakTestSetup.realmMaster.name)
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is made a root asset in the authenticated realm"
        testAsset = assetResource.get(null, managerTestSetup.apartment1Id)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is created in the authenticated realm"
        testAsset = new RoomAsset("Test Room")
            .setRealm(keycloakTestSetup.realmBuilding.name)
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, [managerTestSetup.apartment2LivingroomId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, [managerTestSetup.thingId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset attribute is written in the authenticated realm"
        def response = assetResource.writeAttributeValue(null, managerTestSetup.smartBuildingId, BuildingAsset.STREET.name, '"Teststreet 123"')

        then: "access should be forbidden"
        response.status == 403

        when: "an asset attribute is written in a foreign realm"
        response = assetResource.writeAttributeValue(null, managerTestSetup.smartOfficeId, BuildingAsset.STREET.name, '"Teststreet 456"')

        then: "access should be forbidden"
        response.status == 403
    }

    def "Access assets as testuser3 (restricted user)"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def conditions = new PollingConditions(delay: 0.2, timeout: 5)

        and: "an authenticated test user"
        def accessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "the asset resource"
        def assetResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the assets of the authenticated user are retrieved"
        def assets = assetResource.getCurrentUserAssets(null)

        then: "result should match"
        assets.length == 6
        Asset apartment1 = assets[0]
        apartment1.id == managerTestSetup.apartment1Id
        apartment1.name == "Apartment 1"
        apartment1.createdOn.getTime() < System.currentTimeMillis()
        apartment1.realm == keycloakTestSetup.realmBuilding.name
        apartment1.type == BuildingAsset.DESCRIPTOR.getName()
        apartment1.parentId == managerTestSetup.smartBuildingId
        apartment1.path[0] == managerTestSetup.smartBuildingId
        apartment1.path[1] == managerTestSetup.apartment1Id
        apartment1.attributes.size() == 12

        Asset apartment1Livingroom = assets[1]
        apartment1Livingroom.id == managerTestSetup.apartment1LivingroomId
        apartment1Livingroom.name == "Living Room 1"

        Asset apartment1Kitchen = assets[2]
        apartment1Kitchen.id == managerTestSetup.apartment1KitchenId
        apartment1Kitchen.name == "Kitchen 1"

        Asset apartment1Hallway = assets[3]
        apartment1Hallway.id == managerTestSetup.apartment1HallwayId
        apartment1Hallway.name == "Hallway 1"

        Asset apartment1Bedroom1 = assets[4]
        apartment1Bedroom1.id == managerTestSetup.apartment1Bedroom1Id
        apartment1Bedroom1.name == "Bedroom 1"

        Asset apartment1Bathroom = assets[5]
        apartment1Bathroom.id == managerTestSetup.apartment1BathroomId
        apartment1Bathroom.name == "Bathroom 1"

        when: "the root assets of a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                        .parents(new ParentPredicate(null))
        )

        then: "a bad request exception should be thrown"
        thrown(BadRequestException)

        when: "the root assets of the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(null))
        )

        then: "result should match"
        assets.length == 0

        when: "the root assets of a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .realm(new RealmPredicate(keycloakTestSetup.realmCity.name))
                        .parents(new ParentPredicate(null))
        )

        then: "a bad request exception should be thrown"
        thrown(BadRequestException)

        when: "the child assets of linked asset are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerTestSetup.apartment1Id))
        )

        then: "result should match"
        assets.length == 5

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerTestSetup.smartBuildingId))
        )

        then: "result should match"
        assets.length == 1

        when: "the child assets of an asset in a foreign realm are retrieved"
        assets = assetResource.queryAssets(null,
                new AssetQuery()
                        .parents(new ParentPredicate(managerTestSetup.thingId))
        )

        then: "result should be empty"
        assets.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        assetResource.get(null, managerTestSetup.smartBuildingId)

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "a user asset is retrieved by ID in the authenticated realm"
        apartment1Livingroom = assetResource.get(null, managerTestSetup.apartment1LivingroomId)

        then: "the restricted asset details should be available"
        apartment1Livingroom.id == managerTestSetup.apartment1LivingroomId
        apartment1Livingroom.name == "Living Room 1"
        def resultAttributes = apartment1Livingroom.getAttributes()
        resultAttributes.size() == 7
        def currentTemperatureAttr = apartment1Livingroom.getAttribute("currentTemperature", NUMBER.type).orElse(null)
        currentTemperatureAttr.getType() == NUMBER
        !currentTemperatureAttr.getValue().isPresent()

        MetaMap resultMeta = currentTemperatureAttr.getMeta()
        resultMeta.size() == 8
        resultMeta.getValueOrDefault(LABEL) == "Current temperature"
        resultMeta.getValue(READ_ONLY).orElse(false)
        resultMeta.has(AGENT_LINK)
        resultMeta.getValueOrDefault(ACCESS_RESTRICTED_READ)
        resultMeta.getValueOrDefault(UNITS) != null
        resultMeta.getValueOrDefault(UNITS)[0] == UNITS_CELSIUS

        when: "an asset is retrieved by ID in a foreign realm"
        assetResource.get(null, managerTestSetup.thingId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "all linked assets of the user are retrieved"
        assets = assetResource.queryAssets(null, null)

        then: "result should contain all linked assets"
        assets.length == 6

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in a foreign realm"
        def testAsset = new RoomAsset("Test Room")
            .setRealm(keycloakTestSetup.realmMaster.name)
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is made a root asset in the authenticated realm"
        testAsset = assetResource.get(null, managerTestSetup.apartment1LivingroomId)
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerTestSetup.apartment1LivingroomId)

        then: "the update should be ignored"
        assert testAsset.getParentId() == managerTestSetup.apartment1Id

        when: "an asset is moved to a new parent in the authenticated realm"
        testAsset = assetResource.get(null, managerTestSetup.apartment1LivingroomId)
        testAsset.setParentId(managerTestSetup.apartment2Id)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerTestSetup.apartment1LivingroomId)

        then: "the update should be ignored"
        assert testAsset.getParentId() == managerTestSetup.apartment1Id

        when: "an asset is made public"
        testAsset = assetResource.get(null, managerTestSetup.apartment1LivingroomId)
        testAsset.setAccessPublicRead(true)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerTestSetup.apartment1LivingroomId)

        then: "the update should be ignored"
        assert !testAsset.isAccessPublicRead()

        when: "an asset is renamed"
        testAsset = assetResource.get(null, managerTestSetup.apartment1LivingroomId)
        testAsset.setName("Ignored")
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerTestSetup.apartment1LivingroomId)

        then: "the update should be ignored"
        assert testAsset.getName() == "Living Room 1"

        when: "an asset is created in the authenticated realm"
        testAsset = new RoomAsset("Test Room")
            .setRealm(keycloakTestSetup.realmBuilding.name)
        assetResource.create(null, testAsset)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, [managerTestSetup.apartment1LivingroomId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, [managerTestSetup.thingId])

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "a private asset attribute is written on a user asset"
        def response = assetResource.writeAttributeValue(null, managerTestSetup.apartment1LivingroomId, "lightSwitch", false)

        then: "the attribute should be not found"
        response.status == 404

        when: "a restricted read-only asset attribute is written on a user asset"
        response = assetResource.writeAttributeValue(null, managerTestSetup.apartment1LivingroomId, "currentTemperature", 22.123)

        then: "the request should be forbidden"
        response.status == 403

        when: "an attribute is written on a non-existent user asset"
        response = assetResource.writeAttributeValue(null, "doesnotexist", "lightSwitch", false)

        then: "the attribute should be not found"
        response.status == 404

        when: "an non-existent attribute is written on a user asset"
        response = assetResource.writeAttributeValue(null, managerTestSetup.apartment1LivingroomId, "doesnotexist", '"foo"')

        then: "the attribute should be not found"
        response.status == 404

        when: "an asset attribute is written on a non-user asset"
        response = assetResource.writeAttributeValue(null, managerTestSetup.apartment3LivingroomId, "lightSwitch", false)

        then: "access should be forbidden"
        response.status == 403

        when: "an asset attribute is written in a foreign realm"
        response = assetResource.writeAttributeValue(null, managerTestSetup.smartOfficeId, BuildingAsset.STREET.name, '"Teststreet 123"')

        then: "access should be forbidden"
        response.status == 403

        when: "a non-writable attribute value is written on a user asset"
        response = assetResource.writeAttributeValue(null, managerTestSetup.apartment1KitchenId, "presenceDetected", true)

        then: "access should be forbidden"
        response.status == 403

        when: "a non-writable attribute value is updated on a user asset"
        testAsset = assetResource.get(null, managerTestSetup.apartment1KitchenId)
        testAsset.getAttribute("presenceDetected").get().setValue(true)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerTestSetup.apartment1KitchenId)

        then: "the update should be ignored"
        assert !testAsset.getAttribute("presenceDetected").get().getValue().isPresent()

        when: "a non-writable attribute is updated on a user asset"
        testAsset = assetResource.get(null, managerTestSetup.apartment1KitchenId)
        testAsset.addOrReplaceAttributes(new Attribute<>("presenceDetected", BOOLEAN, true))
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerTestSetup.apartment1KitchenId)

        then: "the update should be ignored"
        assert !testAsset.getAttribute("presenceDetected").get().getValue().isPresent()

        when: "a new attribute is added on a user asset"
        testAsset = assetResource.get(null, managerTestSetup.apartment1KitchenId)
        testAsset.addAttributes(new Attribute<>("myCustomAttribute", NUMBER, 123d).addMeta(new MetaItem<>(LABEL, "Label")))
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerTestSetup.apartment1KitchenId)

        then: "result should match"
        assert testAsset.getAttribute("myCustomAttribute").get().getValue().get() == 123

        when: "a writable attribute value is written on a user asset"
        assetResource.writeAttributeValue(null, managerTestSetup.apartment1KitchenId, "myCustomAttribute", 456)

        then: "result should match"
        conditions.eventually {
            testAsset = assetResource.get(null, managerTestSetup.apartment1KitchenId) as RoomAsset
            assert testAsset.getAttribute("myCustomAttribute").get().getValue().get() == 456
        }

        when: "a writable attribute has a meta item value update"
        testAsset = assetResource.get(null, managerTestSetup.apartment1KitchenId)
        testAsset.getAttribute("myCustomAttribute").get().getMetaItem(LABEL).get().setValue("My label update")
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, managerTestSetup.apartment1KitchenId)

        then: "the result should match"
        assert testAsset.getAttribute("myCustomAttribute").get().getMetaItem(LABEL).get().getValue().get() == "My label update"
    }

    def "Access assets as anonymous user"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "the asset resource"
        def assetResource = getClientApiTarget(serverUri(serverPort), keycloakTestSetup.realmBuilding.name).proxy(AssetResource.class)

        when: "the public assets are retrieved"
        def assets = assetResource.queryAssets(null, new AssetQuery()
                .realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name)))

        then: "the public assets should be retrieved"
        assert assets.size() == 2
        assert assets.find {it.id == managerTestSetup.apartment1Id} != null
        assert assets.find {it.id == managerTestSetup.apartment2LivingroomId} != null

        when: "the public assets are retrieved without a query"
        assets = assetResource.queryAssets(null, null)

        then: "the public assets should be retrieved"
        assert assets.size() == 2
        assert assets.find {it.id == managerTestSetup.apartment1Id} != null
        assert assets.find {it.id == managerTestSetup.apartment2LivingroomId} != null

        when: "an attribute with public write is written to and another with only public read"
        def writeResults = assetResource.writeAttributeValues(null, [
            new AttributeState(managerTestSetup.apartment1Id, Asset.LOCATION.name, null),
            new AttributeState(managerTestSetup.apartment2LivingroomId, Asset.LOCATION.name, null)
        ] as AttributeState[])

        then: "the request should have succeeded but only the apartment1 location event should have been successful"
        assert writeResults.size() == 2
        assert writeResults.find {it.ref.id == managerTestSetup.apartment1Id && it.ref.name == Asset.LOCATION.name}.failure == null
        assert writeResults.find {it.ref.id == managerTestSetup.apartment2LivingroomId && it.ref.name == Asset.LOCATION.name}.failure == AttributeWriteFailure.INSUFFICIENT_ACCESS
    }
}
