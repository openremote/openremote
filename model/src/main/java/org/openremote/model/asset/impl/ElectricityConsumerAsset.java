/*
 * Copyright 2020, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.asset.impl;

import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;

import javax.persistence.Entity;

import static org.openremote.model.Constants.*;
import static org.openremote.model.Constants.UNITS_GRAM;

@Entity
public class ElectricityConsumerAsset extends ElectricityAsset<ElectricityConsumerAsset> {

    public static final AssetDescriptor<ElectricityConsumerAsset> DESCRIPTOR = new AssetDescriptor<>("power-plug", "8A293D", ElectricityConsumerAsset.class);

    public static final AttributeDescriptor<Double> POWER_SETPOINT = ElectricityAsset.POWER_SETPOINT.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_IMPORT_MIN = ElectricityAsset.POWER_IMPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_IMPORT_MAX = ElectricityAsset.POWER_IMPORT_MAX.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MIN = ElectricityAsset.POWER_EXPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MAX = ElectricityAsset.POWER_EXPORT_MAX.withOptional(true);
    public static final AttributeDescriptor<Double> ENERGY_EXPORT_TOTAL = ElectricityAsset.ENERGY_EXPORT_TOTAL.withOptional(true);
    public static final AttributeDescriptor<Integer> EFFICIENCY_IMPORT = ElectricityAsset.EFFICIENCY_IMPORT.withOptional(true);
    public static final AttributeDescriptor<Integer> EFFICIENCY_EXPORT = ElectricityAsset.EFFICIENCY_EXPORT.withOptional(true);
    public static final AttributeDescriptor<Double> TARIFF_IMPORT = ElectricityAsset.TARIFF_IMPORT.withOptional(true);
    public static final AttributeDescriptor<Double> TARIFF_EXPORT = ElectricityAsset.TARIFF_EXPORT.withOptional(true);
    public static final AttributeDescriptor<Double> CARBON_IMPORT = ElectricityAsset.CARBON_IMPORT.withOptional(true);
    public static final AttributeDescriptor<Integer> CARBON_IMPORT_TOTAL = ElectricityAsset.CARBON_IMPORT_TOTAL.withOptional(true);
    public static final AttributeDescriptor<Integer> CARBON_EXPORT_TOTAL = ElectricityAsset.CARBON_EXPORT_TOTAL.withOptional(true);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ElectricityConsumerAsset() {
    }

    public ElectricityConsumerAsset(String name) {
        super(name);
    }
}
