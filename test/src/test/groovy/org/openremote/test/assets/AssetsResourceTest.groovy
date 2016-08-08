package org.openremote.test.assets

import elemental.json.Json
import org.openremote.manager.server.assets.EntityArrayMessageBodyConverter
import org.openremote.manager.server.assets.EntityMessageBodyConverter
import org.openremote.manager.server.assets.EntryPointMessageBodyConverter
import org.openremote.manager.shared.assets.AssetsResource
import org.openremote.manager.shared.ngsi.Attribute
import org.openremote.manager.shared.ngsi.Entity
import org.openremote.manager.shared.ngsi.params.EntityListParams
import org.openremote.manager.shared.ngsi.simplequery.BinaryStatement
import org.openremote.manager.shared.ngsi.simplequery.Query
import org.openremote.manager.shared.ngsi.simplequery.QueryValue
import org.openremote.manager.shared.ngsi.AttributeType;
import org.openremote.test.ContainerTrait
import spock.lang.Specification

import javax.ws.rs.NotFoundException

import static org.openremote.manager.shared.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.shared.Constants.MASTER_REALM

class AssetsResourceTest extends Specification implements ContainerTrait {

    def "Retrieve sample rooms"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(container, realm, MANAGER_CLIENT_ID, "test", "test")

        and: "a test client target"
        def client = createClient(container)
                .register(EntryPointMessageBodyConverter.class)
                .register(EntityMessageBodyConverter.class)
                .register(EntityArrayMessageBodyConverter.class)
                .build();
        def serverUri = serverUri(serverPort);
        def clientTarget = getClientTarget(client, serverUri, realm, accessTokenResponse.getToken());

        and: "the assets resource"
        def assetsResource = clientTarget.proxy(AssetsResource.class);

        when: "query with no restrictions"
        def entities = assetsResource.getEntities(null, null);
        then: "the result should match"
        entities.length == 2
        assertRoom(entities[0], null, null, null);
        assertRoom(entities[1], null, null, null);

        when: "query with id restriction"
        entities = assetsResource.getEntities(
                null,
                new EntityListParams().id("Room1")
        );
        then: "the result should match"
        entities.length == 1
        assertRoom(entities[0], "Room1", 21.3, "Office 123")

        when: "query with id pattern restriction"
        entities = assetsResource.getEntities(
                null,
                new EntityListParams().idPattern("Room1.*")
        );
        then: "the result should match"
        assertRoom(entities[0], "Room1", 21.3, "Office 123")

        when: "query with type restrictions"
        entities = assetsResource.getEntities(
                null,
                new EntityListParams().type("Room")
        );
        then: "the result should match"
        entities.length == 2
        assertRoom(entities[0], null, null, null);
        assertRoom(entities[1], null, null, null);

        when: "query with type restrictions"
        entities = assetsResource.getEntities(
                null,
                new EntityListParams().type("Room")
        );
        then: "the result should match"
        entities.length == 2
        assertRoom(entities[0], null, null, null);
        assertRoom(entities[1], null, null, null);

        when: "query with query restrictions"
        entities = assetsResource.getEntities(
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
        def accessTokenResponse = authenticate(container, realm, MANAGER_CLIENT_ID, "test", "test")

        and: "a test client target"
        def client = createClient(container)
                .register(EntryPointMessageBodyConverter.class)
                .register(EntityMessageBodyConverter.class)
                .register(EntityArrayMessageBodyConverter.class)
                .build();
        def serverUri = serverUri(serverPort);
        def clientTarget = getClientTarget(client, serverUri, realm, accessTokenResponse.getToken());

        and: "the assets resource"
        def assetsResource = clientTarget.proxy(AssetsResource.class);

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
        assetsResource.postEntity(null, room);
        and: "the new room has been retrieved"
        def createdRoom = assetsResource.getEntity(null, room.getId(), null);
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
        assetsResource.patchEntityAttributes(null, createdRoom.getId(), roomPatch);
        and: "the updated room has been retrieved"
        def updatedRoom = assetsResource.getEntity(null, room.getId(), null);
        def updatedTemperature = updatedRoom.getAttribute("temperature");
        then: "the room details should match"
        updatedRoom.getId() == room.getId()
        updatedRoom.getType() == room.getType()
        updatedRoom.getAttribute("label").getValue().asString() == "Office 123"
        updatedTemperature.getValue().asNumber() == new Double(20.6)

        when: "the room is deleted"
        assetsResource.deleteEntity(null, room.getId());
        and: "the deleted room has been retrieved"
        assetsResource.getEntity(null, room.getId(), null);
        then: "the room shouldn't exist"
        thrown NotFoundException

        and: "the server should be stopped"
        stopContainer(container);
    }
}
