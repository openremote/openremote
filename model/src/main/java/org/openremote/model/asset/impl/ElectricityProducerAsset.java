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

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class ElectricityProducerAsset extends Asset<ElectricityProducerAsset> {

    public static final AttributeDescriptor<String> STATUS = new AttributeDescriptor<>("status", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> POWER_CAPACITY = new AttributeDescriptor<>("powerCapacity", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Integer> EFFICIENCY = new AttributeDescriptor<>("efficiency", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Double> POWER_TOTAL = new AttributeDescriptor<>("powerTotal", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_FORECAST_DEVIATION = new AttributeDescriptor<>("powerForecastDeviation", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> ENERGY_TOTAL = new AttributeDescriptor<>("energyTotal", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

    public static final AssetDescriptor<ElectricityProducerAsset> DESCRIPTOR = new AssetDescriptor<>("flash", "EABB4D", ElectricityProducerAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    ElectricityProducerAsset() {
        this(null);
    }

    public ElectricityProducerAsset(String name) {
        super(name);
    }

    public Optional<String> getStatus() {
        return getAttributes().getValue(STATUS);
    }

    public ElectricityProducerAsset setStatus(String value) {
        getAttributes().getOrCreate(STATUS).setValue(value);
        return this;
    }

    public Optional<Double> getPowerTotal() {
        return getAttributes().getValue(POWER_TOTAL);
    }

    public ElectricityProducerAsset setPowerTotal(Double value) {
        getAttributes().getOrCreate(POWER_TOTAL).setValue(value);
        return this;
    }

    public Optional<Double> getPowerForecastDeviation() {
        return getAttributes().getValue(POWER_FORECAST_DEVIATION);
    }

    public ElectricityProducerAsset setPowerForecastDeviation(Double value) {
        getAttributes().getOrCreate(POWER_FORECAST_DEVIATION).setValue(value);
        return this;
    }

    public Optional<Double> getPowerCapacity() {
        return getAttributes().getValue(POWER_CAPACITY);
    }

    public ElectricityProducerAsset setPowerCapacity(Double value) {
        getAttributes().getOrCreate(POWER_CAPACITY).setValue(value);
        return this;
    }

    public Optional<Integer> getEfficiency() {
        return getAttributes().getValue(EFFICIENCY);
    }

    public ElectricityProducerAsset setEfficiency(Integer value) {
        getAttributes().getOrCreate(EFFICIENCY).setValue(value);
        return this;
    }

    public Optional<Double> getEnergyTotal() {
        return getAttributes().getValue(ENERGY_TOTAL);
    }

    public ElectricityProducerAsset setEnergyTotal(Double value) {
        getAttributes().getOrCreate(ENERGY_TOTAL).setValue(value);
        return this;
    }
}
