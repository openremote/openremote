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
package org.openremote.container.timer

import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class PseudoClockTest extends Specification {

    TimerService.Clock clock

    def setup() {
        clock = TimerService.Clock.PSEUDO
        clock.init()
        clock.stop()
    }

    @Unroll
    def "setTime sets #date at midnight in #zone to #expected milliseconds"() {
        expect:
        clock.setTime(LocalDate.parse(date), LocalTime.MIDNIGHT, ZoneId.of(zone)) == expected
        clock.currentTimeMillis == expected

        where:
        date         | zone  || expected
        "1970-01-01" | "UTC" || 0L
        "1970-01-02" | "UTC" || 24L * 3_600_000
        "1970-01-01" | "CET" || -3_600_000L
        "1970-01-02" | "CET" || 23L * 3_600_000
    }

    @Unroll
    def "setTime sets #timestamp to #expected milliseconds"() {
        expect:
        clock.setTime(timestamp) == expected
        clock.currentTimeMillis == expected

        where:
        timestamp                       || expected
        "1970-01-01T00:00:00.000Z"      || 0L
        "1970-01-02T00:00:00.000Z"      || 24L * 3_600_000
        "1970-01-01T00:00:00.000+01:00" || -3_600_000L
        "1970-01-02T00:00:00.000+01:00" || 23L * 3_600_000
    }
}
