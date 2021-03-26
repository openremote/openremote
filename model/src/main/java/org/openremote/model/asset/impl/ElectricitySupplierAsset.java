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
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class ElectricitySupplierAsset extends ElectricityAsset<ElectricitySupplierAsset> {

    public static final AttributeDescriptor<Double> ENERGY_IMPORT_COST = new AttributeDescriptor<>("energyImportCost", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits("EUR");
    public static final AttributeDescriptor<Double> ENERGY_EXPORT_COST = new AttributeDescriptor<>("energyExportCost", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits("EUR");
    public static final AttributeDescriptor<Double> FINANCIAL_WALLET = new AttributeDescriptor<>("financialWallet", ValueType.NUMBER).withUnits("EUR");
    public static final AttributeDescriptor<Integer> CARBON_WALLET = new AttributeDescriptor<>("carbonWallet", ValueType.INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_GRAM);

    public static final AttributeDescriptor<Double> POWER_SETPOINT = ElectricityAsset.POWER_SETPOINT.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_IMPORT_MIN = ElectricityAsset.POWER_IMPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MAX = ElectricityAsset.POWER_EXPORT_MAX.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MIN = ElectricityAsset.POWER_EXPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Integer> EFFICIENCY_IMPORT = ElectricityAsset.EFFICIENCY_IMPORT.withOptional(true);
    public static final AttributeDescriptor<Integer> EFFICIENCY_EXPORT = ElectricityAsset.EFFICIENCY_EXPORT.withOptional(true);
    public static final AttributeDescriptor<Double> TARIFF_IMPORT = ElectricityAsset.TARIFF_IMPORT.withOptional(false);

    public static final AssetDescriptor<ElectricitySupplierAsset> DESCRIPTOR = new AssetDescriptor<>("upload-network", "9257A9", ElectricitySupplierAsset.class);
    public static final AttributeDescriptor<Double> CARBON_IMPORT = new AttributeDescriptor<>("carbonImport", ValueType.NUMBER)
        .withUnits(UNITS_KILO, UNITS_GRAM, UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR).withOptional(true);
    public static final AttributeDescriptor<Double> CARBON_EXPORT = new AttributeDescriptor<>("carbonExport", ValueType.NUMBER)
        .withUnits(UNITS_KILO, UNITS_GRAM, UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR).withOptional(true);
    public static final AttributeDescriptor<Integer> CARBON_IMPORT_TOTAL = new AttributeDescriptor<>("carbonImportTotal", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits(UNITS_KILO, UNITS_GRAM);
    public static final AttributeDescriptor<Integer> CARBON_EXPORT_TOTAL = new AttributeDescriptor<>("carbonExportTotal", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits(UNITS_KILO, UNITS_GRAM);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ElectricitySupplierAsset() {
    }

    public ElectricitySupplierAsset(String name) {
        super(name);
    }

    public Optional<Double> getEnergyImportCost() {
        return getAttributes().getValue(ENERGY_IMPORT_COST);
    }

    public ElectricitySupplierAsset setEnergyImportCost(Double value) {
        getAttributes().getOrCreate(ENERGY_IMPORT_COST).setValue(value);
        return this;
    }

    public Optional<Double> getEnergyExportCost() {
        return getAttributes().getValue(ENERGY_EXPORT_COST);
    }

    public ElectricitySupplierAsset setEnergyExportCost(Double value) {
        getAttributes().getOrCreate(ENERGY_EXPORT_COST).setValue(value);
        return this;
    }

    public Optional<Double> getFinancialWallet() {
        return getAttributes().getValue(FINANCIAL_WALLET);
    }

    public ElectricitySupplierAsset setFinancialWallet(Double value) {
        getAttributes().getOrCreate(FINANCIAL_WALLET).setValue(value);
        return this;
    }

    public Optional<Integer> getCarbonWallet() {
        return getAttributes().getValue(CARBON_WALLET);
    }

    public ElectricitySupplierAsset setCarbonWallet(Integer value) {
        getAttributes().getOrCreate(CARBON_WALLET).setValue(value);
        return this;
    }

    public Optional<Double> getCarbonImport() {
        return getAttributes().getValue(CARBON_IMPORT);
    }

    public ElectricitySupplierAsset setCarbonImport(Double value) {
        getAttributes().getOrCreate(CARBON_IMPORT).setValue(value);
        return this;
    }

    public Optional<Double> getCarbonExport() {
        return getAttributes().getValue(CARBON_EXPORT);
    }

    public ElectricitySupplierAsset setCarbonExport(Double value) {
        getAttributes().getOrCreate(CARBON_EXPORT).setValue(value);
        return this;
    }

    public Optional<Integer> getCarbonImportTotal() {
        return getAttributes().getValue(CARBON_IMPORT_TOTAL);
    }

    public ElectricitySupplierAsset setCarbonImportTotal(Integer value) {
        getAttributes().getOrCreate(CARBON_IMPORT_TOTAL).setValue(value);
        return this;
    }

    public Optional<Integer> getCarbonExportTotal() {
        return getAttributes().getValue(CARBON_EXPORT_TOTAL);
    }

    public ElectricitySupplierAsset setCarbonExportTotal(Integer value) {
        getAttributes().getOrCreate(CARBON_EXPORT_TOTAL).setValue(value);
        return this;
    }
}
