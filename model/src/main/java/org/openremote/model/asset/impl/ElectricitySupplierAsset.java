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

import static org.openremote.model.Constants.UNITS_GRAM;
import static org.openremote.model.Constants.UNITS_KILO;

@Entity
public class ElectricitySupplierAsset extends ElectricityAsset<ElectricitySupplierAsset> {

    public static final AttributeDescriptor<Double> ENERGY_IMPORT_COST = new AttributeDescriptor<>("energyImportCost", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY, true)
    ).withUnits("EUR");
    public static final AttributeDescriptor<Double> ENERGY_EXPORT_INCOME = new AttributeDescriptor<>("energyExportIncome", ValueType.POSITIVE_NUMBER,
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

    public Optional<Double> getEnergyImportCost() {
        return getAttributes().getValue(ENERGY_IMPORT_COST);
    }

    public Optional<Double> getEnergyExportIncome() {
        return getAttributes().getValue(ENERGY_EXPORT_INCOME);
    }

    public Optional<Double> getFinancialWallet() {
        return getAttributes().getValue(FINANCIAL_WALLET);
    }

    public Optional<Integer> getCarbonWallet() {
        return getAttributes().getValue(CARBON_WALLET);
    }
}
