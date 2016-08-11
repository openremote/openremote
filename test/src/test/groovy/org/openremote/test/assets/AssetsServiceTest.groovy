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
package org.openremote.test.assets

import elemental.json.Json
import org.openremote.manager.server.assets.AssetProvider
import org.openremote.manager.server.assets.AssetsService
import org.openremote.manager.shared.ngsi.Attribute
import org.openremote.manager.shared.ngsi.AttributeType
import org.openremote.manager.shared.ngsi.ContextEntity
import org.openremote.manager.shared.ngsi.EntityAttributeQuery
import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.ws.rs.NotFoundException

class AssetsServiceTest extends Specification implements ContainerTrait {

    def "Register Asset Provider"() {
        def requestedAttributes = [];

        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "the assets service is retrieved"
        def assetsService = container.getService(AssetsService.class);

        when: "a mock asset provider with fake attributes is created"
        def fakeAttributes = [
                new Attribute("temperature", AttributeType.FLOAT, Json.create(10.1)),
                new Attribute("speed", AttributeType.INTEGER, Json.create(50))
        ];

        def assetProvider = new AssetProvider() {
            @Override
            List<Attribute> getAssetAttributeValues(String assetId, List<String> attributes) {
                if (attributes.size() != 2 || !attributes.contains("temperature") || !attributes.contains("speed")) {
                    return null;
                }

                requestedAttributes.addAll(attributes);

                return fakeAttributes;
            }

            @Override
            boolean setAssetAttributeValues(String assetId, List<Attribute> attributes) {
                return false
            }
        }

        and: "the mock provider registers as a provider of a fake entity with the assets service"
        def result = assetsService.registerAssetProvider("Car", "Car1", fakeAttributes, assetProvider);

        then: "the mock provider is registered"
        result == true;

        when: "an asset registered with the mock provider is requested"
        def entity = assetsService.getContextBroker().getEntity("Car1", null);

        then: "the mock asset provider should have resolved the request"
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

        when: "provider is unregistered and previously registered asset is requested"
        assetsService.unregisterAssetProvider("Car", "Car1", assetProvider);
        entity = assetsService.getContextBroker().getEntity("Car1", null);

        then: "a 404 response should be received"
        thrown(NotFoundException)

        cleanup: "ensure mock provider is unregistered"
        assetsService.unregisterAssetProvider("Car", "Car1", assetProvider);

        and: "the server should be stopped"
        stopContainer(container);
    }

    def "Auto Refresh of Registration"() {
        def requestedAttributes = [];

        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "the assets service is retrieved"
        def assetsService = container.getService(AssetsService.class);

        when: "a mock asset provider with fake attributes is created"
        def fakeAttributes = [
                new Attribute("temperature", AttributeType.FLOAT, Json.create(10.1)),
                new Attribute("speed", AttributeType.INTEGER, Json.create(50))
        ];

        def assetProvider = new AssetProvider() {
            @Override
            List<Attribute> getAssetAttributeValues(String assetId, List<String> attributes) {
                if (attributes.size() != 2 || !attributes.contains("temperature") || !attributes.contains("speed")) {
                    return null;
                }

                requestedAttributes.addAll(attributes);

                return fakeAttributes;
            }

            @Override
            boolean setAssetAttributeValues(String assetId, List<Attribute> attributes) {
                return false
            }
        }

        and: "the context provider refresh interval is set to 30s (minimum)"
        assetsService.getRegistrationProvider().setRefreshInterval(30);

        and: "the mock provider registers as a provider of a fake entity with the assets service"
        def result = assetsService.registerAssetProvider("Car", "Car1", fakeAttributes, assetProvider);

        then: "the mock provider is registered"
        result == true;

        when: "enough time passes that auto refresh would have occurred"
        Thread.sleep(35000);

        and: "an asset registered with the mock provider is requested"
        def response = assetsService.getContextBrokerV1().queryContext(
                new EntityAttributeQuery([new ContextEntity("Car", "Car1", false)], null)
        );

        then: "the mock asset provider should have resolved the request"
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
        assetsService.unregisterAssetProvider("Car", "Car1", assetProvider);

        and: "the server should be stopped"
        stopContainer(container);
    }
}
