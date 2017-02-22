package org.openremote.test.ngsi

import elemental.json.Json
import org.openremote.manager.server.ngsi.EntityArrayMessageBodyConverter
import org.openremote.manager.server.ngsi.EntityMessageBodyConverter
import org.openremote.manager.server.ngsi.EntryPointMessageBodyConverter
import org.openremote.manager.shared.ngsi.EntityResource
import org.openremote.manager.shared.ngsi.Attribute
import org.openremote.manager.shared.ngsi.Entity
import org.openremote.manager.shared.ngsi.params.EntityListParams
import org.openremote.manager.shared.ngsi.simplequery.BinaryStatement
import org.openremote.manager.shared.ngsi.simplequery.Query
import org.openremote.manager.shared.ngsi.simplequery.QueryValue
import org.openremote.manager.shared.ngsi.AttributeType;
import org.openremote.test.ContainerTrait
import spock.lang.Ignore
import spock.lang.Specification

import javax.ws.rs.NotFoundException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.SetupService.DEMO_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.SetupService.DEMO_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*;

@Ignore
class EntityResourceTest extends Specification implements ContainerTrait {

    def "Retrieve sample rooms"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), DEMO_ADMIN_PASSWORD, DEMO_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "a test client target"
        def client = createClient(container)
                .register(EntryPointMessageBodyConverter.class)
                .register(EntityMessageBodyConverter.class)
                .register(EntityArrayMessageBodyConverter.class)
                .build();
        def serverUri = serverUri(serverPort);
        def clientTarget = getClientTarget(client, serverUri, realm, accessToken);

        and: "the entity resource"
        def entityResource = clientTarget.proxy(EntityResource.class);

        when: "query with no restrictions"
        def entities = entityResource.getEntities(null, null);
        then: "there should be some result (we don't now how many entities total are in test data, at least 2)"
        entities.length >= 2

        when: "query with id restriction"
        entities = entityResource.getEntities(
                null,
                new EntityListParams().id("Room1")
        );
        then: "the result should match"
        entities.length == 1
        assertRoom(entities[0], "Room1", 21.3, "Office 123")

        when: "query with id pattern restriction"
        entities = entityResource.getEntities(
                null,
                new EntityListParams().idPattern("Room1.*")
        );
        then: "the result should match"
        assertRoom(entities[0], "Room1", 21.3, "Office 123")

        when: "query with type restrictions"
        entities = entityResource.getEntities(
                null,
                new EntityListParams().type("Room")
        );
        then: "the result should match"
        entities.length == 2
        assertRoom(entities[0], null, null, null);
        assertRoom(entities[1], null, null, null);

        when: "query with type restrictions"
        entities = entityResource.getEntities(
                null,
                new EntityListParams().type("Room")
        );
        then: "the result should match"
        entities.length == 2
        assertRoom(entities[0], null, null, null);
        assertRoom(entities[1], null, null, null);

        when: "query with query restrictions"
        entities = entityResource.getEntities(
                null,
                new EntityListParams().query(
                        new Query(
                                new BinaryStatement("label", BinaryStatement.Operator.EQUAL, QueryValue.exact("Office 123"))
                        )
                )
        );
        then: "the result should match"
        entities.length == 1
        assertRoom(entities[0], "Room1", 21.3, "Office 123")

        and: "the server should be stopped"
        stopContainer(container);
    }

    boolean assertRoom(Entity room, String expectedId, Double expectedTemperature, String expectedLabel) {
        if (expectedId != null)
            assert room.getId() == expectedId;
        assert room.getType() == "Room"

        def temperature = room.getAttribute("temperature");
        assert temperature.getType() == AttributeType.FLOAT
        assert temperature.getMetadata() != null
        assert temperature.getMetadata().getElements().length == 0
        if (expectedTemperature)
            assert temperature.getValue().asNumber() == expectedTemperature

        def label = room.getAttribute("label")
        assert label.getType() == AttributeType.STRING
        assert label.getMetadata() != null
        assert label.getMetadata().getElements().length == 0
        if (expectedLabel)
            assert label.getValue().asString() == expectedLabel
        true
    }

    def "Create, update, and delete entity"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), DEMO_ADMIN_PASSWORD, DEMO_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "a test client target"
        def client = createClient(container)
                .register(EntryPointMessageBodyConverter.class)
                .register(EntityMessageBodyConverter.class)
                .register(EntityArrayMessageBodyConverter.class)
                .build();
        def serverUri = serverUri(serverPort);
        def clientTarget = getClientTarget(client, serverUri, realm, accessToken);

        and: "the entity resource"
        def entityResource = clientTarget.proxy(EntityResource.class);

        and: "a sample room"
        Entity room = new Entity(Json.createObject());
        room.setId("Room123");
        room.setType("Room");
        room.addAttribute(
                new Attribute("label", Json.createObject())
                        .setType(AttributeType.STRING)
                        .setValue(Json.create("Office 123"))
        )
        room.addAttribute(
                new Attribute("temperature", Json.createObject())
                        .setType(AttributeType.FLOAT)
                        .setValue(Json.create(20.5))
        );

        when: "the room is posted"
        entityResource.postEntity(null, room);
        and: "the new room has been retrieved"
        def createdRoom = entityResource.getEntity(null, room.getId(), null);
        Attribute temperature = createdRoom.getAttribute("temperature");
        then: "the room details should match"
        createdRoom.getId() == room.getId()
        createdRoom.getType() == room.getType()
        temperature.getValue().asNumber() == new Double(20.5)

        when: "the temperature is changed"
        temperature.setValue(Json.create(20.6))
        def roomPatch = new Entity(Json.createObject())
        roomPatch.addAttribute(temperature)
        and: "the room is updated"
        entityResource.patchEntityAttributes(null, createdRoom.getId(), roomPatch);
        and: "the updated room has been retrieved"
        def updatedRoom = entityResource.getEntity(null, room.getId(), null);
        def updatedTemperature = updatedRoom.getAttribute("temperature");
        then: "the room details should match"
        updatedRoom.getId() == room.getId()
        updatedRoom.getType() == room.getType()
        updatedRoom.getAttribute("label").getValue().asString() == "Office 123"
        updatedTemperature.getValue().asNumber() == new Double(20.6)

        when: "the room is deleted"
        entityResource.deleteEntity(null, room.getId());
        and: "the deleted room has been retrieved"
        entityResource.getEntity(null, room.getId(), null);
        then: "the room shouldn't exist"
        thrown NotFoundException

        and: "the server should be stopped"
        stopContainer(container);
    }
}
