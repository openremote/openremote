package org.openremote.test.map

import groovy.json.JsonSlurper
import org.openremote.model.map.MapResource
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Ignore
import spock.lang.Specification

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.*
import static org.openremote.model.Constants.*

// TODO: Remove this once map service is removed and we fallback to standalone tile server
@Ignore
class MapResourceTest extends Specification implements ManagerContainerTrait {

    def "Retrieve map settings"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())

        and: "an authenticated user"
        def realm = MASTER_REALM
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "a test client target"
        def clientTarget = getClientApiTarget(serverUri(serverPort), realm, accessToken)

        and: "the map resource"
        def mapResource = clientTarget.proxy(MapResource.class)

        when: "a request has been made"
        def mapSettings = mapResource.getSettings(null)

        then: "settings should be not-null"
        mapSettings != null

        and: "JSON content is valid"
        def json = new JsonSlurper().parseText(ValueUtil.asJSON(mapSettings).orElse("null"))
        json.options != null
        json.options.default != null
        json.options.default.center.size() == 2
        json.options.default.bounds.size() == 4
        json.sources != null
        json.layers.size() > 0
    }
}
