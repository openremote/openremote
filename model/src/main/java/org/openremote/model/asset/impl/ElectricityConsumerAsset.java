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
import org.openremote.model.value.*;

import javax.persistence.Entity;

import static org.openremote.model.Constants.UNITS_KILO;
import static org.openremote.model.Constants.UNITS_WATT;

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
    public static final AttributeDescriptor<Double> TARIFF_IMPORT = ElectricitySupplierAsset.TARIFF_IMPORT.withOptional(true);
    public static final AttributeDescriptor<Double> TARIFF_EXPORT = ElectricitySupplierAsset.TARIFF_EXPORT.withOptional(true);
    public static final AttributeDescriptor<Double> CARBON_IMPORT = ElectricitySupplierAsset.CARBON_IMPORT.withOptional(true);

    public static final AttributeDescriptor<Double> POWER_FORECAST = new AttributeDescriptor<>("powerForecast", ValueType.NUMBER
    ).withUnits(UNITS_KILO, UNITS_WATT);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ElectricityConsumerAsset() {
    }

    public ElectricityConsumerAsset(String name) {
        super(name);
    }
}
