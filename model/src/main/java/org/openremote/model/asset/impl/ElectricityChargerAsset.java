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

@SuppressWarnings("unchecked")
@Entity
public class ElectricityChargerAsset extends Asset<ElectricityChargerAsset> {

    public enum ConnectorType {
        YAZAKI,
        MENNEKES,
        LE_GRAND,
        CHADEMO,
        COMBO,
        SCHUKO
    }

    public static final ValueDescriptor<ConnectorType> CONNECTOR_TYPE_VALUE = new ValueDescriptor<>("chargerType", ConnectorType.class);

    public static final AttributeDescriptor<String> STATUS = new AttributeDescriptor<>("status", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY)
    );
    public static final AttributeDescriptor<ConnectorType> CONNECTOR_TYPE = new AttributeDescriptor<>("connectorType", CONNECTOR_TYPE_VALUE);
    public static final AttributeDescriptor<Double> POWER_CAPACITY_IMPORT = new AttributeDescriptor<>("powerCapacityImport", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT).withFormat(ValueFormat.NUMBER_0_DP());
    public static final AttributeDescriptor<Double> POWER_CAPACITY_EXPORT = new AttributeDescriptor<>("powerCapacityExport", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_WATT).withFormat(ValueFormat.NUMBER_0_DP());
    public static final AttributeDescriptor<Double> POWER_TOTAL = new AttributeDescriptor<>("power", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT).withFormat(ValueFormat.NUMBER_1_DP());
    public static final AttributeDescriptor<Double> POWER_SETPOINT = new AttributeDescriptor<>("powerSetpoint", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withUnits(UNITS_KILO, UNITS_WATT).withFormat(ValueFormat.NUMBER_1_DP());


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

    public Optional<String> getStatus() {
        return getAttributes().getValue(STATUS);
    }

    
    public <T extends ElectricityChargerAsset> T setStatus(String value) {
        getAttributes().getOrCreate(STATUS).setValue(value);
        return (T)this;
    }

    public Optional<ConnectorType> getConnectorType() {
        return getAttributes().getValue(CONNECTOR_TYPE);
    }

    
    public <T extends ElectricityChargerAsset> T setConnectorType(ConnectorType value) {
        getAttributes().getOrCreate(CONNECTOR_TYPE).setValue(value);
        return (T)this;
    }

    public Optional<Double> getPowerCapacityImport() {
        return getAttributes().getValue(POWER_CAPACITY_IMPORT);
    }

    
    public <T extends ElectricityChargerAsset> T setPowerCapacityImport(Double value) {
        getAttributes().getOrCreate(POWER_CAPACITY_IMPORT).setValue(value);
        return (T)this;
    }
    
    public Optional<Double> getPowerCapacityExport() {
        return getAttributes().getValue(POWER_CAPACITY_EXPORT);
    }

    public <T extends ElectricityChargerAsset> T setPowerCapacityExport(Double value) {
        getAttributes().getOrCreate(POWER_CAPACITY_EXPORT).setValue(value);
        return (T)this;
    }

    public Optional<Double> getPower() {
        return getAttributes().getValue(POWER_TOTAL);
    }

    public <T extends ElectricityChargerAsset> T setPower(Double value) {
        getAttributes().getOrCreate(POWER_TOTAL).setValue(value);
        return (T)this;
    }

    public Optional<Double> getPowerSetpoint() {
        return getAttributes().getValue(POWER_SETPOINT);
    }

    public <T extends ElectricityChargerAsset> T setPowerSetpoint(Double value) {
        getAttributes().getOrCreate(POWER_SETPOINT).setValue(value);
        return (T)this;
    }
}
