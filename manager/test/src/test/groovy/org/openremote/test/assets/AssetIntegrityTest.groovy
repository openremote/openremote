package org.openremote.test.assets

import elemental.json.Json
import org.openremote.container.util.IdentifierUtil
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.shared.asset.AssetResource
import org.openremote.model.Attribute
import org.openremote.model.Attributes
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetAttributes
import org.openremote.model.asset.AssetType
import org.openremote.model.AttributeType
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

        when: "an asset is stored with an illegal attribute name"
        def attributes = new AssetAttributes(testAsset.getAttributes())
        attributes.put(new AssetAttribute(testAsset.id, "illegal- Attribute:name&&&"))
        testAsset.setAttributes(attributes.getJsonObject())
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be bad"
        WebApplicationException ex = thrown()
        ex.response.status == 400

        when: "an asset is stored with a non-empty attribute value"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setAttributes(
                new AssetAttributes().put(new AssetAttribute("foo", AttributeType.STRING, Json.create("bar"))).getJsonObject()
        )
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the attribute should exist"
        new AssetAttributes(testAsset.getAttributes()).get("foo").getValueAsString() == "bar"

        when: "an asset attribute value is written directly"
        assetResource.writeAttributeValue(null, testAsset.getId(), "foo", "\"bar2\"")
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the attribute value should match"
        new AssetAttributes(testAsset.getAttributes()).get("foo").getValueAsString() == "bar2"
        assetResource.readAttributeValue(null, testAsset.getId(), "foo") == "\"bar2\""

        when: "an asset attribute value null is written directly"
        assetResource.writeAttributeValue(null, testAsset.getId(), "foo", "null")
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the attribute value should match"
        !new AssetAttributes(testAsset.getAttributes()).get("foo").hasValue()
        assetResource.readAttributeValue(null, testAsset.getId(), "foo") == "null"

        when: "an asset is updated with a different type"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setType(AssetType.BUILDING)
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the asset should not be updated"
        testAsset.wellKnownType == AssetType.ROOM

        when: "an asset is updated with a non-existent realm"
        testAsset.setRealmId("thisdoesnotexistitreallydoesnt")
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be bad"
        ex = thrown()
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
