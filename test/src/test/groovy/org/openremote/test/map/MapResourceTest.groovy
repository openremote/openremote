package org.openremote.test.map

import com.fasterxml.jackson.databind.node.ObjectNode
import groovy.json.JsonSlurper
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.openremote.manager.map.MapService
import org.openremote.model.manager.MapConfig
import org.openremote.model.map.MapResource
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Files
import java.nio.file.Path

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.*
import static org.openremote.model.Constants.*

// TODO: Remove this once map service is removed and we fallback to standalone tile server
class MapResourceTest extends Specification implements ManagerContainerTrait {
    @Shared
    static ResteasyWebTarget clientTarget

    @Shared
    static MapResource mapResource

    def setupSpec() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())

        and: "an access_token is retrieved"
        def realm = MASTER_REALM
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "a test client target set"
        clientTarget = getClientApiTarget(serverUri(serverPort), realm, accessToken)

        and: "the map resource is configured"
        mapResource = clientTarget.proxy(MapResource.class)
    }

    def "Retrieve map settings"() {
        when: "a request has been made"
        def mapSettings = mapResource.getSettings(null)

        then: "settings should be not-null"
        mapSettings != null

        and: "JSON content is valid"
        def json = new JsonSlurper().parseText(ValueUtil.asJSON(mapSettings).orElse("null"))
        json.options != null
        json.options.default != null
        json.options.default.center.size() >= 2
        json.options.default.bounds.size() == 4
        json.sources != null
        json.layers.size() > 0
    }

    def "Upload custom map tiles"() {
        when: "valid tiles are uploaded"
        def response = clientTarget
                .path("map/upload")
                .queryParam("filename", "eindhoven.mbtiles")
                .request()
                .post(Entity.entity(Files.readAllBytes(Path.of("manager/src/map/mapdata.mbtiles")), MediaType.APPLICATION_OCTET_STREAM_TYPE))

        then: "the custom tiles should be saved"
        new PollingConditions(timeout: 20, delay: 1).eventually {
            assert Files.exists(Path.of("tmp/map/eindhoven.mbtiles"))
        }

        and: "and the mapsettings match with the custom mbtiles metadata"
        def json = ValueUtil.JSON.readValue(response.getEntity(), MapConfig.class)
        json.options != null
        json.options.default != null
        json.options.default.center.size() == 3
        json.options.default.bounds.size() == 4
        json.sources != null

        when: "invalid tiles are uploaded"
        mapResource.deleteMap(null)

        then: "an internal server error should occur"
        !Files.exists(Path.of("tmp/map/eindhoven.mbtiles"))

        when: "invalid tiles are uploaded"
        response = clientTarget
            .path("map/upload")
            .queryParam("filename", "eindhoven.mbtiles")
            .request()
            .post(Entity.entity(Files.readAllBytes(Path.of("manager/src/map/mapsettings.json")), MediaType.APPLICATION_OCTET_STREAM_TYPE))

        then: "an internal server error should occur"
        response.status == 500

        when: "illegal filename was specified"
        response = clientTarget
                .path("map/upload")
                .queryParam("filename", "../mapdata.mbtiles")
                .request()
                .post(Entity.entity(Files.readAllBytes(Path.of("manager/src/map/mapdata.mbtiles")), MediaType.APPLICATION_OCTET_STREAM_TYPE))

        then: "bad request error should occur"
        response.status == 400

        when: "the tiles are too large"
        container.getService(MapService.class).customMapLimit = 10
        response = clientTarget
                .path("map/upload")
                .queryParam("filename", "mapdata.mbtiles")
                .request()
                .post(Entity.entity(Files.readAllBytes(Path.of("manager/src/map/mapdata.mbtiles")), MediaType.APPLICATION_OCTET_STREAM_TYPE))

        then: "request entity too large error should occur"
        response.status == 413
    }

    def "Configure custom tile server URL"() {
        when: "mapsettings with a valid tile server URL is configured"
        def mapSettings = ValueUtil.parse("""{
            "options" : { "default": {} },
            "sources" : {
                "vector_tiles" : {
                    "type" : "vector" ,
                    "tiles" : [ "https://example.com/tileset/{z}/{x}/{y}.mvt" ],
                    "custom" : true
                }
            }
        }""", MapConfig.class)
        def savedSettings = (ObjectNode)mapResource.saveSettings(null, mapSettings.get())

        then: "the custom tile server URL should be returned"
        savedSettings.get("sources").get("vector_tiles").get("url").textValue() == "https://example.com/tileset/{z}/{x}/{y}.mvt"

        when: "an invalid tile server URL is configured"
        mapSettings = ValueUtil.parse("""{
            "options" : { "default": {} },
            "sources" : {
                "vector_tiles" : {
                    "type" : "vector" ,
                    "tiles" : [ "https://example.com/tileset.mvt" ],
                    "custom" : true
                }
            }
        }""", MapConfig.class)
        savedSettings = (ObjectNode)mapResource.saveSettings(null, mapSettings.get())

        then: "the URL should be reset back to normal"
        savedSettings.get("sources").get("vector_tiles").get("url").textValue() == MapService.DEFAULT_VECTOR_TILES_URL
    }
}
