package org.openremote.manager.datapoint

import org.openremote.model.datapoint.query.AssetDatapointAllQuery
import org.openremote.model.datapoint.query.AssetDatapointQuery
import org.openremote.model.util.ValueUtil
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

class AssetDatapointQueryDeserializationTest extends Specification {

    def "AssetDatapointQuery deserializes fromTime without timezone as LocalDateTime"() {
        given:
        def json = '''
            {
              "type": "all",
              "fromTime": "2026-02-12T00:00:00.000",
              "toTime": "2026-02-12T01:00:00.000"
            }
        '''

        when:
        AssetDatapointQuery query = ValueUtil.JSON.readValue(json, AssetDatapointQuery.class)

        then:
        query instanceof AssetDatapointAllQuery
        query.fromTime == LocalDateTime.of(2026, 2, 12, 0, 0, 0, 0)
        query.toTime == LocalDateTime.of(2026, 2, 12, 1, 0, 0, 0)
    }

    def "AssetDatapointQuery deserializes fromTime with Z and converts to server timezone"() {
        given:
        def json = '''
            {
              "type": "all",
              "fromTime": "2026-02-12T00:00:00.000Z",
              "toTime": "2026-02-12T01:00:00.000Z"
            }
        '''

        when:
        AssetDatapointQuery query = ValueUtil.JSON.readValue(json, AssetDatapointQuery.class)

        then:
        query instanceof AssetDatapointAllQuery
        query.fromTime == OffsetDateTime.parse("2026-02-12T00:00:00.000Z")
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
        query.toTime == OffsetDateTime.parse("2026-02-12T01:00:00.000Z")
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    def "AssetDatapointQuery deserializes fromTime with explicit numeric offset and converts to server timezone"() {
        given:
        def json = '''
            {
              "type": "all",
              "fromTime": "2026-02-12T00:00:00.000+02:00",
              "toTime": "2026-02-12T01:00:00.000+02:00"
            }
        '''

        when:
        AssetDatapointQuery query = ValueUtil.JSON.readValue(json, AssetDatapointQuery.class)

        then:
        query instanceof AssetDatapointAllQuery
        query.fromTime == OffsetDateTime.parse("2026-02-12T00:00:00.000+02:00")
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
        query.toTime == OffsetDateTime.parse("2026-02-12T01:00:00.000+02:00")
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
    }
}
