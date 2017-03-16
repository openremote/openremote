package org.openremote.test.assets

import org.openremote.container.util.IdentifierUtil
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.asset.AssetResource
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AssetIntegrityTest extends Specification implements ManagerContainerTrait {

    def "Test asset changes as superuser"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        and: "an authenticated admin user"
        def realm = MASTER_REALM
        def realmId = getActiveTenantRealmId(container, realm)
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

        when: "an asset is created in the authenticated realm"
        def testAsset = new Asset(realmId, "Test Room", AssetType.ROOM)
        testAsset.setId(IdentifierUtil.generateGlobalUniqueId())
        assetResource.create(null, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.wellKnownType == AssetType.ROOM
        testAsset.realmId == realmId
        testAsset.parentId == null

        when: "an asset is updated with a different type"
        testAsset.setType(AssetType.BUILDING)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should not be updated"
        testAsset.wellKnownType == AssetType.ROOM

        when: "an asset is updated with a non-existent realm"
        testAsset.setRealmId("thisdoesnotexistitreallydoesnt")
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be bad"
        WebApplicationException ex = thrown()
        ex.response.status == 400

        when: "an asset is updated with a non-existent parent"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setParentId("thisdoesnotexistitreallydoesnt")
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset is updated with itself as a parent"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setParentId(testAsset.getId())
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset is deleted but has children"
        assetResource.delete(null, managerDemoSetup.apartment1Id)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
