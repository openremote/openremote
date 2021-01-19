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

import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class ElectricityConsumerAsset extends Asset<ElectricityConsumerAsset> {

    public static final AttributeDescriptor<String> STATUS = new AttributeDescriptor<>("status", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<String> DEMAND_RESPONSE_TYPE = new AttributeDescriptor<>("demandResponseType", ValueType.TEXT);
    public static final AttributeDescriptor<Double> TARIFF_IMPORT = new AttributeDescriptor<>("tariffImport", ValueType.NUMBER)
        .withUnits("EUR", UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> TARIFF_EXPORT = new AttributeDescriptor<>("tariffExport", ValueType.NUMBER)
        .withUnits("EUR", UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Integer> CARBON_IMPORT = new AttributeDescriptor<>("carbonImport", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_KILO, UNITS_GRAM, UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Integer> CARBON_EXPORT = new AttributeDescriptor<>("carbonExport", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_KILO, UNITS_GRAM, UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> POWER_TOTAL = new AttributeDescriptor<>("powerTotal", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_FORECAST_DEVIATION = new AttributeDescriptor<>("powerForecastDeviation", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_SETPOINT = new AttributeDescriptor<>("powerSetpoint", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_AVAILABLE_MAX = new AttributeDescriptor<>("powerAvailableMax", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_AVAILABLE_MIN = new AttributeDescriptor<>("powerAvailableMin", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> ENERGY_TOTAL = new AttributeDescriptor<>("energyTotal", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);

    public static final AssetDescriptor<ElectricityConsumerAsset> DESCRIPTOR = new AssetDescriptor<>("power-plug", "8A293D", ElectricityConsumerAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    ElectricityConsumerAsset() {
        this(null);
    }

    public ElectricityConsumerAsset(String name) {
        super(name);
    }

    public Optional<String> getStatus() {
        return getAttributes().getValue(STATUS);
    }

    public Optional<String> getDemandResponseType() {
        return getAttributes().getValue(DEMAND_RESPONSE_TYPE);
    }

    public Optional<Double> getTariffImport() {
        return getAttributes().getValue(TARIFF_IMPORT);
    }

    public Optional<Double> getTariffExport() {
        return getAttributes().getValue(TARIFF_EXPORT);
    }

    public Optional<Integer> getCarbonImport() {
        return getAttributes().getValue(CARBON_IMPORT);
    }

    public Optional<Integer> getCarbonExport() {
        return getAttributes().getValue(CARBON_EXPORT);
    }

    public Optional<Double> getPowerTotal() {
        return getAttributes().getValue(POWER_TOTAL);
    }

    public Optional<Double> getPowerForecastDeviation() {
        return getAttributes().getValue(POWER_FORECAST_DEVIATION);
    }

    public Optional<Double> getPowerSetpoint() {
        return getAttributes().getValue(POWER_SETPOINT);
    }

    public Optional<Double> getPowerAvailableMax() {
        return getAttributes().getValue(POWER_AVAILABLE_MAX);
    }

    public Optional<Double> getPowerAvailableMin() {
        return getAttributes().getValue(POWER_AVAILABLE_MIN);
    }

    public Optional<Double> getEnergyTotal() {
        return getAttributes().getValue(ENERGY_TOTAL);
    }
}
