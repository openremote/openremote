package org.openremote.test.assets

import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.shared.asset.AssetResource
import org.openremote.model.asset.Asset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import org.openremote.model.asset.AssetType
import org.openremote.container.util.IdentifierUtil

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

        and: "an authenticated admin user"
        def realm = MASTER_REALM
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource"
        def client = createClient(container).build()
        def serverUri = serverUri(serverPort)
        def assetResource = getClientTarget(client, serverUri, realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assetInfos = assetResource.getHomeAssets(null);

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.smartOfficeId

        when: "the root assets of the authenticated realm are retrieved"
        assetInfos = assetResource.getRoot(null, MASTER_REALM);

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.smartOfficeId

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assetInfos = assetResource.getChildren(null, assetInfos[0].id)

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.groundFloorId

        when: "the root assets of the authenticated realm are retrieved"
        assetInfos = assetResource.getRoot(null, null);

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.smartOfficeId

        when: "the root assets of the given realm are retrieved"
        assetInfos = assetResource.getRoot(null, "customerA");

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.smartHomeId

        when: "the child assets of an asset in a foreign realm are retrieved"
        assetInfos = assetResource.getChildren(null, assetInfos[0].id)

        then: "result should match"
        assetInfos.length == 3
        // Should be ordered by creation time
        assetInfos[0].id == managerDemoSetup.apartment1Id
        assetInfos[1].id == managerDemoSetup.apartment2Id
        assetInfos[2].id == managerDemoSetup.apartment3Id

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoThing = assetResource.get(null, managerDemoSetup.thingId);

        then: "result should match"
        demoThing.id == managerDemoSetup.thingId

        when: "an asset is retrieved by ID in a foreign realm"
        def demoSmartHome = assetResource.get(null, managerDemoSetup.smartHomeId);

        then: "result should match"
        demoSmartHome.id == managerDemoSetup.smartHomeId

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in the authenticated realm"
        def testAsset = new Asset(MASTER_REALM, "Test Room", AssetType.ROOM)
        testAsset.setId(IdentifierUtil.generateGlobalUniqueId())
        assetResource.create(null, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.wellKnownType == AssetType.ROOM
        testAsset.realm == MASTER_REALM
        testAsset.parentId == null

        when: "an asset is updated with a new parent in the authenticated realm"
        testAsset.setParentId(managerDemoSetup.groundFloorId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == managerDemoSetup.groundFloorId

        when: "an asset is moved to a foreign realm and made a root asset"
        testAsset.setRealm("customerA")
        testAsset.setParentId(null)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.realm == "customerA"
        testAsset.parentId == null

        when: "an asset is updated with a new parent in a foreign realm"
        testAsset.setParentId(managerDemoSetup.smartHomeId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.realm == "customerA"
        testAsset.parentId == managerDemoSetup.smartHomeId

        when: "an asset is deleted in the authenticated realm"
        assetResource.delete(null, managerDemoSetup.thingId)
        testAsset = assetResource.get(null, managerDemoSetup.thingId)

        then: "the asset should not be found"
        WebApplicationException ex = thrown()
        ex.response.status == 404

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, managerDemoSetup.apartment1LivingroomId)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)

        then: "the asset should not be found"
        ex = thrown()
        ex.response.status == 404

        cleanup: "the server should be stopped"
        stopContainer(container);
    }

    def "Access assets as testuser1"() {

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
        def assetResource = getClientTarget(client, serverUri, realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assetInfos = assetResource.getHomeAssets(null);

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.smartOfficeId

        when: "the root assets of the authenticated realm are retrieved"
        assetInfos = assetResource.getRoot(null, MASTER_REALM);

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.smartOfficeId

        when: "the child assets of an asset in the authenticated realm are retrieved"
        assetInfos = assetResource.getChildren(null, assetInfos[0].id)

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.groundFloorId

        when: "the root assets of the authenticated realm are retrieved"
        assetInfos = assetResource.getRoot(null, null);

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.smartOfficeId
        assetInfos[0].realm == MASTER_REALM

        when: "the root assets of the given realm are retrieved"
        assetInfos = assetResource.getRoot(null, "customerA");

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "the child assets of an asset in a foreign realm are retrieved"
        assetInfos = assetResource.getChildren(null, managerDemoSetup.smartHomeId)

        then: "result should be empty"
        assetInfos.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoThing = assetResource.get(null, managerDemoSetup.thingId);

        then: "result should match"
        demoThing.id == managerDemoSetup.thingId

        when: "an asset is retrieved by ID in a foreign realm"
        def demoBuilding = assetResource.get(null, managerDemoSetup.smartHomeId);

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in the master realm"
        def testAsset = new Asset(MASTER_REALM, "Test Room", AssetType.ROOM)
        testAsset.setId(IdentifierUtil.generateGlobalUniqueId())
        assetResource.create(null, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.wellKnownType == AssetType.ROOM
        testAsset.realm == MASTER_REALM
        testAsset.parentId == null

        when: "an asset is updated with a new parent in the authenticated realm"
        testAsset.setParentId(managerDemoSetup.groundFloorId)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should be updated"
        testAsset.parentId == managerDemoSetup.groundFloorId

        when: "an asset is moved to a foreign realm and made a root asset"
        testAsset.setRealm("customerA")
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
        testAsset = assetResource.get(null, managerDemoSetup.thingId)

        then: "the asset should not be found"
        ex = thrown()
        ex.response.status == 404

        when: "an asset is deleted in a foreign realm"
        assetResource.delete(null, managerDemoSetup.apartment1LivingroomId)
        testAsset = assetResource.get(null, managerDemoSetup.apartment1LivingroomId)

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        cleanup: "the server should be stopped"
        stopContainer(container);
    }

    def "Access assets as testuser2"() {
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
        def assetResource = getClientTarget(client, serverUri, realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assetInfos = assetResource.getHomeAssets(null);

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.smartHomeId
        assetInfos[0].realm == "customerA"

        when: "the root assets of a foreign realm are retrieved"
        assetInfos = assetResource.getRoot(null, MASTER_REALM);

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "the root assets of the authenticated realm are retrieved"
        assetInfos = assetResource.getRoot(null, null);

        then: "result should match"
        assetInfos.length == 1
        assetInfos[0].id == managerDemoSetup.smartHomeId
        assetInfos[0].realm == "customerA"

        when: "the root assets of the given realm are retrieved"
        assetInfos = assetResource.getRoot(null, "customerB");

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "the child assets of an asset in a foreign realm are retrieved"
        assetInfos = assetResource.getChildren(null, managerDemoSetup.thingId)

        then: "result should be empty"
        assetInfos.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoSmartHome = assetResource.get(null, managerDemoSetup.smartHomeId);

        then: "result should match"
        demoSmartHome.id == managerDemoSetup.smartHomeId

        when: "an asset is retrieved by ID in a foreign realm"
        def demoThing = assetResource.get(null, managerDemoSetup.thingId);

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        /* ############################################## WRITE ####################################### */

        when: "an asset is created in a foreign realm"
        def testAsset = new Asset(MASTER_REALM, "Test Room", AssetType.ROOM)
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
        testAsset = new Asset("customerA", "Test Room", AssetType.ROOM)
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
        stopContainer(container);
    }

    def "Access assets as testuser3"() {
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
        def assetResource = getClientTarget(client, serverUri, realm, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        when: "the home assets of the authenticated user are retrieved"
        def assetInfos = assetResource.getHomeAssets(null);

        then: "result should match"
        assetInfos.length == 2
        assetInfos[0].id == managerDemoSetup.apartment1Id
        assetInfos[1].id == managerDemoSetup.apartment2Id
        assetInfos[0].realm == "customerA"

        when: "the root assets of a foreign realm are retrieved"
        assetInfos = assetResource.getRoot(null, MASTER_REALM);

        then: "access should be forbidden"
        WebApplicationException ex = thrown()
        ex.response.status == 403

        when: "the root assets of the authenticated realm are retrieved"
        assetInfos = assetResource.getRoot(null, null);

        then: "result should match"
        assetInfos.length == 0

        when: "the root assets of the given realm are retrieved"
        assetInfos = assetResource.getRoot(null, "customerB");

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "the child assets of an asset in a foreign realm are retrieved"
        assetInfos = assetResource.getChildren(null, managerDemoSetup.thingId)

        then: "result should be empty"
        assetInfos.length == 0

        when: "an asset is retrieved by ID in the authenticated realm"
        def demoSmartHome = assetResource.get(null, managerDemoSetup.smartHomeId);

        then: "result should match"
        demoSmartHome.id == managerDemoSetup.smartHomeId
/* TODO
        when: "an asset is retrieved by ID in a foreign realm"
        def demoThing = assetResource.get(null, managerDemoSetup.thingId);

        then: "access should be forbidden"
        ex = thrown()
        ex.response.status == 403
*/
        /* ############################################## WRITE ####################################### */
/*
        when: "an asset is created in a foreign realm"
        def testAsset = new Asset(MASTER_REALM, "Test Room", AssetType.ROOM)
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
        testAsset = new Asset("customerA", "Test Room", AssetType.ROOM)
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
*/
        cleanup: "the server should be stopped"
        stopContainer(container);
    }
}
