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

import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueType;

import java.util.Optional;

import static org.openremote.model.Constants.*;

public abstract class ElectricityStorageAsset extends ElectricityAsset<ElectricityStorageAsset> {


    public static final AttributeDescriptor<Boolean> SUPPORTS_EXPORT = new AttributeDescriptor<>("supportsExport", ValueType.BOOLEAN);
    public static final AttributeDescriptor<Boolean> SUPPORTS_IMPORT = new AttributeDescriptor<>("supportsImport", ValueType.BOOLEAN);
    public static final AttributeDescriptor<Double> ENERGY_LEVEL = new AttributeDescriptor<>("energyLevel", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> ENERGY_CAPACITY = new AttributeDescriptor<>("energyCapacity", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE = new AttributeDescriptor<>("energyLevelPercentage", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE_MAX = new AttributeDescriptor<>("energyLevelPercentageMax", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE_MIN = new AttributeDescriptor<>("energyLevelPercentageMin", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Integer[][]> ENERGY_LEVEL_SCHEDULE = new AttributeDescriptor<>("energyLevelSchedule", ValueType.POSITIVE_INTEGER.asArray().asArray())
        .withOptional(true);
    public static final AttributeDescriptor<AttributeExecuteStatus> FORCE_CHARGE = new AttributeDescriptor<>("forceCharge", ValueType.EXECUTION_STATUS);

    public static final AttributeDescriptor<Double> POWER_SETPOINT = ElectricityAsset.POWER_SETPOINT.withOptional(false);
    public static final AttributeDescriptor<Double> POWER_IMPORT_MIN = ElectricityAsset.POWER_IMPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MIN = ElectricityAsset.POWER_EXPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Double> CARBON_IMPORT = ElectricitySupplierAsset.CARBON_IMPORT.withOptional(true);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ElectricityStorageAsset() {
    }

    public ElectricityStorageAsset(String name) {
        super(name);
    }

    public Optional<Boolean> isSupportsExport() {
        return getAttributes().getValue(SUPPORTS_EXPORT);
    }

    public ElectricityStorageAsset setSupportsExport(Boolean value) {
        getAttributes().getOrCreate(SUPPORTS_EXPORT).setValue(value);
        return this;
    }

    public Optional<Boolean> isSupportsImport() {
        return getAttributes().getValue(SUPPORTS_IMPORT);
    }

    public ElectricityStorageAsset setSupportsImport(Boolean value) {
        getAttributes().getOrCreate(SUPPORTS_IMPORT).setValue(value);
        return this;
    }

    public Optional<Double> getEnergyCapacity() {
        return getAttributes().getValue(ENERGY_CAPACITY);
    }

    public ElectricityStorageAsset setEnergyCapacity(Double value) {
        getAttributes().getOrCreate(ENERGY_CAPACITY).setValue(value);
        return this;
    }

    public Optional<Double> getEnergyLevel() {
        return getAttributes().getValue(ENERGY_LEVEL);
    }

    public ElectricityStorageAsset setEnergyLevel(Double value) {
        getAttributes().getOrCreate(ENERGY_LEVEL).setValue(value);
        return this;
    }

    public Optional<Integer> getEnergyLevelPercentage() {
        return getAttributes().getValue(ENERGY_LEVEL_PERCENTAGE);
    }

    public ElectricityStorageAsset setEnergyLevelPercentage(Integer value) {
        getAttributes().getOrCreate(ENERGY_LEVEL_PERCENTAGE).setValue(value);
        return this;
    }

    public Optional<Integer> getEnergyLevelPercentageMin() {
        return getAttributes().getValue(ENERGY_LEVEL_PERCENTAGE_MIN);
    }

    public ElectricityStorageAsset setEnergyLevelPercentageMin(Integer value) {
        getAttributes().getOrCreate(ENERGY_LEVEL_PERCENTAGE_MIN).setValue(value);
        return this;
    }

    public Optional<Integer> getEnergyLevelPercentageMax() {
        return getAttributes().getValue(ENERGY_LEVEL_PERCENTAGE_MAX);
    }

    public ElectricityStorageAsset setEnergyLevelPercentageMax(Integer value) {
        getAttributes().getOrCreate(ENERGY_LEVEL_PERCENTAGE_MAX).setValue(value);
        return this;
    }

    public Optional<Integer[][]> getEnergyLevelSchedule() {
        return getAttributes().getValue(ENERGY_LEVEL_SCHEDULE);
    }

    public ElectricityStorageAsset setEnergyLevelSchedule(Integer[][] value) {
        getAttributes().getOrCreate(ENERGY_LEVEL_SCHEDULE).setValue(value);
        return this;
    }
}
