/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test.ngsi

import elemental.json.Json
import org.openremote.manager.server.ngsi.EntityArrayMessageBodyConverter
import org.openremote.manager.server.ngsi.EntityMessageBodyConverter
import org.openremote.manager.server.ngsi.EntityProvider
import org.openremote.manager.server.ngsi.EntityService
import org.openremote.manager.server.ngsi.EntryPointMessageBodyConverter
import org.openremote.model.Consumer
import org.openremote.manager.shared.ngsi.Attribute
import org.openremote.manager.shared.ngsi.AttributeType
import org.openremote.manager.shared.ngsi.ContextEntity
import org.openremote.manager.shared.ngsi.Entity
import org.openremote.manager.shared.ngsi.EntityAttributeQuery
import org.openremote.manager.shared.ngsi.params.BasicEntityParams
import org.openremote.manager.shared.ngsi.EntityResource
import org.openremote.manager.shared.ngsi.params.SubscriptionParams
import org.openremote.test.ContainerTrait
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.NotFoundException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.*;

@Ignore
class EntityServiceTest extends Specification implements ContainerTrait {

    def "Register Entity Provider"() {
        def requestedAttributes = [];

        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "the entity service is retrieved"
        def entityService = container.getService(EntityService.class);

        when: "a mock entity provider with fake attrs is created"
        def fakeAttributes = [
                new Attribute("temperature", AttributeType.FLOAT, Json.create(10.1)),
                new Attribute("speed", AttributeType.INTEGER, Json.create(50))
        ];

        def entityProvider = new EntityProvider() {
            @Override
            List<Attribute> getAttributeValues(String entityId, List<String> attributes) {
                if (attributes.size() != 2 || !attributes.contains("temperature") || !attributes.contains("speed")) {
                    return null;
                }

                requestedAttributes.addAll(attributes);

                return fakeAttributes;
            }

            @Override
            boolean setAttributeValues(String entityId, List<Attribute> attributes) {
                return false
            }
        }

        and: "the mock provider registers as a provider of a fake entity with the entity service"
        def result = entityService.registerEntityProvider("Car", "Car1", fakeAttributes, entityProvider);

        then: "the mock provider is registered"
        result == true;

        when: "an entity registered with the mock provider is requested"
        def entity = entityService.getContextBroker().getEntity("Car1", null);

        then: "the mock entity provider should have resolved the request"
        def conditions = new PollingConditions(timeout: 10)

        conditions.eventually {
            assert entity != null;
        }

        requestedAttributes.size() == 2;
        entity.getId() == "Car1";
        entity.getType() == "Car";
        entity.getAttributes().length == 2;
        Attribute tempAttr = Arrays.stream(entity.getAttributes())
                .filter({ a -> a.getName().equals("temperature") })
                .findFirst()
                .get();
        Attribute speedAttr = Arrays.stream(entity.getAttributes())
                .filter({ a -> a.getName().equals("speed") })
                .findFirst()
                .get();
        tempAttr != null;
        tempAttr.getType() == AttributeType.FLOAT;
        tempAttr.getValue().asString() == fakeAttributes[0].getValue().asString();
        speedAttr != null;
        speedAttr.getType() == AttributeType.INTEGER;
        speedAttr.getValue().asString() == fakeAttributes[1].getValue().asString();

        when: "provider is unregistered and previously registered entity is requested"
        entityService.unregisterEntityProvider("Car", "Car1", entityProvider);
        entity = entityService.getContextBroker().getEntity("Car1", null);

        then: "a 404 response should be received"
        thrown(NotFoundException)

        cleanup: "ensure mock provider is unregistered"
        entityService.unregisterEntityProvider("Car", "Car1", entityProvider);

        and: "the server should be stopped"
        stopContainer(container);
    }

    def "Auto Refresh Entity Provider"() {
        def requestedAttributes = [];

        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "the entity service is retrieved"
        def entityService = container.getService(EntityService.class);

        when: "a mock entity provider with fake attrs is created"
        def fakeAttributes = [
                new Attribute("temperature", AttributeType.FLOAT, Json.create(10.1)),
                new Attribute("speed", AttributeType.INTEGER, Json.create(50))
        ];

        def entityProvider = new EntityProvider() {
            @Override
            List<Attribute> getAttributeValues(String entityId, List<String> attributes) {
                if (attributes.size() != 2 || !attributes.contains("temperature") || !attributes.contains("speed")) {
                    return null;
                }

                requestedAttributes.addAll(attributes);

                return fakeAttributes;
            }

            @Override
            boolean setAttributeValues(String entityId, List<Attribute> attributes) {
                return false
            }
        }

        and: "the context provider refresh interval is set to 30s (minimum)"
        entityService.getRegistrationProvider().setRefreshInterval(30);

        and: "the mock provider registers as a provider of a fake entity with the entity service"
        def result = entityService.registerEntityProvider("Car", "Car1", fakeAttributes, entityProvider);

        then: "the mock provider is registered"
        result == true;

        when: "enough time passes that auto refresh would have occurred"
        Thread.sleep(35000);

        and: "an entity registered with the mock provider is requested"
        def response = entityService.getContextBrokerV1().queryContext(
                new EntityAttributeQuery([new ContextEntity("Car", "Car1", false)], null)
        );

        then: "the mock entity provider should have resolved the request"
        def conditions = new PollingConditions(timeout: 10)

        conditions.eventually {
            assert response.contextResponses != null;
        }

        requestedAttributes.size() == 2;
        response.contextResponses.size() == 1;
        response.contextResponses.get(0).getContextElement().getId() == "Car1";
        response.contextResponses.get(0).getContextElement().getType() == "Car";
        response.contextResponses.get(0).getContextElement().getAttributes().size() == 2;
        Attribute tempAttr = response.contextResponses.get(0).getContextElement().getAttributes()
                .stream()
                .filter({ a -> a.getName().equals("temperature") })
                .findFirst()
                .get();
        Attribute speedAttr = response.contextResponses.get(0).getContextElement().getAttributes()
                .stream()
                .filter({ a -> a.getName().equals("speed") })
                .findFirst()
                .get();
        tempAttr != null;
        tempAttr.getType() == AttributeType.FLOAT;
        tempAttr.getValue().asString() == fakeAttributes[0].getValue().asString();
        speedAttr != null;
        speedAttr.getType() == AttributeType.INTEGER;
        speedAttr.getValue().asString() == fakeAttributes[1].getValue().asString();

        cleanup: "ensure mock provider is unregistered"
        entityService.unregisterEntityProvider("Car", "Car1", entityProvider);

        and: "the server should be stopped"
        stopContainer(container);
    }

    def "Register Entity Listener"() {
        def receivedEntities = [];

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

        and: "the entity service"
        def entityService = container.getService(EntityService.class);

        and: "a sample entity"
        Entity car = new Entity(Json.createObject());
        car.setId("Car123");
        car.setType("Car");
        car.addAttribute(
                new Attribute("make", Json.createObject())
                        .setType(AttributeType.STRING)
                        .setValue(Json.create("AUDI"))
        )
        car.addAttribute(
                new Attribute("model", Json.createObject())
                        .setType(AttributeType.STRING)
                        .setValue(Json.create("RS6"))
        );
        car.addAttribute(
                new Attribute("speed", Json.createObject())
                        .setType(AttributeType.INTEGER)
                        .setValue(Json.create(250))
        );

        when: "the entity is posted"
        entityResource.postEntity(null, car);

        and: "a mock entity listener is created"
        def listener = new Consumer<Entity[]>() {
            @Override
            void accept(Entity[] entities) throws Exception {
                receivedEntities.addAll(entities);
            }
        }

        and: "the mock listener registers with the entity service for all IDs starting with Car"
        def subscription = new SubscriptionParams();
        subscription.setEntities(
                [new BasicEntityParams("Car.*", true)]
        );
        def result = entityService.registerEntityListener(listener, subscription);

        then: "the mock listener is registered"
        result == true;

        and: "the mock listener should have been notified"
        def conditions = new PollingConditions(timeout: 10)

        conditions.eventually {
            assert receivedEntities.size() == 1;
        }

        when: "the car speed is modified"
        def speed = car.getAttribute("speed");
        speed.setValue(Json.create(0))
        def carPatch = new Entity(Json.createObject())
        carPatch.addAttribute(speed)

        and: "the car is updated"
        entityResource.patchEntityAttributes(null, car.getId(), carPatch);

        then: "the mock listener should have been notified"
        conditions.eventually {
            assert receivedEntities.size() == 2;
        }

        when: "the mock listener is unregistered"
        entityService.unregisterEntityListener(listener);

        and: "the car speed is modified"
        speed.setValue(Json.create(60))
        carPatch = new Entity(Json.createObject())
        carPatch.addAttribute(speed)

        and: "the car is updated"
        entityResource.patchEntityAttributes(null, car.getId(), carPatch);

        then: "the mock listener should not have been notified"
        Thread.sleep(5000);
        receivedEntities.size() == 2;

        cleanup: "ensure mock listener is unregistered"
        if (entityService != null)
            entityService.unregisterEntityListener(listener);
        if (entityResource != null && car != null)
            entityResource.deleteEntity(null, car.getId());

        and: "the server should be stopped"
        stopContainer(container);
    }

    def "Auto Refresh Entity Listener"() {
        def receivedEntities = [];

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

        and: "the entity service"
        def entityService = container.getService(EntityService.class);

        and: "a sample entity"
        Entity car = new Entity(Json.createObject());
        car.setId("Car123");
        car.setType("Car");
        car.addAttribute(
                new Attribute("make", Json.createObject())
                        .setType(AttributeType.STRING)
                        .setValue(Json.create("AUDI"))
        )
        car.addAttribute(
                new Attribute("model", Json.createObject())
                        .setType(AttributeType.STRING)
                        .setValue(Json.create("RS6"))
        );
        car.addAttribute(
                new Attribute("speed", Json.createObject())
                        .setType(AttributeType.INTEGER)
                        .setValue(Json.create(250))
        );

        when: "the entity is posted"
        entityResource.postEntity(null, car);

        and: "a mock entity listener is created"
        def listener = new Consumer<Entity[]>() {
            @Override
            void accept(Entity[] entities) throws Exception {
                receivedEntities.addAll(entities);
            }
        }

        and: "the entity listener refresh interval is set to 30s (minimum)"
        entityService.getSubscriptionProvider().setRefreshInterval(30);

        and: "the mock listener registers with the entity service for all IDs starting with Car"
        def subscription = new SubscriptionParams();
        subscription.setEntities(
                [new BasicEntityParams("Car.*", true)]
        );
        def result = entityService.registerEntityListener(listener, subscription);

        then: "the mock listener is registered"
        result == true;

        and: "the mock listener should have been notified"
        def conditions = new PollingConditions(timeout: 15)

        conditions.eventually {
            assert receivedEntities.size() == 1;
        }

        when: "enough time passes that auto refresh would have occurred"
        Thread.sleep(35000);

        and: "the car speed is modified"
        def speed = car.getAttribute("speed");
        speed.setValue(Json.create(0))
        def carPatch = new Entity(Json.createObject())
        carPatch.addAttribute(speed)

        and: "the car is updated"
        entityResource.patchEntityAttributes(null, car.getId(), carPatch);

        then: "the mock listener should have been notified"
        conditions.eventually {
            assert receivedEntities.size() == 2;
        }

        cleanup: "ensure mock listener is unregistered"
        if (entityService != null)
            entityService.unregisterEntityListener(listener);
        if (entityResource != null && car != null)
            entityResource.deleteEntity(null, car.getId());

        and: "the server should be stopped"
        stopContainer(container);
    }
}
