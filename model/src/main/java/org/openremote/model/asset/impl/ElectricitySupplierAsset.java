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
import org.openremote.model.value.AbstractNameValueHolder;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;
import static org.openremote.model.value.ValueType.NUMBER;

@Entity
public class ElectricitySupplierAsset extends ElectricityAsset<ElectricitySupplierAsset> {

    public static final AttributeDescriptor<Double> FINANCIAL_COST = new AttributeDescriptor<>("financialCost", ValueType.NUMBER).withUnits("EUR");
    public static final AttributeDescriptor<Double> CARBON_COST = new AttributeDescriptor<>("carbonCost", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_GRAM);
    public static final AttributeDescriptor<Double> ENERGY_LOCAL = new AttributeDescriptor<>("energyLocal", NUMBER,
            new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> ENERGY_RENEWABLE_SHARE = new AttributeDescriptor<>("energyRenewableShare", NUMBER,
            new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_PERCENTAGE);
    public static final AttributeDescriptor<Double> ENERGY_SELF_CONSUMPTION = new AttributeDescriptor<>("energySelfConsumption", NUMBER,
            new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_PERCENTAGE);
    public static final AttributeDescriptor<Double> ENERGY_AUTARKY = new AttributeDescriptor<>("energyAutarky", NUMBER,
            new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_PERCENTAGE);

    public static final AttributeDescriptor<Double> POWER_SETPOINT = ElectricityAsset.POWER_SETPOINT.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_IMPORT_MIN = ElectricityAsset.POWER_IMPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MAX = ElectricityAsset.POWER_EXPORT_MAX.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MIN = ElectricityAsset.POWER_EXPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Integer> EFFICIENCY_IMPORT = ElectricityAsset.EFFICIENCY_IMPORT.withOptional(true);
    public static final AttributeDescriptor<Integer> EFFICIENCY_EXPORT = ElectricityAsset.EFFICIENCY_EXPORT.withOptional(true);
    public static final AttributeDescriptor<Double> TARIFF_IMPORT = ElectricityAsset.TARIFF_IMPORT.withOptional(false);
    public static final AttributeDescriptor<Double> TARIFF_EXPORT = ElectricityAsset.TARIFF_EXPORT.withOptional(false);

    public static final AssetDescriptor<ElectricitySupplierAsset> DESCRIPTOR = new AssetDescriptor<>("upload-network", "9257A9", ElectricitySupplierAsset.class);
    public static final AttributeDescriptor<Double> CARBON_IMPORT = new AttributeDescriptor<>("carbonImport", ValueType.NUMBER)
            .withUnits(UNITS_KILO, UNITS_GRAM, UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR).withOptional(false);
    public static final AttributeDescriptor<Double> CARBON_EXPORT = new AttributeDescriptor<>("carbonExport", ValueType.NUMBER)
            .withUnits(UNITS_KILO, UNITS_GRAM, UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR).withOptional(false);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ElectricitySupplierAsset() {
    }

    public ElectricitySupplierAsset(String name) {
        super(name);
    }

    public Optional<Double> getFinancialCost() {
        return getAttributes().getValue(FINANCIAL_COST);
    }

    public ElectricitySupplierAsset setFinancialCost(Double value) {
        getAttributes().getOrCreate(FINANCIAL_COST).setValue(value);
        return this;
    }

    public Optional<Double> getCarbonCost() {
        return getAttributes().getValue(CARBON_COST);
    }

    public ElectricitySupplierAsset setCarbonCost(Double value) {
        getAttributes().getOrCreate(CARBON_COST).setValue(value);
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

    public Optional<Double> getEnergyLocal() {
        return getAttribute(ENERGY_LOCAL).flatMap(AbstractNameValueHolder::getValue);
    }

    public ElectricitySupplierAsset setEnergyLocal(Double value) {
        getAttributes().getOrCreate(ENERGY_LOCAL).setValue(value);
        return this;
    }

    public Optional<Double> getEnergyRenewableShare() {
        return getAttribute(ENERGY_RENEWABLE_SHARE).flatMap(AbstractNameValueHolder::getValue);
    }

    public ElectricitySupplierAsset setEnergyRenewableShare(Double value) {
        getAttributes().getOrCreate(ENERGY_RENEWABLE_SHARE).setValue(value);
        return this;
    }

    public Optional<Double> getEnergySelfConsumption() {
        return getAttribute(ENERGY_SELF_CONSUMPTION).flatMap(AbstractNameValueHolder::getValue);
    }

    public ElectricitySupplierAsset setEnergySelfConsumption(Double value) {
        getAttributes().getOrCreate(ENERGY_SELF_CONSUMPTION).setValue(value);
        return this;
    }

    public Optional<Double> getEnergyAutarky() {
        return getAttribute(ENERGY_AUTARKY).flatMap(AbstractNameValueHolder::getValue);
    }

    public ElectricitySupplierAsset setEnergyAutarky(Double value) {
        getAttributes().getOrCreate(ENERGY_AUTARKY).setValue(value);
        return this;
    }
}
