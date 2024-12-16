package org.openremote.test.assets

import jakarta.ws.rs.WebApplicationException
import org.jboss.resteasy.api.validation.ViolationReport
import org.openremote.container.timer.TimerService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetResource
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

        then: "the request should fail validation and return a validation report indicating the failure(s)"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        def report = ex.response.readEntity(ViolationReport)
        report != null
        report.propertyViolations.size() == 1
        report.propertyViolations.get(0).path == "attributes[illegal- Attribute:name&&&].name"

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
        ).token

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

        assetResource.writeAttributeValues(null, states.toArray() as AttributeEvent[])

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

        assetResource.writeAttributeValues(null, states.toArray() as AttributeEvent[])

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

        assetResource.writeAttributeValues(null, states.toArray() as AttributeEvent[])

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
