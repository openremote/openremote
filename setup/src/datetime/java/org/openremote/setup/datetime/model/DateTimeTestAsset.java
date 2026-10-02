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
package org.openremote.setup.datetime.model;

import static org.openremote.model.value.ValueFormat.StyleRepresentation.DIGIT_2;
import static org.openremote.model.value.ValueFormat.StyleRepresentation.MEDIUM;
import static org.openremote.model.value.ValueFormat.StyleRepresentation.NUMERIC;
import static org.openremote.model.value.ValueFormat.StyleRepresentation.SHORT;

import jakarta.persistence.Entity;
import java.time.Instant;
import java.util.Date;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueFormat;
import org.openremote.model.value.ValueType;

/**
 * Has a date and time attribute for every case of the date time input: each value type, each way a
 * value format sets the precision of the picker, and the date constraints. The label of each
 * attribute states what the picker is expected to do.
 *
 * <p>Formats and constraints are meta items, which attributes copy from their descriptor, so they
 * also show on assets created in the UI.
 */
@Entity
public class DateTimeTestAsset extends Asset<DateTimeTestAsset> {

  // Value types

  public static final AttributeDescriptor<Long> TIMESTAMP =
      new AttributeDescriptor<>(
          "timestamp",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Timestamp: minutes, writes a number"));

  public static final AttributeDescriptor<String> TIMESTAMP_ISO8601 =
      new AttributeDescriptor<>(
          "timestampISO8601",
          ValueType.TIMESTAMP_ISO8601,
          new MetaItem<>(MetaItemType.LABEL, "ISO 8601 timestamp: minutes, writes an ISO string"));

  public static final AttributeDescriptor<String> TIMESTAMP_ISO8601_SECONDS =
      new AttributeDescriptor<>(
          "timestampISO8601Seconds",
          ValueType.TIMESTAMP_ISO8601,
          new MetaItem<>(
              MetaItemType.LABEL,
              "ISO 8601 timestamp with moment ss: seconds, writes an ISO string"),
          new MetaItem<>(
              MetaItemType.FORMAT, new ValueFormat().setMomentJsFormat("DD-MM-YYYY HH:mm:ss")));

  public static final AttributeDescriptor<String> TIMESTAMP_ISO8601_MILLISECONDS =
      new AttributeDescriptor<>(
          "timestampISO8601Milliseconds",
          ValueType.TIMESTAMP_ISO8601,
          new MetaItem<>(
              MetaItemType.LABEL,
              "ISO 8601 timestamp with moment ss.SSS: milliseconds, writes an ISO string"),
          new MetaItem<>(
              MetaItemType.FORMAT, new ValueFormat().setMomentJsFormat("DD-MM-YYYY HH:mm:ss.SSS")));

  public static final AttributeDescriptor<Date> DATE_AND_TIME =
      new AttributeDescriptor<>(
          "dateAndTime",
          ValueType.DATE_AND_TIME,
          new MetaItem<>(MetaItemType.LABEL, "Date and time: minutes, writes a number"));

  public static final AttributeDescriptor<Long> LONG_AS_DATE =
      new AttributeDescriptor<>(
          "longAsDate",
          ValueType.LONG,
          new MetaItem<>(MetaItemType.LABEL, "Long as date: minutes"),
          new MetaItem<>(MetaItemType.FORMAT, new ValueFormat().setAsDate(true)));

  public static final AttributeDescriptor<Long> EMPTY_TIMESTAMP =
      new AttributeDescriptor<>(
          "emptyTimestamp",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Empty timestamp: minutes"));

  // Precision from a moment format

  public static final AttributeDescriptor<Long> MOMENT_SECONDS =
      new AttributeDescriptor<>(
          "momentSeconds",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Moment ss: seconds"),
          new MetaItem<>(
              MetaItemType.FORMAT, new ValueFormat().setMomentJsFormat("DD-MM-YYYY HH:mm:ss")));

  public static final AttributeDescriptor<Long> MOMENT_MILLISECONDS =
      new AttributeDescriptor<>(
          "momentMilliseconds",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Moment ss.SSS: milliseconds"),
          new MetaItem<>(
              MetaItemType.FORMAT, new ValueFormat().setMomentJsFormat("DD-MM-YYYY HH:mm:ss.SSS")));

  public static final AttributeDescriptor<Long> MOMENT_UNIX_MILLISECONDS =
      new AttributeDescriptor<>(
          "momentUnixMilliseconds",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Moment x: milliseconds"),
          new MetaItem<>(MetaItemType.FORMAT, new ValueFormat().setMomentJsFormat("x")));

  public static final AttributeDescriptor<Long> MOMENT_UNIX_SECONDS =
      new AttributeDescriptor<>(
          "momentUnixSeconds",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Moment X: seconds"),
          new MetaItem<>(MetaItemType.FORMAT, new ValueFormat().setMomentJsFormat("X")));

  public static final AttributeDescriptor<Long> MOMENT_LOCALIZED_SECONDS =
      new AttributeDescriptor<>(
          "momentLocalizedSeconds",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Moment L LTS: seconds"),
          new MetaItem<>(MetaItemType.FORMAT, new ValueFormat().setMomentJsFormat("L LTS")));

  public static final AttributeDescriptor<Long> MOMENT_LITERAL_LAST_SEEN =
      new AttributeDescriptor<>(
          "momentLiteralLastSeen",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Moment [Last seen] literal: minutes"),
          new MetaItem<>(
              MetaItemType.FORMAT,
              new ValueFormat().setMomentJsFormat("[Last seen] DD-MM-YYYY HH:mm")));

  public static final AttributeDescriptor<Long> MOMENT_LITERAL_SENSOR =
      new AttributeDescriptor<>(
          "momentLiteralSensor",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Moment [Sensor] literal: minutes"),
          new MetaItem<>(
              MetaItemType.FORMAT,
              new ValueFormat().setMomentJsFormat("[Sensor] DD-MM-YYYY HH:mm")));

  // Precision from Intl options, with asDate so the display formats the epoch milliseconds as dates

  public static final AttributeDescriptor<Date> INTL_SECONDS =
      new AttributeDescriptor<>(
          "intlSeconds",
          ValueType.DATE_AND_TIME,
          new MetaItem<>(MetaItemType.LABEL, "Intl second: seconds"),
          new MetaItem<>(MetaItemType.FORMAT, dateTimeFormat().setSecond(DIGIT_2)));

  public static final AttributeDescriptor<Date> INTL_FRACTIONAL_SECONDS =
      new AttributeDescriptor<>(
          "intlFractionalSeconds",
          ValueType.DATE_AND_TIME,
          new MetaItem<>(MetaItemType.LABEL, "Intl fractionalSecondDigits: milliseconds"),
          new MetaItem<>(
              MetaItemType.FORMAT,
              dateTimeFormat().setSecond(DIGIT_2).setFractionalSecondDigits(3)));

  public static final AttributeDescriptor<Date> INTL_TIME_STYLE_MEDIUM =
      new AttributeDescriptor<>(
          "intlTimeStyleMedium",
          ValueType.DATE_AND_TIME,
          new MetaItem<>(MetaItemType.LABEL, "Intl timeStyle medium: seconds"),
          new MetaItem<>(
              MetaItemType.FORMAT,
              new ValueFormat().setAsDate(true).setDateStyle(SHORT).setTimeStyle(MEDIUM)));

  public static final AttributeDescriptor<Date> INTL_TIME_STYLE_SHORT =
      new AttributeDescriptor<>(
          "intlTimeStyleShort",
          ValueType.DATE_AND_TIME,
          new MetaItem<>(MetaItemType.LABEL, "Intl timeStyle short: minutes"),
          new MetaItem<>(
              MetaItemType.FORMAT,
              new ValueFormat().setAsDate(true).setDateStyle(SHORT).setTimeStyle(SHORT)));

  // The display elsewhere uses the built-in moment format of the timestamp type, in minutes
  public static final AttributeDescriptor<Long> TIMESTAMP_WITH_SECOND =
      new AttributeDescriptor<>(
          "timestampWithSecond",
          ValueType.TIMESTAMP,
          new MetaItem<>(
              MetaItemType.LABEL,
              "Timestamp with Intl second: seconds, displayed elsewhere in minutes"),
          new MetaItem<>(MetaItemType.FORMAT, new ValueFormat().setSecond(DIGIT_2)));

  // Constraints and state

  public static final AttributeDescriptor<Long> PAST =
      new AttributeDescriptor<>(
          "past",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Past: latest selectable is now"),
          new MetaItem<>(
              MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.Past())));

  public static final AttributeDescriptor<Long> FUTURE =
      new AttributeDescriptor<>(
          "future",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Future: earliest selectable is the minute after now"),
          new MetaItem<>(
              MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.Future())));

  public static final AttributeDescriptor<Long> FUTURE_OR_PRESENT =
      new AttributeDescriptor<>(
          "futureOrPresent",
          ValueType.TIMESTAMP,
          new MetaItem<>(
              MetaItemType.LABEL, "Future or present: earliest selectable is the minute after now"),
          new MetaItem<>(
              MetaItemType.CONSTRAINTS,
              ValueConstraint.constraints(new ValueConstraint.FutureOrPresent())));

  public static final AttributeDescriptor<Long> MIN_BETWEEN_MINUTES =
      new AttributeDescriptor<>(
          "minBetweenMinutes",
          ValueType.TIMESTAMP,
          new MetaItem<>(
              MetaItemType.LABEL,
              "Min 10:30:45 on 2 Jan 2026 (Amsterdam): earliest selectable that day is 10:31"),
          new MetaItem<>(
              MetaItemType.CONSTRAINTS,
              ValueConstraint.constraints(
                  new ValueConstraint.Min(
                      Instant.parse("2026-01-02T09:30:45.123Z").toEpochMilli()))));

  public static final AttributeDescriptor<Long> RANGE =
      new AttributeDescriptor<>(
          "range",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Range: only 2026 (UTC)"),
          new MetaItem<>(
              MetaItemType.CONSTRAINTS,
              ValueConstraint.constraints(
                  new ValueConstraint.Min(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()),
                  new ValueConstraint.Max(Instant.parse("2026-12-31T23:59:59Z").toEpochMilli()))));

  public static final AttributeDescriptor<Long> READ_ONLY_TIMESTAMP =
      new AttributeDescriptor<>(
          "readOnlyTimestamp",
          ValueType.TIMESTAMP,
          new MetaItem<>(MetaItemType.LABEL, "Read only timestamp: minutes"),
          new MetaItem<>(MetaItemType.READ_ONLY, true));

  public static final AssetDescriptor<DateTimeTestAsset> DESCRIPTOR =
      new AssetDescriptor<>("calendar-clock", "3d85c6", DateTimeTestAsset.class);

  protected DateTimeTestAsset() {}

  public DateTimeTestAsset(String name) {
    super(name);
  }

  private static ValueFormat dateTimeFormat() {
    return new ValueFormat()
        .setAsDate(true)
        .setYear(NUMERIC)
        .setMonth(DIGIT_2)
        .setDay(DIGIT_2)
        .setHour(DIGIT_2)
        .setMinute(DIGIT_2);
  }
}
