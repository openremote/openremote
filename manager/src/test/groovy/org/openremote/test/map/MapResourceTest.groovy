package org.openremote.test.map

import groovy.json.JsonSlurper
import org.openremote.manager.shared.map.MapResource
import org.openremote.test.ContainerTrait
import spock.lang.Specification

import static org.openremote.manager.server.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.server.Constants.MASTER_REALM

class MapResourceTest extends Specification implements ContainerTrait {

    def "Retrieve map settings"() {
        given: "An authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(realm, MANAGER_CLIENT_ID, "test", "test")

        and: "The map resource"
        def mapResource = getClientTarget(realm, accessTokenResponse.getToken()).proxy(MapResource.class);

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
