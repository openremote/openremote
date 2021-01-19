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
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class ElectricitySupplierAsset extends Asset<ElectricitySupplierAsset> {

    public static final AttributeDescriptor<String> STATUS = new AttributeDescriptor<>("status", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> POWER_CAPACITY = new AttributeDescriptor<>("powerCapacity", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_EDR_SETPOINT = new AttributeDescriptor<>("powerEDRSetpoint", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_EDR_RESERVE = new AttributeDescriptor<>("powerEDRReserve", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Integer> POWER_EDR_MIN_PERIOD = new AttributeDescriptor<>("powerEDRMinPeriod", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_SECOND);
    public static final AttributeDescriptor<Double> TARIFF_IMPORT = new AttributeDescriptor<>("energyTariffImport", ValueType.POSITIVE_NUMBER)
        .withUnits("EUR", UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> TARIFF_EXPORT = new AttributeDescriptor<>("energyTariffExport", ValueType.POSITIVE_NUMBER)
        .withUnits("EUR", UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> ENERGY_TARIFF_IMPORT_FORECAST_DEVIATION = new AttributeDescriptor<>("energyTariffImportForecastDeviation", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> ENERGY_TARIFF_EXPORT_FORECAST_DEVIATION = new AttributeDescriptor<>("energyTariffExportForecastDeviation", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> ENERGY_TAX = new AttributeDescriptor<>("energyTax", ValueType.POSITIVE_NUMBER)
        .withUnits("EUR", UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> GRID_COST = new AttributeDescriptor<>("gridCost", ValueType.POSITIVE_NUMBER)
        .withUnits("EUR");
    public static final AttributeDescriptor<Double> GRID_CONNECTION_COST = new AttributeDescriptor<>("gridConnectionCost", ValueType.POSITIVE_NUMBER)
        .withUnits("EUR");
    public static final AttributeDescriptor<Double> CARBON_IMPORT = new AttributeDescriptor<>("carbonImport", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_GRAM, UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> CARBON_EXPORT = new AttributeDescriptor<>("carbonExport", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_GRAM, UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> POWER_TOTAL = new AttributeDescriptor<>("powerTotal", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_FORECAST_DEVIATION = new AttributeDescriptor<>("powerForecastDeviation", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> ENERGY_TOTAL_IMPORT = new AttributeDescriptor<>("energyTotalImport", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> ENERGY_TOTAL_EXPORT = new AttributeDescriptor<>("energyTotalExport", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> ENERGY_TOTAL_IMPORT_COST = new AttributeDescriptor<>("energyTotalImportCost", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits("EUR");
    public static final AttributeDescriptor<Double> ENERGY_TOTAL_EXPORT_INCOME = new AttributeDescriptor<>("energyTotalExportIncome", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits("EUR");
    public static final AttributeDescriptor<Integer> CARBON_TOTAL = new AttributeDescriptor<>("carbonTotal", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits(UNITS_KILO, UNITS_GRAM);

    public static final AssetDescriptor<ElectricitySupplierAsset> DESCRIPTOR = new AssetDescriptor<>("upload-network", "9257A9", ElectricitySupplierAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    ElectricitySupplierAsset() {
        this(null);
    }

    public ElectricitySupplierAsset(String name) {
        super(name);
    }

    public Optional<String> getStatus() {
        return getAttributes().getValue(STATUS);
    }

    public Optional<Double> getPowerCapacity() {
        return getAttributes().getValue(POWER_CAPACITY);
    }

    public Optional<Double> getPowerEDRSetpoint() {
        return getAttributes().getValue(POWER_EDR_SETPOINT);
    }

    public Optional<Double> getPowerEDRReserve() {
        return getAttributes().getValue(POWER_EDR_RESERVE);
    }

    public Optional<Integer> getPowerEDRMinPeriod() {
        return getAttributes().getValue(POWER_EDR_MIN_PERIOD);
    }

    public Optional<Double> getTariffImport() {
        return getAttributes().getValue(TARIFF_IMPORT);
    }

    public Optional<Double> getTariffExport() {
        return getAttributes().getValue(TARIFF_EXPORT);
    }

    public Optional<Double> getEnergyTariffImportForecastDeviation() {
        return getAttributes().getValue(ENERGY_TARIFF_IMPORT_FORECAST_DEVIATION);
    }

    public Optional<Double> getEnergyTariffExportForecastDeviation() {
        return getAttributes().getValue(ENERGY_TARIFF_EXPORT_FORECAST_DEVIATION);
    }

    public Optional<Double> getEnergyTax() {
        return getAttributes().getValue(ENERGY_TAX);
    }

    public Optional<Double> getGridCost() {
        return getAttributes().getValue(GRID_COST);
    }

    public Optional<Double> getGridConnectionCost() {
        return getAttributes().getValue(GRID_CONNECTION_COST);
    }

    public Optional<Double> getCarbonImport() {
        return getAttributes().getValue(CARBON_IMPORT);
    }

    public Optional<Double> getCarbonExport() {
        return getAttributes().getValue(CARBON_EXPORT);
    }

    public Optional<Double> getPowerTotal() {
        return getAttributes().getValue(POWER_TOTAL);
    }

    public Optional<Double> getPowerForecastDeviation() {
        return getAttributes().getValue(POWER_FORECAST_DEVIATION);
    }

    public Optional<Double> getEnergyTotalImport() {
        return getAttributes().getValue(ENERGY_TOTAL_IMPORT);
    }

    public Optional<Double> getEnergyTotalExport() {
        return getAttributes().getValue(ENERGY_TOTAL_EXPORT);
    }

    public Optional<Double> getEnergyTotalImportCost() {
        return getAttributes().getValue(ENERGY_TOTAL_IMPORT_COST);
    }

    public Optional<Double> getEnergyTotalExportIncome() {
        return getAttributes().getValue(ENERGY_TOTAL_EXPORT_INCOME);
    }

    public Optional<Integer> getCarbonTotal() {
        return getAttributes().getValue(CARBON_TOTAL);
    }
}
