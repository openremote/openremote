/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.extension.demosetup.model;

import static org.openremote.model.Constants.*;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import jakarta.persistence.Entity;

@Entity
public class IrrigationAsset extends Asset<IrrigationAsset> {

  public static final AttributeDescriptor<Double> FLOW_WATER =
      new AttributeDescriptor<>("flowWater", ValueType.POSITIVE_NUMBER)
          .withUnits(UNITS_LITRE, UNITS_PER, UNITS_HOUR);
  public static final AttributeDescriptor<Double> FLOW_NUTRIENTS =
      new AttributeDescriptor<>("flowNutrients", ValueType.POSITIVE_NUMBER)
          .withUnits(UNITS_LITRE, UNITS_PER, UNITS_HOUR);
  public static final AttributeDescriptor<Double> FLOW_TOTAL =
      new AttributeDescriptor<>("flowTotal", ValueType.POSITIVE_NUMBER)
          .withUnits(UNITS_LITRE, UNITS_PER, UNITS_HOUR);
  public static final AttributeDescriptor<Double> TANK_LEVEL =
      new AttributeDescriptor<>("tankLevel", ValueType.POSITIVE_NUMBER).withUnits(UNITS_LITRE);

  public static final AssetDescriptor<IrrigationAsset> DESCRIPTOR =
      new AssetDescriptor<>("water-pump", "3d85c6", IrrigationAsset.class);

  /** For use by hydrators (i.e. JPA/Jackson) */
  protected IrrigationAsset() {}

  public IrrigationAsset(String name) {
    super(name);
  }
}
