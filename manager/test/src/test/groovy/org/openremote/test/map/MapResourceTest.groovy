package org.openremote.test.map

import groovy.json.JsonSlurper
import org.openremote.manager.shared.map.MapResource
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.*
import static org.openremote.model.Constants.*;

class MapResourceTest extends Specification implements ManagerContainerTrait {

    def "Retrieve map settings"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort();
        def container = startContainerNoDemoImport(defaultConfig(serverPort), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM;
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "a test client target"
        def clientTarget = getClientTarget(serverUri(serverPort), realm, accessToken);

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

        cleanup: "the server should be stopped"
        stopContainer(container);
    }
}
