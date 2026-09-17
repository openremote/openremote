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
package org.openremote.setup.datetime;

import static org.openremote.model.Constants.MASTER_REALM;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.openremote.manager.setup.ManagerSetup;
import org.openremote.model.Container;
import org.openremote.model.asset.impl.GroupAsset;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.setup.datetime.model.DateTimeTestAsset;

/**
 * Creates a {@link DateTimeTestAsset} in the master realm. Every attribute except the empty one
 * gets the same instant with seconds and milliseconds, so trimming to the precision of each picker
 * shows, in the representation of its value type.
 *
 * <p>The asset is the child of a group, whose asset page lists the values as display text, and has
 * a location, so the map card shows the display text too.
 */
public class ManagerDateTimeSetup extends ManagerSetup {

  private static final Logger LOG = Logger.getLogger(ManagerDateTimeSetup.class.getName());

  /** 2026-01-02T10:30:45.123 in Europe/Amsterdam. */
  public static final Instant VALUE = Instant.parse("2026-01-02T09:30:45.123Z");

  /** Rotterdam, as longitude and latitude. */
  public static final GeoJSONPoint LOCATION =
      new GeoJSONPoint(4.4840943240837134, 51.91550228909358);

  public ManagerDateTimeSetup(Container container) {
    super(container);
  }

  @Override
  public void onStart() throws Exception {
    super.onStart();

    GroupAsset group = new GroupAsset("Date Time Test Assets", DateTimeTestAsset.class);
    group.setRealm(MASTER_REALM);
    group = assetStorageService.merge(group);

    DateTimeTestAsset asset = new DateTimeTestAsset("Date Time Test Asset");
    asset.setParent(group);
    asset.setLocation(LOCATION);

    List<AttributeDescriptor<Long>> timestamps =
        List.of(
            DateTimeTestAsset.TIMESTAMP,
            DateTimeTestAsset.LONG_AS_DATE,
            DateTimeTestAsset.MOMENT_SECONDS,
            DateTimeTestAsset.MOMENT_MILLISECONDS,
            DateTimeTestAsset.MOMENT_UNIX_MILLISECONDS,
            DateTimeTestAsset.MOMENT_UNIX_SECONDS,
            DateTimeTestAsset.MOMENT_LOCALIZED_SECONDS,
            DateTimeTestAsset.MOMENT_LITERAL_LAST_SEEN,
            DateTimeTestAsset.MOMENT_LITERAL_SENSOR,
            DateTimeTestAsset.TIMESTAMP_WITH_SECOND,
            DateTimeTestAsset.PAST,
            DateTimeTestAsset.RANGE,
            DateTimeTestAsset.READ_ONLY_TIMESTAMP);
    timestamps.forEach(
        descriptor -> asset.getAttributes().getOrCreate(descriptor).setValue(VALUE.toEpochMilli()));

    List<AttributeDescriptor<String>> isoTimestamps =
        List.of(
            DateTimeTestAsset.TIMESTAMP_ISO8601,
            DateTimeTestAsset.TIMESTAMP_ISO8601_SECONDS,
            DateTimeTestAsset.TIMESTAMP_ISO8601_MILLISECONDS);
    isoTimestamps.forEach(
        descriptor -> asset.getAttributes().getOrCreate(descriptor).setValue(VALUE.toString()));

    List<AttributeDescriptor<Date>> dates =
        List.of(
            DateTimeTestAsset.DATE_AND_TIME,
            DateTimeTestAsset.INTL_SECONDS,
            DateTimeTestAsset.INTL_FRACTIONAL_SECONDS,
            DateTimeTestAsset.INTL_TIME_STYLE_MEDIUM,
            DateTimeTestAsset.INTL_TIME_STYLE_SHORT);
    dates.forEach(
        descriptor -> asset.getAttributes().getOrCreate(descriptor).setValue(Date.from(VALUE)));

    // A future constraint needs a value after now
    asset
        .getAttributes()
        .getOrCreate(DateTimeTestAsset.FUTURE)
        .setValue(Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli());

    DateTimeTestAsset stored = assetStorageService.merge(asset);
    LOG.info("Created date time test asset " + stored.getId() + " in group " + group.getId());
  }
}
