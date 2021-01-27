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
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class ElectricityStorageAsset extends Asset<ElectricityStorageAsset> {

    public static final AttributeDescriptor<String> STATUS = new AttributeDescriptor<>("status", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<Double> LEVELISED_COST_OF_STORAGE = new AttributeDescriptor<>("levelisedCostOfStorage", ValueType.POSITIVE_NUMBER)
        .withUnits("EUR", UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> ENERGY_CAPACITY = new AttributeDescriptor<>("energyCapacity", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE = new AttributeDescriptor<>("energyLevelPercentage", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_MAX_PERCENTAGE = new AttributeDescriptor<>("energyLevelMaxPercentage", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_MIN_PERCENTAGE = new AttributeDescriptor<>("energyLevelMinPercentage", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Double> ENERGY_LEVEL = new AttributeDescriptor<>("energyLevel", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> ENERGY_TOTAL_IMPORT = new AttributeDescriptor<>("energyTotalImport", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> ENERGY_TOTAL_EXPORT = new AttributeDescriptor<>("energyTotalExport", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> POWER_CAPACITY_IMPORT = new AttributeDescriptor<>("powerCapacityImport", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_CAPACITY_EXPORT = new AttributeDescriptor<>("powerCapacityExport", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_TOTAL = new AttributeDescriptor<>("power", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_SETPOINT = new AttributeDescriptor<>("powerSetpoint", ValueType.NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Integer> CHARGE_CYCLES = new AttributeDescriptor<>("chargeCycles", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );

    public static final AttributeDescriptor<Integer> CHARGE_EFFICIENCY = new AttributeDescriptor<>("chargeEfficiency", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));

    public static final AttributeDescriptor<Double> FINANCIAL_WALLET = new AttributeDescriptor<>("financialWallet", ValueType.POSITIVE_NUMBER).withUnits("EUR");
    public static final AttributeDescriptor<Integer> CARBON_WALLET = new AttributeDescriptor<>("carbonWallet", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_GRAM);

    public static final AssetDescriptor<ElectricityStorageAsset> DESCRIPTOR = new AssetDescriptor<>("battery-charging", "1B7C89", ElectricityStorageAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    ElectricityStorageAsset() {
        this(null);
    }

    public ElectricityStorageAsset(String name) {
        super(name);
    }

    public Optional<String> getStatus() {
        return getAttributes().getValue(STATUS);
    }

    public Optional<Double> getLevelisedCostOfStorage() {
        return getAttributes().getValue(LEVELISED_COST_OF_STORAGE);
    }

    public Optional<Double> getEnergyCapacity() {
        return getAttributes().getValue(ENERGY_CAPACITY);
    }

    public Optional<Integer> getEnergyLevelPercentage() {
        return getAttributes().getValue(ENERGY_LEVEL_PERCENTAGE);
    }

    public Optional<Double> getEnergyLevel() {
        return getAttributes().getValue(ENERGY_LEVEL);
    }

    public Optional<Integer> getEnergyLevelMinPercentage() {
        return getAttributes().getValue(ENERGY_LEVEL_MIN_PERCENTAGE);
    }

    public Optional<Integer> getEnergyLevelMaxPercentage() {
        return getAttributes().getValue(ENERGY_LEVEL_MAX_PERCENTAGE);
    }

    public Optional<Double> getEnergyTotalImport() {
        return getAttributes().getValue(ENERGY_TOTAL_IMPORT);
    }

    public Optional<Double> getEnergyTotalExport() {
        return getAttributes().getValue(ENERGY_TOTAL_EXPORT);
    }

    public Optional<Double> getPowerCapacityImport() {
        return getAttributes().getValue(POWER_CAPACITY_IMPORT);
    }

    public Optional<Double> getPowerCapacityExport() {
        return getAttributes().getValue(POWER_CAPACITY_EXPORT);
    }

    public Optional<Double> getPower() {
        return getAttributes().getValue(POWER_TOTAL);
    }

    public Optional<Double> getPowerSetpoint() {
        return getAttributes().getValue(POWER_SETPOINT);
    }

    public Optional<Integer> getChargeCycles() {
        return getAttributes().getValue(CHARGE_CYCLES);
    }

    public Optional<Double> getFinancialWallet() {
        return getAttributes().getValue(FINANCIAL_WALLET);
    }

    public Optional<Integer> getCarbonWallet() {
        return getAttributes().getValue(CARBON_WALLET);
    }

    public Optional<Integer> getChargeEfficiency() {
        return getAttributes().getValue(CHARGE_EFFICIENCY);
    }

    @SuppressWarnings("unchecked")
    public <T extends ElectricityStorageAsset> T setEnergyLevelPercentage(Integer value) {
        getAttributes().getOrCreate(ENERGY_LEVEL_PERCENTAGE).setValue(value);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public <T extends ElectricityStorageAsset> T setEnergyLevel(Double value) {
        getAttributes().getOrCreate(ENERGY_LEVEL).setValue(value);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public <T extends ElectricityStorageAsset> T setEnergyTotalImport(Double value) {
        getAttributes().getOrCreate(ENERGY_TOTAL_IMPORT).setValue(value);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public <T extends ElectricityStorageAsset> T setEnergyTotalExport(Double value) {
        getAttributes().getOrCreate(ENERGY_TOTAL_EXPORT).setValue(value);
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    public <T extends ElectricityStorageAsset> T setPowerTotal(Double value) {
        getAttributes().getOrCreate(POWER_TOTAL).setValue(value);
        return (T)this;
    }
}
