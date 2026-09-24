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
package org.openremote.agent.protocol.simulator

import spock.lang.Specification

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class SimulatorAgentProtocolScheduleTest extends Specification {

  private static final long SECOND = 1000
  private static final long MINUTE = SECOND * 60
  private static final long HOUR = MINUTE * 60
  private static final long DAY = HOUR * 24

  def "a non-recurring schedule returns its start forever"() {
    given:
    def start = instant("2000-01-01T00:00:00.000Z")
    def schedule = schedule(start)

    expect:
    [
      instant("1999-01-01T00:00:00.000Z"),
      start,
      instant("9999-12-31T23:59:59.999Z")
    ].each { assert schedule.tryAdvanceActive(it, 0) == start }
  }

  def "a non-recurring schedule with an end keeps returning its start"() {
    given:
    def start = instant("2000-01-01T00:00:00.000Z")
    def end = instant("2000-01-10T00:00:00.000Z")
    def schedule = schedule(start, end)

    expect:
    ([instant("1999-01-01T00:00:00.000Z")] + (0..9).collect { start + DAY * it })
    .each { assert schedule.tryAdvanceActive(it, 0) == start }

    and: "the end is checked separately"
    schedule.isAfterScheduleEnd(end + 1)
    schedule.tryAdvanceActive(end + 1, 0) == start
  }

  def "a daily recurrence returns its last occurrence after UNTIL"() {
    given:
    def start = instant("2000-01-01T00:00:00.000Z")
    def schedule = schedule(start, null, "FREQ=DAILY;UNTIL=20000104T235959")

    expect:
    schedule.tryAdvanceActive(instant("1999-01-01T00:00:00.000Z") + DAY - 1, 0) == start
    (0..3).each { day ->
      def occurrence = start + DAY * day
      assert schedule.tryAdvanceActive(occurrence, 0) == occurrence
      assert schedule.tryAdvanceActive(occurrence + DAY - 1, 0) == occurrence
    }

    and:
    def lastOccurrence = instant("2000-01-04T00:00:00.000Z")
    schedule.tryAdvanceActive(lastOccurrence, 0) == lastOccurrence
    schedule.tryAdvanceActive(lastOccurrence + DAY, 0) == lastOccurrence
  }

  def "a daily recurrence returns its last occurrence after COUNT"() {
    given:
    def start = instant("2000-01-01T00:00:00.000Z")
    def schedule = schedule(start, null, "FREQ=DAILY;COUNT=4")

    expect:
    schedule.tryAdvanceActive(instant("1999-01-01T00:00:00.000Z") + DAY - 1, 0) == start
    (0..2).each { day ->
      def occurrence = start + DAY * day
      assert schedule.tryAdvanceActive(occurrence, 0) == occurrence
      assert schedule.tryAdvanceActive(occurrence + DAY - 1, 0) == occurrence
    }

    and:
    def lastOccurrence = instant("2000-01-04T00:00:00.000Z")
    schedule.tryAdvanceActive(lastOccurrence, 0) == lastOccurrence
    schedule.tryAdvanceActive(lastOccurrence + DAY, 0) == lastOccurrence
  }

  def "a recurrence with BYHOUR and BYMINUTE starts at the configured time"() {
    given:
    def start = instant("2000-01-01T00:00:00.000Z")
    def schedule = schedule(start, null, "FREQ=DAILY;BYHOUR=17;BYMINUTE=30")

    expect:
    schedule.tryAdvanceActive(
            instant("1999-01-01T00:00:00.000Z") + DAY - 1,
            0
            ) == start + 17 * HOUR + 30 * MINUTE
  }

  def "a minutely recurrence returns its last occurrence after UNTIL"() {
    given:
    def start = instant("2000-01-01T00:00:00.000Z")
    // A trailing Z would make UNTIL an OffsetDateTime, while ical4j compares LocalDateTime
    // candidates and therefore ends the recurrence differently.
    def schedule = schedule(start, null, "FREQ=MINUTELY;UNTIL=20000101T000500")

    expect:
    schedule.tryAdvanceActive(instant("1999-01-01T00:00:00.000Z") + MINUTE - 1, 0) == start
    (0..3).each { minute ->
      def occurrence = start + MINUTE * minute
      assert schedule.tryAdvanceActive(occurrence, 0) == occurrence
      assert schedule.tryAdvanceActive(occurrence + MINUTE - 1, 0) == occurrence
    }

    and:
    def lastOccurrence = instant("2000-01-01T00:04:00.000Z")
    schedule.tryAdvanceActive(lastOccurrence + MINUTE, 0) == lastOccurrence
    schedule.tryAdvanceActive(lastOccurrence + 2 * MINUTE, 0) == lastOccurrence
  }

  def "a recurrence catches up with the current time"() {
    given:
    def start = instant("2000-01-01T00:00:00.000Z")
    def schedule = schedule(start, null, "FREQ=MINUTELY")
    def now = instant("2000-01-01T01:00:00.000Z")

    expect:
    schedule.tryAdvanceActive(now, 0) == start + 60 * MINUTE
    schedule.tryAdvanceActive(now + MINUTE, 0) == start + 61 * MINUTE
  }

  def "an hourly recurrence calculates offset delays"() {
    given:
    def start = instant("2000-01-01T00:00:00.000Z")
    def schedule = schedule(start, null, "FREQ=HOURLY")

    expect:
    delays(schedule, [
      (start): 100 * SECOND,
      (instant("2000-01-01T00:01:39.000Z")): SECOND,
      (instant("2000-01-01T00:01:40.000Z")): 3600 * SECOND
    ])
  }

  def "an hourly recurrence with a future start calculates offset delays"() {
    given:
    def schedule = schedule(
            instant("2000-01-02T00:00:00.000Z"),
            null,
            "FREQ=HOURLY"
            )

    expect:
    delays(schedule, [
      (instant("2000-01-01T00:00:00.000Z")): DAY + 100 * SECOND,
      (instant("2000-01-01T00:01:00.000Z")): DAY + 40 * SECOND,
      (instant("2000-01-01T00:02:00.000Z")): DAY - 20 * SECOND
    ])
  }

  def "an hourly recurrence with UNTIL calculates delays through its last occurrence"() {
    given:
    def schedule = schedule(
            instant("2000-01-01T00:00:00.000Z"),
            null,
            "FREQ=HOURLY;UNTIL=20000101T020000"
            )

    expect:
    delays(schedule, recurringDelayExpectations())

    and: "UNTIL is checked separately while delays remain positive before UNTIL plus offset"
    schedule.isAfterScheduleEnd(instant("2000-01-01T03:00:00.001Z"))
  }

  def "an hourly recurrence with COUNT calculates delays through its last occurrence"() {
    given:
    def schedule = schedule(
            instant("2000-01-01T00:00:00.000Z"),
            null,
            "FREQ=HOURLY;COUNT=3"
            )

    expect:
    delays(schedule, recurringDelayExpectations())
  }

  private static void delays(
          SimulatorProtocol.Schedule schedule,
          Map<Long, Long> expectedDelays
  ) {
    expectedDelays.each { now, expected ->
      def timeSinceOccurrenceStarted = now - schedule.tryAdvanceActive(now, 0)
      assert schedule.getDelay(100, timeSinceOccurrenceStarted).asLong == expected
    }
  }

  private static Map<Long, Long> recurringDelayExpectations() {
    [
      (instant("2000-01-01T00:00:00.000Z")): 100 * SECOND,
      (instant("2000-01-01T00:01:00.000Z")): 40 * SECOND,
      (instant("2000-01-01T00:02:00.000Z")): 3580 * SECOND,
      (instant("2000-01-01T01:00:00.000Z")): 100 * SECOND,
      (instant("2000-01-01T01:01:00.000Z")): 40 * SECOND,
      (instant("2000-01-01T01:02:00.000Z")): 3580 * SECOND,
      (instant("2000-01-01T02:00:00.000Z")): 100 * SECOND,
      (instant("2000-01-01T02:01:00.000Z")): 40 * SECOND,
      (instant("2000-01-01T02:02:00.000Z")): -20 * SECOND,
      (instant("2000-01-01T03:00:00.000Z")): -3500 * SECOND
    ]
  }

  private static SimulatorProtocol.Schedule schedule(
          long start,
          Long end = null,
          String recurrence = null
  ) {
    new SimulatorProtocol.Schedule(
            LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneOffset.UTC),
            end == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneOffset.UTC),
            recurrence
            )
  }

  private static long instant(String value) {
    Instant.parse(value).toEpochMilli()
  }
}
