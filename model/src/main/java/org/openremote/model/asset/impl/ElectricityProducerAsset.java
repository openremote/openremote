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
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import jakarta.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.UNITS_KILO;
import static org.openremote.model.Constants.UNITS_WATT;

@Entity
public class ElectricityProducerAsset extends ElectricityAsset<ElectricityProducerAsset> {

    public static final AttributeDescriptor<Double> POWER_SETPOINT = ElectricityAsset.POWER_SETPOINT;
    public static final AttributeDescriptor<Double> POWER_IMPORT_MIN = ElectricityAsset.POWER_IMPORT_MIN;
    public static final AttributeDescriptor<Double> POWER_IMPORT_MAX = ElectricityAsset.POWER_IMPORT_MAX;
    public static final AttributeDescriptor<Double> POWER_EXPORT_MIN = ElectricityAsset.POWER_EXPORT_MIN;
    public static final AttributeDescriptor<Double> ENERGY_IMPORT_TOTAL = ElectricityAsset.ENERGY_IMPORT_TOTAL;
    public static final AttributeDescriptor<Integer> EFFICIENCY_IMPORT = ElectricityAsset.EFFICIENCY_IMPORT;
    public static final AttributeDescriptor<Integer> EFFICIENCY_EXPORT = ElectricityAsset.EFFICIENCY_EXPORT;
    public static final AttributeDescriptor<Double> TARIFF_IMPORT = ElectricitySupplierAsset.TARIFF_IMPORT;
    public static final AttributeDescriptor<Double> TARIFF_EXPORT = ElectricitySupplierAsset.TARIFF_EXPORT;
    public static final AttributeDescriptor<Double> CARBON_IMPORT = ElectricitySupplierAsset.CARBON_IMPORT;

    public static final AttributeDescriptor<Double> POWER_FORECAST = new AttributeDescriptor<>("powerForecast", ValueType.NUMBER
    ).withUnits(UNITS_KILO, UNITS_WATT);

    public static final AssetDescriptor<ElectricityProducerAsset> DESCRIPTOR = new AssetDescriptor<>("flash", "EABB4D", ElectricityProducerAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    ElectricityProducerAsset() {
    }

    public ElectricityProducerAsset(String name) {
        super(name);
    }

    public Optional<Double> getPowerForecast() {
        return getAttributes().getOrCreate(POWER_FORECAST).getValue();
    }
}
