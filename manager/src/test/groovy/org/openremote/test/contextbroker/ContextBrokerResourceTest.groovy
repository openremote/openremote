package org.openremote.test.contextbroker

import elemental.json.Json
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.openremote.manager.server.contextbroker.EntityArrayMessageBodyConverter
import org.openremote.manager.server.contextbroker.EntityMessageBodyConverter
import org.openremote.manager.server.contextbroker.EntryPointMessageBodyConverter
import org.openremote.manager.shared.contextbroker.ContextBrokerResource
import org.openremote.manager.shared.ngsi.Attribute
import org.openremote.manager.shared.ngsi.Entity
import org.openremote.test.ContainerTrait
import spock.lang.Specification

import static org.openremote.manager.server.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.server.Constants.MASTER_REALM

class ContextBrokerResourceTest extends Specification implements ContainerTrait {

    @Override
    def prepareClient(ResteasyClientBuilder clientBuilder) {
        clientBuilder
                .register(EntryPointMessageBodyConverter.class)
                .register(EntityMessageBodyConverter.class)
                .register(EntityArrayMessageBodyConverter.class);
    }

    def "Retrieve sample rooms"() {
        given: "An authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(realm, MANAGER_CLIENT_ID, "test", "test")

        and: "the context broker resource"
        def contextBrokerResource = getClientTarget(realm, accessTokenResponse.getToken()).proxy(ContextBrokerResource.class);

        when: "a request has been made"
        def entities = contextBrokerResource.getEntities(null);

        then: "the number of rooms should match"
        entities != null
        entities.length == 2
    }

    def "Create, update, and delete entity"() {
        given: "An authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(realm, MANAGER_CLIENT_ID, "test", "test")

        and: "the context broker resource"
        def contextBrokerResource = getClientTarget(realm, accessTokenResponse.getToken()).proxy(ContextBrokerResource.class);

        and: "a sample room"
        Entity room = new Entity(Json.createObject());
        room.setId("Room123");
        room.setType("Room");
        room.addAttribute(
                new Attribute("temperature", Json.createObject())
                        .setType("float")
                        .setValue(Json.create(20.5))
        );

        when: "the room is posted"
        def response = contextBrokerResource.postEntity(null, room);

        and: "the new room has been retrieved"
        def createdRoom = contextBrokerResource.getEntity(null, room.getId());
        Attribute temperature = createdRoom.getAttribute("temperature");

        then: "the response code should be 201"
        response.getStatus() == 201

        and: "the room details should match"
        createdRoom.getId() == room.getId()
        createdRoom.getType() == room.getType()
        temperature.getValue().asNumber() == new Double(20.5)

        when: "the temperature is changed"
        temperature.setValue(Json.create(20.6))

        and: "the room is updated"
        response = contextBrokerResource.putEntity(null, createdRoom.getId(), createdRoom);

        then: "the response should be 204"
        response.getStatus() == 204

        when: "the updated room has been retrieved"
        def updatedRoom = contextBrokerResource.getEntity(null, room.getId());
        def updatedTemperature = updatedRoom.getAttribute("temperature");

        then: "the room details should match"
        updatedRoom.getId() == room.getId()
        updatedRoom.getType() == room.getType()
        updatedTemperature.getValue().asNumber() == new Double(20.6)

        when: "the room is deleted"
        response = contextBrokerResource.deleteEntity(null, updatedRoom.getId());

        then: "the response should be 204"
        response.getStatus() == 204
    }
}
