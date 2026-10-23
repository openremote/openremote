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
package org.openremote.model.query

import org.openremote.model.query.filter.*
import org.openremote.model.util.ValueUtil
import spock.lang.Specification

class AssetQueryCopyTest extends Specification {

  def "Copying an asset query preserves and isolates #predicate.class.simpleName"() {
    given:
    def original = new AssetQuery().attributes(new AttributePredicate("value", predicate))
    // Serialization is used only to compare all predicate options, not to make the copy.
    def expected = ValueUtil.JSON.writeValueAsString(predicate)

    when:
    def copied = new AssetQuery(original).attributes.items[0].value

    then:
    copied.class == predicate.class
    !copied.is(predicate)
    ValueUtil.JSON.writeValueAsString(copied) == expected

    when:
    mutate(predicate)

    then:
    ValueUtil.JSON.writeValueAsString(copied) == expected

    where:
    predicate | mutate
    new StringPredicate(AssetQuery.Match.CONTAINS, false, "match").negate(true) | {
      it.value = "next"
    }
    new BooleanPredicate(true) | { it.value = false }
    new NumberPredicate(new BigDecimal("1234567890.123456789"), AssetQuery.Operator.BETWEEN).rangeValue(99).negate(true) | {
      it.rangeValue = 100
    }
    new DateTimePredicate("2026-01-01", "2026-12-31").negate(true) | {
      it.rangeValue = "2027-01-01"
    }
    new ArrayPredicate([values: [1, 2]], 0, 3, 1, 5, true) | { it.value.values.add(3) }
    new RadialGeofencePredicate(100, 51, 4, true) | { it.radius = 200 }
    new RectangularGeofencePredicate(50, 3, 52, 5, true) | { it.latMax = 53 }
    new GeoJSONGeofencePredicate('{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}', true) | {
      it.geoJSON = null
    }
    new CalendarEventPredicate(new Date(1800000000000L)) | { it.timestamp.time = 0 }
    new ValueEmptyPredicate().negate(true) | { it.negate = false }
    new ValueAnyPredicate() | { }
    new ExtendedStringPredicate(value: "match", extra: "custom") | { it.extra = "next" }
  }

  def "Copying an asset query preserves null and empty options"() {
    given:
    def original = new AssetQuery(
            ids: [] as String[], names: [] as StringPredicate[], parents: [] as ParentPredicate[],
            paths: [] as PathPredicate[], userIds: [] as String[], types: [] as String[],
            select: new AssetQuery.Select().excludeAttributes(), attributes: new LogicGroup())
    original.attributes.items = []
    original.attributes.groups = []

    when:
    def copied = new AssetQuery(original)
    def blank = new AssetQuery(new AssetQuery())

    then:
    ["ids", "names", "parents", "paths", "userIds", "types"].every {
      copied[it].length == 0 && !copied[it].is(original[it]) && blank[it] == null
    }
    copied.select.attributes.length == 0
    !copied.select.attributes.is(original.select.attributes)
    copied.attributes.items.empty
    copied.attributes.groups.empty
    !copied.attributes.items.is(original.attributes.items)
    !copied.attributes.groups.is(original.attributes.groups)
    copied.@includeDeletePending == null
    blank.select == null
    blank.attributes == null
  }

  def "Copying location predicates retains their type and isolates their metadata"() {
    given:
    def predicate = new LocationAttributePredicate(new RadialGeofencePredicate(100, 51, 4))
    predicate.meta(new NameValuePredicate("label", new StringPredicate("original")))
    predicate.timestampOlderThan = "PT5M"

    when:
    def copied = new AssetQuery(new AssetQuery().attributes(predicate)).attributes.items[0]
    predicate.value.radius = 200
    predicate.meta[0].name.value = "next"
    predicate.timestampOlderThan = "PT10M"

    then:
    copied instanceof LocationAttributePredicate
    copied.value.radius == 100
    copied.meta[0].name.value == "label"
    copied.timestampOlderThan == "PT5M"
  }

  def "Copying array predicate payloads isolates nested mutable values"() {
    given:
    def payload = [numbers: [1, 2] as int[], records: [[value: "original"]],
      time: new Date(1800000000000L), tree: ValueUtil.JSON.readTree('{"value":1}'),
      counter: new java.util.concurrent.atomic.AtomicInteger(7)]
    def predicate = new ArrayPredicate(payload, null, null, null, null, false)

    when:
    def copied = new AssetQuery(new AssetQuery().attributes(new AttributePredicate("value", predicate)))
            .attributes.items[0].value.value
    payload.numbers[0] = 99
    payload.records[0].value = "next"
    payload.time.time = 0
    payload.tree.put("value", 2)
    payload.counter.set(8)

    then:
    copied.numbers instanceof int[]
    copied.numbers.toList() == [1, 2]
    copied.records == [[value: "original"]]
    copied.time.time == 1800000000000L
    copied.tree.get("value").intValue() == 1
    copied.counter.get() == 7
  }

  static class ExtendedStringPredicate extends StringPredicate {
    String extra
  }
}
