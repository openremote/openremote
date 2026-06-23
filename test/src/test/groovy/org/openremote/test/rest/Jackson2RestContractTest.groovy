package org.openremote.test.rest

import groovy.json.JsonSlurper
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.transform.recurrence.Frequency
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.calendar.CalendarEvent
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.model.datapoint.query.AssetDatapointAllQuery
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.util.MapAccess
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static java.nio.charset.StandardCharsets.UTF_8

import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER

class Jackson2RestContractTest extends Specification implements ManagerContainerTrait {

    def "REST JSON preserves Jackson 2 model contracts"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)

        and: "an authenticated admin target is available"
        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            MapAccess.getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )
        def target = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken)

        and: "an asset contains GeoJSON and calendar values"
        def recurrence = new Recur(Frequency.DAILY, 2)
        def calendarEvent = new CalendarEvent(
            Date.from(LocalDateTime.of(2026, 2, 12, 9, 0).atZone(ZoneOffset.UTC).toInstant()),
            Date.from(LocalDateTime.of(2026, 2, 12, 10, 0).atZone(ZoneOffset.UTC).toInstant()),
            recurrence
        )
        def thing = assetStorageService.find(managerTestSetup.thingId, true)
        thing.setLocation(new GeoJSONPoint(12.34d, 56.78d, 9.0d))
        thing.addAttributes(new Attribute<>("testCalendar", ValueType.CALENDAR_EVENT, calendarEvent))
        assetStorageService.merge(thing)

        and: "a datapoint exists in a local-time range represented with an offset"
        def datapointAttribute = "light1PowerConsumption"
        def fromLocal = LocalDateTime.of(2026, 2, 12, 10, 0)
        def toLocal = fromLocal.plusMinutes(30)
        def datapointInstant = fromLocal.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant()
        assetDatapointService.upsertValues(
            managerTestSetup.thingId,
            datapointAttribute,
            [new ValueDatapoint<>(datapointInstant.toEpochMilli(), 42.5d)]
        )
        assert assetDatapointService.getDatapoints(new AttributeRef(managerTestSetup.thingId, datapointAttribute)).any { it.value == 42.5d }
        assert assetDatapointService.queryDatapoints(
            managerTestSetup.thingId,
            datapointAttribute,
            new AssetDatapointAllQuery(fromLocal, toLocal)
        ).size() == 1

        and: "the raw REST endpoint can see the seeded datapoint with local date-times"
        def timestampQueryJson = """
            {
              "type": "all",
              "fromTimestamp": ${fromLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()},
              "toTimestamp": ${toLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}
            }
        """
        assert readJson(target
            .path("asset")
            .path("datapoint")
            .path(managerTestSetup.thingId)
            .path(datapointAttribute)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(jsonEntity(timestampQueryJson))).size() == 1

        def localQueryJson = """
            {
              "type": "all",
              "fromTime": "${fromLocal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}",
              "toTime": "${toLocal.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}"
            }
        """
        assert readJson(target
            .path("asset")
            .path("datapoint")
            .path(managerTestSetup.thingId)
            .path(datapointAttribute)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(jsonEntity(localQueryJson))).size() == 1

        when: "datapoints are queried with a raw JSON body containing offset date-times"
        def queryOffset = ZoneId.systemDefault().rules.getOffset(datapointInstant).totalSeconds == 0 ? ZoneOffset.ofHours(2) : ZoneOffset.UTC
        def queryJson = """
            {
              "type": "all",
              "fromTime": "${fromLocal.atZone(ZoneId.systemDefault()).withZoneSameInstant(queryOffset).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}",
              "toTime": "${toLocal.atZone(ZoneId.systemDefault()).withZoneSameInstant(queryOffset).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}"
            }
        """
        def datapoints = readJson(target
            .path("asset")
            .path("datapoint")
            .path(managerTestSetup.thingId)
            .path(datapointAttribute)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(jsonEntity(queryJson)))

        then: "the REST request body used the custom offset-aware time deserializer"
        datapoints.size() == 1
        datapoints[0].y == 42.5d

        when: "the asset is read as a raw REST response"
        def assetJson = readJson(target
            .path("asset")
            .path(managerTestSetup.thingId)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get())

        then: "GeoJSON coordinates are emitted as coordinate arrays"
        assetJson.attributes.location.value.type == "Point"
        assetJson.attributes.location.value.coordinates == [12.34d, 56.78d, 9.0d]

        and: "calendar recurrences are emitted as RRULE strings"
        assetJson.attributes.testCalendar.value.recurrence == recurrence.toString()
    }

    private static Object readJson(Response response) {
        try {
            def body = response.readEntity(String.class)
            assert response.status == 200: body
            return new JsonSlurper().parseText(body)
        } finally {
            response.close()
        }
    }

    private static Entity<byte[]> jsonEntity(String json) {
        Entity.entity(json.getBytes(UTF_8), MediaType.APPLICATION_JSON_TYPE)
    }
}
