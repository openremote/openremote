package org.openremote.test.assets

import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.setup.SetupService
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AssetIntegrityTest extends Specification implements ManagerContainerTrait {

    def "Test asset changes as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource"
        def serverUri = serverUri(serverPort)
        def assetResource = getClientApiTarget(serverUri, MASTER_REALM, accessToken).proxy(AssetResource.class)

        when: "an asset is created in the authenticated realm"
        RoomAsset testAsset = new RoomAsset("Test Room")
            .setRealm(keycloakTestSetup.realmMaster.name)
        testAsset = assetResource.create(null, testAsset)

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.type == RoomAsset.DESCRIPTOR.getName()
        testAsset.realm == keycloakTestSetup.realmMaster.name
        testAsset.parentId == null

        when: "an asset is stored with an illegal attribute name"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.addOrReplaceAttributes(
            new Attribute<>("illegal- Attribute:name&&&", ValueType.TEXT)
        )

        assetResource.update(null, testAsset.getId(), testAsset)

        then: "the request should be bad"
        WebApplicationException ex = thrown()
        ex.response.status == 400

        when: "an asset is stored with a non-empty attribute value"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.addOrReplaceAttributes(
                new Attribute<>("foo", ValueType.TEXT, "bar")
        )
        assetResource.update(null, testAsset.id, testAsset)
        testAsset = assetResource.get(null, testAsset.getId())

        then: "the attribute should exist"
        testAsset.getAttribute("foo").isPresent()
        testAsset.getAttribute("foo").get().getValue().get() == "bar"

        when: "an asset attribute value is written directly"
        assetResource.writeAttributeValue(null, testAsset.getId(), "foo", '"bar2"')

        then: "the attribute value should match"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            def asset = assetResource.get(null, testAsset.getId())
            assert asset.getAttribute("foo").get().getValue().get() == "bar2"
        }

        when: "an asset attribute value null is written directly"
        assetResource.writeAttributeValue(null, testAsset.getId(), "foo", null)

        then: "the attribute value should match"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            def asset = assetResource.get(null, testAsset.getId())
            assert !asset.getAttribute("foo").get().getValue().isPresent()
        }

        when: "an asset is updated with a different type"
        testAsset = assetResource.get(null, testAsset.getId())
        def newTestAsset = new ThingAsset(testAsset.getName())
            .setId(testAsset.getId())
            .setRealm(testAsset.getRealm())
            .setAttributes(testAsset.getAttributes())
            .setParentId(testAsset.getParentId())

        assetResource.update(null, testAsset.id, newTestAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is updated with a new realm"
        testAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is created with a non existent realm"
        newTestAsset.setId(null)
        newTestAsset.setRealm("nonexistentrealm")
        assetResource.create(null, newTestAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is updated with a non-existent parent"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setParentId(UniqueIdentifierGenerator.generateId())
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is updated with itself as a parent"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setParentId(testAsset.getId())
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is updated with a parent in a different realm"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setParentId(managerTestSetup.smartBuildingId)
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.status == 403

        when: "an asset is deleted but has children"
        assetResource.delete(null, [managerTestSetup.apartment1Id])

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400
    }
}
