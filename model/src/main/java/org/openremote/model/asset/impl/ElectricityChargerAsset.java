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
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class ElectricityChargerAsset extends ElectricityStorageAsset {

    public enum ConnectorType {
        YAZAKI,
        MENNEKES,
        LE_GRAND,
        CHADEMO,
        COMBO,
        SCHUKO
    }

    public static final ValueDescriptor<ConnectorType> CONNECTOR_TYPE_VALUE = new ValueDescriptor<>("connectorType", ConnectorType.class);

    public static final AttributeDescriptor<ConnectorType> CONNECTOR_TYPE = new AttributeDescriptor<>("connectorType", CONNECTOR_TYPE_VALUE);
    public static final AttributeDescriptor<Boolean> VEHICLE_CONNECTED = new AttributeDescriptor<>("vehicleConnected", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VEHICLE_ID = new AttributeDescriptor<>("vehicleID", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY, true)).withOptional(true);

    public static final AttributeDescriptor<Double> POWER_IMPORT_MIN = ElectricityAsset.POWER_IMPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Double> POWER_EXPORT_MIN = ElectricityAsset.POWER_EXPORT_MIN.withOptional(true);
    public static final AttributeDescriptor<Double> ENERGY_EXPORT_TOTAL = ElectricityAsset.ENERGY_EXPORT_TOTAL.withOptional(true);
    public static final AttributeDescriptor<Double> ENERGY_LEVEL = ElectricityStorageAsset.ENERGY_LEVEL.withOptional(true);
    public static final AttributeDescriptor<Double> ENERGY_CAPACITY = ElectricityStorageAsset.ENERGY_CAPACITY.withOptional(true);
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE = ElectricityStorageAsset.ENERGY_LEVEL_PERCENTAGE.withOptional(true);
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE_MAX = ElectricityStorageAsset.ENERGY_LEVEL_PERCENTAGE_MAX.withOptional(true);
    public static final AttributeDescriptor<Integer> ENERGY_LEVEL_PERCENTAGE_MIN = ElectricityStorageAsset.ENERGY_LEVEL_PERCENTAGE_MIN.withOptional(true);
    public static final AttributeDescriptor<Integer> EFFICIENCY_IMPORT = ElectricityAsset.EFFICIENCY_IMPORT.withOptional(true);
    public static final AttributeDescriptor<Integer> EFFICIENCY_EXPORT = ElectricityAsset.EFFICIENCY_EXPORT.withOptional(true);
    public static final AttributeDescriptor<Double> TARIFF_IMPORT = ElectricityAsset.TARIFF_IMPORT.withOptional(true);
    public static final AttributeDescriptor<Double> TARIFF_EXPORT = ElectricityAsset.TARIFF_EXPORT.withOptional(true);
    public static final AttributeDescriptor<Double> CARBON_IMPORT = ElectricityAsset.CARBON_IMPORT.withOptional(true);
    public static final AttributeDescriptor<Double> CARBON_EXPORT = ElectricityAsset.CARBON_EXPORT.withOptional(true);
    public static final AttributeDescriptor<Integer> CARBON_IMPORT_TOTAL = ElectricityAsset.CARBON_IMPORT_TOTAL.withOptional(true);
    public static final AttributeDescriptor<Integer> CARBON_EXPORT_TOTAL = ElectricityAsset.CARBON_EXPORT_TOTAL.withOptional(true);

    public static final AssetDescriptor<ElectricityChargerAsset> DESCRIPTOR = new AssetDescriptor<>("ev-station", "8A293D", ElectricityChargerAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    ElectricityChargerAsset() {
        this(null);
    }

    public ElectricityChargerAsset(String name) {
        super(name);
    }

    public Optional<ConnectorType> getConnectorType() {
        return getAttributes().getValue(CONNECTOR_TYPE);
    }

    public ElectricityChargerAsset setConnectorType(ConnectorType value) {
        getAttributes().getOrCreate(CONNECTOR_TYPE).setValue(value);
        return this;
    }

    public Optional<Boolean> getVehicleConnected() {
        return getAttributes().getValue(VEHICLE_CONNECTED);
    }

    public Optional<String> getVehicleID() {
        return getAttributes().getValue(VEHICLE_ID);
    }
}
