package org.openremote.test.assets

import jakarta.ws.rs.WebApplicationException
import org.jboss.resteasy.api.validation.ViolationReport
import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.gateway.GatewayService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.impl.GatewayAsset
import org.openremote.model.asset.impl.GroupAsset
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeState
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.util.UniqueIdentifierGenerator
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

import static org.openremote.model.util.MapAccess.getString
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
        )

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

        then: "the request should fail validation and return a validation report indicating the failure(s)"
        WebApplicationException ex = thrown()
        ex.response.withCloseable { r ->
            assert r.status == 400
            def report =  r.readEntity(ViolationReport)
            assert report != null
            assert report.propertyViolations.size() == 1
            assert report.propertyViolations.get(0).path == "attributes[illegal- Attribute:name&&&].name"
            return true
        }

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
        ex.response.withCloseable { r ->
            assert r.status == 403
            return true
        }

        when: "an asset is updated with a new realm"
        testAsset.setRealm(keycloakTestSetup.realmBuilding.name)
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.withCloseable { r ->
            assert r.status == 403
            return true
        }

        when: "an asset is created with a non existent realm"
        newTestAsset.setId(null)
        newTestAsset.setRealm("nonexistentrealm")
        assetResource.create(null, newTestAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.withCloseable { r ->
            assert r.status == 403
            return true
        }

        when: "an asset is updated with a non-existent parent"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setParentId(UniqueIdentifierGenerator.generateId())
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.withCloseable { r ->
            assert r.status == 403
            return true
        }

        when: "an asset is updated with itself as a parent"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setParentId(testAsset.getId())
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.withCloseable { r ->
            assert r.status == 403
            return true
        }

        when: "an asset is updated with a parent in a different realm"
        testAsset = assetResource.get(null, testAsset.getId())
        testAsset.setParentId(managerTestSetup.smartBuildingId)
        assetResource.update(null, testAsset.id, testAsset)

        then: "the request should be forbidden"
        ex = thrown()
        ex.response.withCloseable { r ->
            assert r.status == 403
            return true
        }

        when: "an asset is deleted but has children"
        assetResource.delete(null, [managerTestSetup.apartment1Id])

        then: "the request should be bad"
        ex = thrown()
        ex.response.withCloseable { r ->
            assert r.status == 400
            return true
        }
    }

    def "Batch parent updates reject unknown child asset ids as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user will make the call"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "there's an asset in the master realm"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
        def existingAsset = assetResource.create(null, new RoomAsset("Existing asset").setRealm(keycloakTestSetup.realmMaster.name))
        def missingChildId = UniqueIdentifierGenerator.generateId()

        when: "an updateParent call is made with child asset list containing an unknown child asset id"
        assetResource.updateParent(null, managerTestSetup.groundFloorId, [existingAsset.id, missingChildId])

        then: "the request should be bad and no asset should be moved"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        (assetResource.get(null, existingAsset.id) as RoomAsset).parentId == null
    }

    def "Batch parent updates reject unknown parent asset ids as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user will make the call"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "there's an asset in the master realm"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
        def existingAsset = assetResource.create(null, new RoomAsset("Existing asset").setRealm(keycloakTestSetup.realmMaster.name))
        def missingParentId = UniqueIdentifierGenerator.generateId()

        when: "an updateParent call is made with a non-existent parent asset id"
        assetResource.updateParent(null, missingParentId, [existingAsset.id])

        then: "the request should be bad and no asset should be moved"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        (assetResource.get(null, existingAsset.id) as RoomAsset).parentId == null
    }

    def "Batch parent updates reject self-parent requests as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user will make the call"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "there's an asset in the master realm"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
        def existingAsset = assetResource.create(null, new RoomAsset("Existing asset").setRealm(keycloakTestSetup.realmMaster.name))

        when: "an updateParent call is made with the asset itself as the parent"
        assetResource.updateParent(null, existingAsset.id, [existingAsset.id])

        then: "the request should be bad and the asset should remain a root asset"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        (assetResource.get(null, existingAsset.id) as RoomAsset).parentId == null
    }

    def "Batch parent updates reject mixed child realms as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user will make the call"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "there are assets in the master and building realms"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
        def masterAssetForMixedRealm = assetResource.create(null, new RoomAsset("Master Mixed Realm").setRealm(keycloakTestSetup.realmMaster.name))
        def buildingAssetForMixedRealm = assetResource.create(null, new RoomAsset("Building Mixed Realm").setRealm(keycloakTestSetup.realmBuilding.name))

        when: "an updateParent call is made with child asset list containing child assets from different realms"
        assetResource.updateParent(null, managerTestSetup.groundFloorId, [masterAssetForMixedRealm.id, buildingAssetForMixedRealm.id])

        then: "the request should be bad and no asset should be moved"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        (assetResource.get(null, masterAssetForMixedRealm.id) as RoomAsset).parentId == null
        (assetResource.get(null, buildingAssetForMixedRealm.id) as RoomAsset).parentId == null
    }

    def "Batch parent update rejects null or empty child lists as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user will make the call"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "there's an asset in the master realm"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
        def rootAsset = assetResource.create(null, new RoomAsset("Root Asset").setRealm(keycloakTestSetup.realmMaster.name))

        when: "a parent update is requested with an empty child list"
        assetResource.updateParent(null, managerTestSetup.groundFloorId, [] as List<String>)

        then: "the request should be bad"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        (assetResource.get(null, rootAsset.id) as RoomAsset).parentId == null

        when: "a parent clear is requested with a null child list"
        assetResource.updateNoneParent(null, null)

        then: "the request should be bad"
        ex = thrown()
        ex.response.status == 400
        (assetResource.get(null, rootAsset.id) as RoomAsset).parentId == null
    }

    def "Batch parent updates reject moving an asset under its own descendant as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user will make the call"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "there is an asset hierarchy in the master realm"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
        def rootAsset = assetResource.create(null, new RoomAsset("Root asset").setRealm(keycloakTestSetup.realmMaster.name))
        def childAsset = assetResource.create(null, new RoomAsset("Child asset").setRealm(keycloakTestSetup.realmMaster.name).setParentId(rootAsset.id))
        def grandChildAsset = assetResource.create(null, new RoomAsset("Grandchild asset").setRealm(keycloakTestSetup.realmMaster.name).setParentId(childAsset.id))

        when: "an updateParent call tries to move the root asset under its own descendant"
        assetResource.updateParent(null, grandChildAsset.id, [rootAsset.id])

        then: "the request should be bad and the hierarchy should remain unchanged"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        (assetResource.get(null, rootAsset.id) as RoomAsset).parentId == null
        (assetResource.get(null, childAsset.id) as RoomAsset).parentId == rootAsset.id
        (assetResource.get(null, grandChildAsset.id) as RoomAsset).parentId == childAsset.id
    }

    def "Batch parent updates reject group child-type mismatches as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user will make the call"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "there is a group parent that only accepts room assets"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
        def groupParent = assetResource.create(null, new GroupAsset("Rooms only", RoomAsset.class).setRealm(keycloakTestSetup.realmMaster.name))
        def invalidChild = assetResource.create(null, new ThingAsset("Invalid child").setRealm(keycloakTestSetup.realmMaster.name))

        when: "an updateParent call tries to move a mismatched asset type under the group"
        assetResource.updateParent(null, groupParent.id, [invalidChild.id])

        then: "the request should be bad and the asset should not be moved"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        (assetResource.get(null, invalidChild.id) as ThingAsset).parentId == null
    }

    def "Batch parent updates reject moving a local asset under a gateway asset as superuser"() {
        given: "the server container is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def gatewayService = container.getService(GatewayService.class)

        and: "an authenticated admin user will make the call"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "there is a locally registered gateway asset in the building realm"
        GatewayAsset gateway = assetStorageService.merge(new GatewayAsset("Test gateway")
                .setRealm(managerTestSetup.realmBuildingName))
        conditions.eventually {
            assert gatewayService.isLocallyRegisteredGateway(gateway.id)
        }

        and: "there is a regular asset in the same realm"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
        def localAsset = assetResource.create(null, new RoomAsset("Local asset").setRealm(managerTestSetup.realmBuildingName))

        when: "an updateParent call tries to move the local asset under the gateway asset"
        assetResource.updateParent(null, gateway.id, [localAsset.id])

        then: "the request should be bad and the asset should not be moved"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        (assetResource.get(null, localAsset.id) as RoomAsset).parentId == null
    }

    def "Batch parent clear rejects gateway descendant assets as superuser"() {
        given: "the server container is started"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def gatewayService = container.getService(GatewayService.class)

        and: "an authenticated admin user will make the call"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "there is a locally registered gateway asset and a gateway descendant in the building realm"
        GatewayAsset gateway = assetStorageService.merge(new GatewayAsset("Test gateway")
                .setRealm(managerTestSetup.realmBuildingName))
        conditions.eventually {
            assert gatewayService.isLocallyRegisteredGateway(gateway.id)
        }
        def gatewayDescendant = assetStorageService.merge(
                new RoomAsset("Gateway descendant")
                        .setId(UniqueIdentifierGenerator.generateId("GatewayDescendant"))
                        .setRealm(managerTestSetup.realmBuildingName)
                        .setParentId(gateway.id),
                false,
                gateway,
                null
        )

        when: "an updateNoneParent call tries to clear the parent of the gateway descendant"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
        assetResource.updateNoneParent(null, [gatewayDescendant.id])

        then: "the request should be bad and the gateway descendant should remain attached to the gateway"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        (assetStorageService.find(gatewayDescendant.id, true) as RoomAsset).parentId == gateway.id
    }

    def "Test writing attributes with timestamps"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def clientEventService = container.getService(ClientEventService.class)
        TimerService timerService = container.getService(TimerService.class);
        AssetDatapointService datapointService = container.getService(AssetDatapointService.class);

        and: "an authenticated admin user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        and: "the asset resource"
        def serverUri = serverUri(serverPort)
        def assetResource = getClientApiTarget(serverUri, MASTER_REALM, accessToken).proxy(AssetResource.class)

        when: "an asset is created in the authenticated realm"
        RoomAsset testAsset = new RoomAsset("Test Room")
                .setRealm(keycloakTestSetup.realmMaster.name)
                .addAttributes(new Attribute<Object>("noTimestampTest", ValueType.LONG))
        testAsset = assetResource.create(null, testAsset)

        then: "the asset should exist"
        testAsset.name == "Test Room"
        testAsset.type == RoomAsset.DESCRIPTOR.getName()
        testAsset.realm == keycloakTestSetup.realmMaster.name
        testAsset.parentId == null

        when: "an attribute is updated with no timestamp"
        testAsset = assetResource.get(null, testAsset.getId())

        timerService.clock.advanceTime(10, TimeUnit.MINUTES);
        timerService.stop(container)
        Long timestamp = timerService.getCurrentTimeMillis()

        assetResource.writeAttributeValue(null, testAsset.getId(), "noTimestampTest", 123)

        then: "the attribute's timestamp should match"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            RoomAsset asset = assetResource.get(null, testAsset.getId()) as RoomAsset
            assert asset.getAttribute("noTimestampTest").get().getTimestamp().get() == timestamp;
            assert asset.getAttribute("noTimestampTest").get().getValue().get() == 123;

        }

        when: "an attribute is updated with a timestamp"
        testAsset = assetResource.get(null, testAsset.getId())

        timerService.clock.advanceTime(10, TimeUnit.MINUTES);
        timerService.stop(container)
        timestamp = timerService.getCurrentTimeMillis()

        assetResource.writeAttributeValue(null, testAsset.getId(), testAsset.AREA.name, timestamp-2000, 123)

        then: "the attribute's timestamp should match"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            RoomAsset asset = assetResource.get(null, testAsset.getId()) as RoomAsset
            assert asset.getAttribute(testAsset.AREA.name).get().getTimestamp().get() == timestamp-2000;
            assert asset.getAttribute(testAsset.AREA.name).get().getValue().get() == 123;

        }

        when: "an attribute is updated with the current timestamp"
        testAsset = assetResource.get(null, testAsset.getId())

        timerService.clock.advanceTime(10, TimeUnit.MINUTES);
        timerService.stop(container)
        timestamp = timerService.getCurrentTimeMillis()

        assetResource.writeAttributeValue(null, testAsset.getId(), testAsset.AREA.name, timestamp-2000, 123)

        then: "the attribute's timestamp should match"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            RoomAsset asset = assetResource.get(null, testAsset.getId()) as RoomAsset
            assert asset.getAttribute(testAsset.AREA.name).get().getTimestamp().get() == timestamp-2000;
            assert asset.getAttribute(testAsset.AREA.name).get().getValue().get() == 123;

        }

        when: "an attribute value is updated with a timestamp earlier than the current value"

        assetResource.writeAttributeValue(null, testAsset.getId(), testAsset.AREA.name, timestamp-3000, 1234)

        then: "the timestamp and value of the attribute should be the original one"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            RoomAsset asset = assetResource.get(null, testAsset.getId()) as RoomAsset
            assert asset.getAttribute(testAsset.AREA.name).get().getTimestamp().get() == timestamp-2000;
            assert asset.getAttribute(testAsset.AREA.name).get().getValue().get() == 123;
        }

        when: "an attribute value is updated with a timestamp of current clock time"
        assetResource.writeAttributeValue(null, testAsset.getId(), testAsset.AREA.name, 0, 12345)

        then: "the timestamp and value of the attribute should be the new ones"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            RoomAsset asset = assetResource.get(null, testAsset.getId()) as RoomAsset
            assert asset.getAttribute(testAsset.AREA.name).get().getTimestamp().get() == timestamp;
            assert asset.getAttribute(testAsset.AREA.name).get().getValue().get() == 12345;
        }

        timerService.clock.advanceTime(10, TimeUnit.MINUTES);

        when: "Multiple attribute values are updated with current timestamps"
        timestamp = timerService.getCurrentTimeMillis();

        List<AttributeEvent> states = new ArrayList<>();
        states.add(new AttributeEvent(new AttributeState(testAsset.getId(), testAsset.AREA.getName(), 123456), timestamp-3000));
        states.add(new AttributeEvent(new AttributeState(testAsset.getId(), testAsset.ROOM_NUMBER.getName(), 123456), timestamp-3000));

        assetResource.writeAttributeEvents(null, states.toArray() as AttributeEvent[])

        then: "the attribute value should match"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            RoomAsset asset = assetResource.get(null, testAsset.getId()) as RoomAsset
            assert asset.getAttribute(asset.AREA.getName()).get().getValue().get() == 123456;
            assert asset.getAttribute(asset.AREA.getName()).get().getTimestamp().get() == timestamp-3000;

            assert asset.getAttribute(asset.ROOM_NUMBER.getName()).get().getValue().get() == 123456;
            assert asset.getAttribute(asset.ROOM_NUMBER.getName()).get().getTimestamp().get() == timestamp-3000;
        }

        when: "Multiple attribute values are updated with future timestamps"
        timestamp = timerService.getCurrentTimeMillis();

        states = new ArrayList<>();
        states.add(new AttributeEvent(new AttributeState(testAsset.getId(), testAsset.AREA.getName(), 1234567), timestamp-2000));
        states.add(new AttributeEvent(new AttributeState(testAsset.getId(), testAsset.ROOM_NUMBER.getName(), 1234567), timestamp-2000));

        assetResource.writeAttributeEvents(null, states.toArray() as AttributeEvent[])

        then: "the attribute value should match"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            RoomAsset asset = assetResource.get(null, testAsset.getId()) as RoomAsset
            assert asset.getAttribute(asset.AREA.getName()).get().getValue().get() == 1234567;
            assert asset.getAttribute(asset.AREA.getName()).get().getTimestamp().get() == timestamp-2000;

            assert asset.getAttribute(asset.ROOM_NUMBER.getName()).get().getValue().get() == 1234567;
            assert asset.getAttribute(asset.ROOM_NUMBER.getName()).get().getTimestamp().get() == timestamp-2000;
        }

        when: "Multiple attribute values are updated with past timestamps"
        states = new ArrayList<>();
        states.add(new AttributeEvent(new AttributeState(testAsset.getId(), testAsset.AREA.getName(), 12345678), timestamp-4000));
        states.add(new AttributeEvent(new AttributeState(testAsset.getId(), testAsset.ROOM_NUMBER.getName(), 12345678), timestamp-4000));

        assetResource.writeAttributeEvents(null, states.toArray() as AttributeEvent[])

        then: "the attribute value should match"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            RoomAsset asset = assetResource.get(null, testAsset.getId()) as RoomAsset
            assert asset.getAttribute(asset.AREA.getName()).get().getValue().get() == 1234567;
            assert asset.getAttribute(asset.AREA.getName()).get().getTimestamp().get() == timestamp-2000;

            assert asset.getAttribute(asset.ROOM_NUMBER.getName()).get().getValue().get() == 1234567;
            assert asset.getAttribute(asset.ROOM_NUMBER.getName()).get().getTimestamp().get() == timestamp-2000;
        }

        when: "Multiple attribute values are updated with no timestamps"
        timerService.stop(container)
        timestamp = timerService.getCurrentTimeMillis();

        List<AttributeState> noTimestampStates = new ArrayList<>();
        noTimestampStates.add(new AttributeState(testAsset.getId(), testAsset.AREA.getName(), 123456789));
        noTimestampStates.add(new AttributeState(testAsset.getId(), testAsset.ROOM_NUMBER.getName(), 123456789));

        assetResource.writeAttributeValues(null, noTimestampStates.toArray() as AttributeState[])

        then: "the attribute value should match"
        new PollingConditions(timeout: 5, delay: 0.2).eventually {
            RoomAsset asset = assetResource.get(null, testAsset.getId()) as RoomAsset
            assert asset.getAttribute(asset.AREA.getName()).get().getValue().get() == 123456789;
            assert asset.getAttribute(asset.AREA.getName()).get().getTimestamp().get() == timestamp;

            assert asset.getAttribute(asset.ROOM_NUMBER.getName()).get().getValue().get() == 123456789;
            assert asset.getAttribute(asset.ROOM_NUMBER.getName()).get().getTimestamp().get() == timestamp;
        }

    }
}
