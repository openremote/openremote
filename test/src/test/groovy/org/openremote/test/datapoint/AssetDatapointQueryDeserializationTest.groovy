/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
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
