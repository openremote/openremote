package org.openremote.test.map

import groovy.json.JsonSlurper
import org.openremote.manager.shared.map.MapResource
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import static org.openremote.manager.shared.Constants.APP_CLIENT_ID
import static org.openremote.manager.shared.Constants.MASTER_REALM

class MapResourceTest extends Specification implements ManagerContainerTrait {

    def "Retrieve map settings"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessTokenResponse = authenticate(container, realm, APP_CLIENT_ID, "test", "test")

        and: "a test client target"
        def client = createClient(container).build();
        def serverUri = serverUri(serverPort);
        def clientTarget = getClientTarget(client, serverUri, realm, accessTokenResponse.getToken());

        and: "the map resource"
        def mapResource = clientTarget.proxy(MapResource.class);

        when: "a request has been made"
        def mapSettings = mapResource.getSettings(null);

        then: "settings should be not-null"
        mapSettings != null;

        and: "JSON content is valid"
        def json = new JsonSlurper().parseText(mapSettings.toJson());
        json.center.size() == 2;
        json.maxBounds.size() == 4;
        json.maxZoom == 18;
        json.style != null;

        and: "the server should be stopped"
        stopContainer(container);
    }
}
