/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.query.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.hibernate.internal.util.SerializationHelper;

@JsonSubTypes({
  @JsonSubTypes.Type(value = StringPredicate.class, name = StringPredicate.name),
  @JsonSubTypes.Type(value = BooleanPredicate.class, name = BooleanPredicate.name),
  @JsonSubTypes.Type(value = DateTimePredicate.class, name = DateTimePredicate.name),
  @JsonSubTypes.Type(value = NumberPredicate.class, name = NumberPredicate.name),
  @JsonSubTypes.Type(value = RadialGeofencePredicate.class, name = RadialGeofencePredicate.name),
  @JsonSubTypes.Type(
      value = RectangularGeofencePredicate.class,
      name = RectangularGeofencePredicate.name),
  @JsonSubTypes.Type(value = GeoJSONGeofencePredicate.class, name = GeoJSONGeofencePredicate.name),
  @JsonSubTypes.Type(value = ArrayPredicate.class, name = ArrayPredicate.name),
  @JsonSubTypes.Type(value = ValueAnyPredicate.class, name = ValueAnyPredicate.name),
  @JsonSubTypes.Type(value = ValueEmptyPredicate.class, name = ValueEmptyPredicate.name),
  @JsonSubTypes.Type(value = CalendarEventPredicate.class, name = CalendarEventPredicate.name)
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "predicateType")
public abstract class ValuePredicate implements Serializable {

  /** Copies a predicate and its mutable values, retaining their types and null/empty values. */
  public static ValuePredicate copy(ValuePredicate source) {
    return switch (source) {
      case null -> null;
      case StringPredicate p when p.getClass() == StringPredicate.class ->
          new StringPredicate(p.match, p.caseSensitive, p.value).negate(p.negate);
      case BooleanPredicate p when p.getClass() == BooleanPredicate.class ->
          new BooleanPredicate(p.value);
      case NumberPredicate p when p.getClass() == NumberPredicate.class -> {
        NumberPredicate result = new NumberPredicate((Number) copyValue(p.value), p.operator);
        result.rangeValue = (Number) copyValue(p.rangeValue);
        result.negate = p.negate;
        yield result;
      }
      case DateTimePredicate p when p.getClass() == DateTimePredicate.class -> {
        DateTimePredicate result = new DateTimePredicate(p.operator, p.value);
        result.rangeValue = p.rangeValue;
        result.negate = p.negate;
        yield result;
      }
      case ArrayPredicate p when p.getClass() == ArrayPredicate.class ->
          new ArrayPredicate(
              copyValue(p.value),
              p.index,
              p.lengthEquals,
              p.lengthGreaterThan,
              p.lengthLessThan,
              p.negated);
      case CalendarEventPredicate p when p.getClass() == CalendarEventPredicate.class ->
          new CalendarEventPredicate(p.timestamp == null ? null : (Date) p.timestamp.clone());
      case RadialGeofencePredicate p when p.getClass() == RadialGeofencePredicate.class ->
          new RadialGeofencePredicate(p.radius, p.lat, p.lng, p.negated);
      case RectangularGeofencePredicate p when p.getClass() == RectangularGeofencePredicate.class ->
          new RectangularGeofencePredicate(p.latMin, p.lngMin, p.latMax, p.lngMax, p.negated);
      case GeoJSONGeofencePredicate p when p.getClass() == GeoJSONGeofencePredicate.class ->
          new GeoJSONGeofencePredicate(p.geoJSON, p.negated);
      case ValueEmptyPredicate p when p.getClass() == ValueEmptyPredicate.class ->
          new ValueEmptyPredicate().negate(p.negate);
      case ValueAnyPredicate p when p.getClass() == ValueAnyPredicate.class ->
          new ValueAnyPredicate();
      // ValuePredicate's Serializable contract also allows custom predicate implementations.
      default -> (ValuePredicate) SerializationHelper.clone(source);
    };
  }

  private static Object copyValue(Object value) {
    if (value == null
        || value instanceof String
        || value instanceof Boolean
        || value instanceof Character
        || value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long
        || value instanceof Float
        || value instanceof Double
        || value instanceof BigInteger
        || value instanceof BigDecimal
        || value instanceof Enum<?>) {
      return value;
    }
    if (value instanceof JsonNode node) return node.deepCopy();
    if (value instanceof Date date) return date.clone();
    if (value.getClass().isArray()) {
      int length = Array.getLength(value);
      Object result = Array.newInstance(value.getClass().getComponentType(), length);
      if (value.getClass().getComponentType().isPrimitive()) {
        System.arraycopy(value, 0, result, 0, length);
      } else {
        for (int i = 0; i < length; i++) Array.set(result, i, copyValue(Array.get(value, i)));
      }
      return result;
    }
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> result = new LinkedHashMap<>();
      map.forEach((key, item) -> result.put(copyValue(key), copyValue(item)));
      return result;
    }
    if (value instanceof List<?> list) {
      List<Object> result = new ArrayList<>(list.size());
      list.forEach(item -> result.add(copyValue(item)));
      return result;
    }
    if (value instanceof Set<?> set) {
      Set<Object> result = new LinkedHashSet<>();
      set.forEach(item -> result.add(copyValue(item)));
      return result;
    }
    if (value instanceof Serializable serializable) return SerializationHelper.clone(serializable);
    throw new IllegalArgumentException(
        "Cannot copy predicate value of type: " + value.getClass().getName());
  }

  public static Predicate<Object> asPredicateOrTrue(
      Supplier<Long> currentMillisSupplier, ValuePredicate valuePredicate) {
    return obj ->
        Optional.ofNullable(valuePredicate)
            .map(vp -> vp.asPredicate(currentMillisSupplier).test(obj))
            .orElse(true);
  }

  public abstract Predicate<Object> asPredicate(Supplier<Long> currentMillisSupplier);
}
