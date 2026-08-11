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
package org.openremote.model.geo

import spock.lang.Specification

class GeoJSONPointTest extends Specification {

    def "parseRawLocation returns null for invalid input '#input'"() {
        expect:
        GeoJSONPoint.parseRawLocation(input) == null

        where:
        input << [null, "", "1,2,3", "abc,2", "1,def"]
    }

    def "parseRawLocation parses valid input"() {
        when:
        def point = GeoJSONPoint.parseRawLocation("12.5, -3.75")

        then:
        point != null
        point.x == 12.5d
        point.y == -3.75d
    }

    def "offsetByMeters offsets east and north"() {
        given:
        def origin = new GeoJSONPoint(0d, 0d)

        when:
        def offset = origin.offsetByMeters(1000d, 1000d)

        then:
        def expectedLat = 0.0090436947d
        def expectedLon = 0.0089831528d
        Math.abs(offset.y - expectedLat) < 1e-9
        Math.abs(offset.x - expectedLon) < 1e-9
    }
}
