package org.openremote.test.assets

import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetType
import org.openremote.model.attribute.AttributeValueType
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AssetIntegrityTest extends Specification implements ManagerContainerTrait {

    def "Test asset changes as superuser"() {
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
                getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource"
        def serverUri = serverUri(serverPort)
        def assetResource = getClientApiTarget(serverUri, MASTER_REALM, accessToken).proxy(AssetResource.class)

        when: "an asset is created in the authenticated realm"
        def testAsset = new Asset("Test Room", AssetType.ROOM, null, keycloakDemoSetup.masterTenant.realm)
        testAsset = assetResource.create(null, testAsset)

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.wellKnownType == AssetType.ROOM
        testAsset.realm == keycloakDemoSetup.masterTenant.realm
        testAsset.parentId == null

        when: "an asset is stored with an illegal attribute name"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setAttributes(
            new AssetAttribute(testAsset.id, "illegal- Attribute:name&&&", AttributeValueType.STRING)
        )

        assetResource.update(null, testAsset.getId(), testAsset)

        then: "the request should be bad"
        WebApplicationException ex = thrown()
        ex.response.status == 400

        when: "an asset is stored with a non-empty attribute value"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setAttributes(
                new AssetAttribute("foo", AttributeValueType.STRING, Values.create("bar"), getClockTimeOf(container))
        )
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the attribute should exist"
        testAsset.getAttribute("foo").isPresent()
        testAsset.getAttribute("foo").get().getValueAsString().get() == "bar"

        when: "an asset attribute value is written directly"
        assetResource.writeAttributeValue(null, testAsset.getId(), "foo", "\"bar2\"")

        then: "the attribute value should match"
        new PollingConditions(delay: 1, timeout: 5).eventually {
            def asset = assetResource.get(null, testAsset.getId())
            assert asset.getAttribute("foo").get().getValueAsString().get() == "bar2"
        }

        when: "an asset attribute value null is written directly"
        assetResource.writeAttributeValue(null, testAsset.getId(), "foo", "null")

        then: "the attribute value should match"
        new PollingConditions(delay: 1, timeout: 5).eventually {
            def asset = assetResource.get(null, testAsset.getId())
            assert !asset.getAttribute("foo").get().getValue().isPresent()
        }

        when: "an asset is updated with a different type"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setType(AssetType.BUILDING)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should not be updated"
        testAsset.wellKnownType == AssetType.ROOM

        when: "an asset is updated with a non-existent realm"
        testAsset.setRealm("thisdoesnotexistitreallydoesnt")
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.status == 403

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

        when: "an asset is updated with a parent in a different realm"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setParentId(managerDemoSetup.smartBuildingId)
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        when: "an asset is deleted but has children"
        assetResource.delete(null, [managerDemoSetup.apartment1Id])

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
