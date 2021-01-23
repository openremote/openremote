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
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Collection;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class ElectricVehicleAsset extends ElectricityStorageAsset {

    public static final AttributeDescriptor<ElectricityChargerAsset.ConnectorType> CONNECTOR_TYPE = new AttributeDescriptor<>("connectorType", ElectricityChargerAsset.CONNECTOR_TYPE_VALUE);
    public static final AttributeDescriptor<Double> EFFICIENCY = new AttributeDescriptor<>("efficiency", ValueType.POSITIVE_NUMBER)
        .withUnits(UNITS_KILO, UNITS_METRE, UNITS_PER, UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Integer> ODOMETER = new AttributeDescriptor<>("odometer", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_KILO, UNITS_METRE);
    public static final AttributeDescriptor<Boolean> CHARGER_CONNECTED = new AttributeDescriptor<>("chargerConnected", ValueType.BOOLEAN);

    public static final AssetDescriptor<ElectricVehicleAsset> DESCRIPTOR = new AssetDescriptor<>("car-electric", "49B0D8", ElectricVehicleAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    ElectricVehicleAsset() {
        this(null);
    }

    public ElectricVehicleAsset(String name) {
        super(name);
    }

    @Override
    public ElectricVehicleAsset setId(String id) {
        super.setId(id);
        return this;
    }

    @Override
    public ElectricVehicleAsset setName(String name) throws IllegalArgumentException {
        super.setName(name);
        return this;
    }

    @Override
    public ElectricVehicleAsset setAccessPublicRead(boolean accessPublicRead) {
        super.setAccessPublicRead(accessPublicRead);
        return this;
    }

    @Override
    public ElectricVehicleAsset setParent(Asset<?> parent) {
        super.setParent(parent);
        return this;
    }

    @Override
    public ElectricVehicleAsset setParentId(String parentId) {
        super.setParentId(parentId);
        return this;
    }

    @Override
    public ElectricVehicleAsset setRealm(String realm) {
        super.setRealm(realm);
        return this;
    }

    @Override
    protected ElectricityStorageAsset setPath(String[] path) {
        super.setPath(path);
        return this;
    }

    @Override
    public ElectricVehicleAsset setAttributes(AttributeMap attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public Asset<?> setAttributes(Attribute<?>... attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricVehicleAsset setAttributes(Collection<Attribute<?>> attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricVehicleAsset setLocation(GeoJSONPoint location) {
        super.setLocation(location);
        return this;
    }

    @Override
    public ElectricVehicleAsset setEmail(String email) {
        super.setEmail(email);
        return this;
    }

    @Override
    public ElectricVehicleAsset setNotes(String notes) {
        super.setNotes(notes);
        return this;
    }

    @Override
    public ElectricVehicleAsset setManufacturer(String manufacturer) {
        super.setManufacturer(manufacturer);
        return this;
    }

    @Override
    public ElectricVehicleAsset setModel(String model) {
        super.setModel(model);
        return this;
    }

    @Override
    public ElectricVehicleAsset addAttributes(Attribute<?>... attributes) {
        super.addAttributes(attributes);
        return this;
    }

    @Override
    public ElectricVehicleAsset addOrReplaceAttributes(Attribute<?>... attributes) {
        super.addOrReplaceAttributes(attributes);
        return this;
    }

    @Override
    public ElectricVehicleAsset setTags(String[] tags) {
        super.setTags(tags);
        return this;
    }

    public Optional<ElectricityChargerAsset.ConnectorType> getConnectorType() {
        return getAttributes().getValue(CONNECTOR_TYPE);
    }

    public ElectricVehicleAsset setConnectorType(ElectricityChargerAsset.ConnectorType value) {
        getAttributes().getOrCreate(CONNECTOR_TYPE).setValue(value);
        return this;
    }

    public Optional<Double> getEfficiency() {
        return getAttributes().getValue(EFFICIENCY);
    }

    public ElectricVehicleAsset setEfficiency(Double value) {
        getAttributes().getOrCreate(EFFICIENCY).setValue(value);
        return this;
    }

    public Optional<Integer> getOdometer() {
        return getAttributes().getValue(ODOMETER);
    }

    public ElectricVehicleAsset setOdometer(Integer value) {
        getAttributes().getOrCreate(ODOMETER).setValue(value);
        return this;
    }

    public Optional<Boolean> getChargerConnected() {
        return getAttributes().getValue(CHARGER_CONNECTED);
    }

    public ElectricVehicleAsset setChargerConnected(Boolean value) {
        getAttributes().getOrCreate(CHARGER_CONNECTED).setValue(value);
        return this;
    }
}
