package org.openremote.test.map

import groovy.json.JsonSlurper
import org.openremote.container.ContainerService
import org.openremote.manager.server.map.MapService
import org.openremote.manager.shared.map.MapResource
import org.openremote.test.ContainerTrait
import spock.lang.Specification

class MapResourceTest extends Specification implements ContainerTrait {

    @Override
    ContainerService[] getContainerServices() {
        [new MapService()]
    }

    def "Retrieve map settings"() {
        given: "The map resource"
        def mapResource = getClientTarget().proxy(MapResource.class);

        when: "A request has been made"
        def mapSettings = mapResource.getSettings(null);

        then: "Settings should be not-null"
        mapSettings != null;

        and: "JSON content is valid"
        def json = new JsonSlurper().parseText(mapSettings.toJson());
        json.center.size() == 2;
        json.maxBounds.size() == 4;
        json.maxZoom == 18;
        json.style != null;
    }
}
