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
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.*;

import java.util.Optional;

import static org.openremote.model.Constants.*;

@SuppressWarnings("unchecked")
public abstract class ElectricityAsset<T extends ElectricityAsset<?>> extends Asset<T> {

    public static final AttributeDescriptor<Double> POWER = new AttributeDescriptor<>("power", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_SETPOINT = new AttributeDescriptor<>("powerSetpoint", ValueType.NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT).withOptional(true);
    public static final AttributeDescriptor<Double> POWER_IMPORT_MIN = new AttributeDescriptor<>("powerImportMin", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_IMPORT_MAX = new AttributeDescriptor<>("powerImportMax", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MIN = new AttributeDescriptor<>("powerExportMin", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MAX = new AttributeDescriptor<>("powerExportMax", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT);

    
    public static final AttributeDescriptor<Double> ENERGY_IMPORT_TOTAL = new AttributeDescriptor<>("energyImportTotal", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> ENERGY_EXPORT_TOTAL = new AttributeDescriptor<>("energyExportTotal", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Integer> EFFICIENCY_IMPORT = new AttributeDescriptor<>("efficiencyImport", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));
    public static final AttributeDescriptor<Integer> EFFICIENCY_EXPORT = new AttributeDescriptor<>("efficiencyExport", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_PERCENTAGE).withConstraints(new ValueConstraint.Min(0), new ValueConstraint.Max(100));

    public static final AttributeDescriptor<Double> TARIFF_IMPORT = new AttributeDescriptor<>("tariffImport", ValueType.NUMBER)
        .withUnits("EUR", UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR).withOptional(true);
    public static final AttributeDescriptor<Double> TARIFF_EXPORT = new AttributeDescriptor<>("tariffExport", ValueType.NUMBER)
        .withUnits("EUR", UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR).withOptional(true);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ElectricityAsset() {
    }

    public ElectricityAsset(String name) {
        super(name);
    }

    public Optional<Double> getPower() {
        return getAttributes().getValue(POWER);
    }

    public T setPower(Double value) {
        getAttributes().getOrCreate(POWER).setValue(value);
        return (T)this;
    }

    public Optional<Double> getPowerSetpoint() {
        return getAttributes().getValue(POWER_SETPOINT);
    }

    public T setPowerSetpoint(Double value) {
        getAttributes().getOrCreate(POWER_SETPOINT).setValue(value);
        return (T)this;
    }
    
    public Optional<Double> getPowerImportMin() {
        return getAttributes().getValue(POWER_IMPORT_MIN);
    }

    public T setPowerImportMin(Double value) {
        getAttributes().getOrCreate(POWER_IMPORT_MIN).setValue(value);
        return (T)this;
    }
    
    public Optional<Double> getPowerImportMax() {
        return getAttributes().getValue(POWER_IMPORT_MAX);
    }

    public T setPowerImportMax(Double value) {
        getAttributes().getOrCreate(POWER_IMPORT_MAX).setValue(value);
        return (T)this;
    }
    
    public Optional<Double> getPowerExportMin() {
        return getAttributes().getValue(POWER_EXPORT_MIN);
    }

    public T setPowerExportMin(Double value) {
        getAttributes().getOrCreate(POWER_EXPORT_MIN).setValue(value);
        return (T)this;
    }
    
    public Optional<Double> getPowerExportMax() {
        return getAttributes().getValue(POWER_EXPORT_MAX);
    }

    public T setPowerExportMax(Double value) {
        getAttributes().getOrCreate(POWER_EXPORT_MAX).setValue(value);
        return (T)this;
    }

    public Optional<Double> getEnergyImportTotal() {
        return getAttributes().getValue(ENERGY_IMPORT_TOTAL);
    }

    public T setEnergyImportTotal(Double value) {
        getAttributes().getOrCreate(ENERGY_IMPORT_TOTAL).setValue(value);
        return (T)this;
    }

    public Optional<Double> getEnergyExportTotal() {
        return getAttributes().getValue(ENERGY_EXPORT_TOTAL);
    }

    public T setEnergyExportTotal(Double value) {
        getAttributes().getOrCreate(ENERGY_EXPORT_TOTAL).setValue(value);
        return (T)this;
    }

    public Optional<Integer> getEfficiencyImport() {
        return getAttributes().getValue(EFFICIENCY_IMPORT);
    }

    public T setEfficiencyImport(Integer value) {
        getAttributes().getOrCreate(EFFICIENCY_IMPORT).setValue(value);
        return (T)this;
    }
    
    public Optional<Integer> getEfficiencyExport() {
        return getAttributes().getValue(EFFICIENCY_EXPORT);
    }

    public T setEfficiencyExport(Integer value) {
        getAttributes().getOrCreate(EFFICIENCY_EXPORT).setValue(value);
        return (T)this;
    }

    public Optional<Double> getTariffImport() {
        return getAttributes().getValue(TARIFF_IMPORT);
    }

    public T setTariffImport(Double value) {
        getAttributes().getOrCreate(TARIFF_IMPORT).setValue(value);
        return (T)this;
    }

    public Optional<Double> getTariffExport() {
        return getAttributes().getValue(TARIFF_EXPORT);
    }

    public T setTariffExport(Double value) {
        getAttributes().getOrCreate(TARIFF_EXPORT).setValue(value);
        return (T)this;
    }
}
