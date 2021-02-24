/*
 * Copyright 2021, OpenRemote Inc.
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
public class ElectricVehicleFleetGroupAsset extends GroupAsset {

    public static final AttributeDescriptor<String> FLEET_CATEGORY = new AttributeDescriptor<>("fleetCategory", ValueType.TEXT);
    public static final AttributeDescriptor<Integer> AVAILABLE_CHARGING_SPACES = new AttributeDescriptor<>("availableChargingSpaces", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> AVAILABLE_DISCHARGING_SPACES = new AttributeDescriptor<>("availableDischargingSpaces", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> POWER_IMPORT_MAX = new AttributeDescriptor<>("powerImportMax", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> POWER_EXPORT_MAX = new AttributeDescriptor<>("powerExportMax", ValueType.POSITIVE_INTEGER);

    public static final AttributeDescriptor<Integer> MILEAGE_MINIMUM = new AttributeDescriptor<>("mileageMinimum", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_KILO, UNITS_METRE);

    public static final AssetDescriptor<ElectricVehicleFleetGroupAsset> DESCRIPTOR = new AssetDescriptor<>("car-multiple", "49B0D8", ElectricVehicleFleetGroupAsset.class);

    protected ElectricVehicleFleetGroupAsset() {
    }

    public ElectricVehicleFleetGroupAsset(String name) {
        super(name, ElectricVehicleAsset.class);
    }

    public Optional<String> getFleetCategory() {
        return getAttributes().getValue(FLEET_CATEGORY);
    }
    
    public Optional<Integer> getAvailableChargingSpaces() {
        return getAttributes().getValue(AVAILABLE_CHARGING_SPACES);
    }
    
    public Optional<Integer> getAvailableDischargingSpaces() {
        return getAttributes().getValue(AVAILABLE_DISCHARGING_SPACES);
    }
    
    public Optional<Integer> getPowerImportMax() {
        return getAttributes().getValue(POWER_IMPORT_MAX);
    }
    
    public Optional<Integer> getPowerExportMax() {
        return getAttributes().getValue(POWER_EXPORT_MAX);
    }

    public Optional<Integer> getMileageMinimum() { return getAttributes().getValue(MILEAGE_MINIMUM); }

    @Override
    public ElectricVehicleFleetGroupAsset setId(String id) {
        super.setId(id);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setVersion(long version) {
        super.setVersion(version);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setName(String name) throws IllegalArgumentException {
        super.setName(name);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setAccessPublicRead(boolean accessPublicRead) {
        super.setAccessPublicRead(accessPublicRead);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setParent(Asset<?> parent) {
        super.setParent(parent);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setParentId(String parentId) {
        super.setParentId(parentId);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setRealm(String realm) {
        super.setRealm(realm);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setAttributes(AttributeMap attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public Asset<?> setAttributes(Attribute<?>... attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setAttributes(Collection<Attribute<?>> attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset addAttributes(Attribute<?>... attributes) {
        super.addAttributes(attributes);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset addOrReplaceAttributes(Attribute<?>... attributes) {
        super.addOrReplaceAttributes(attributes);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setLocation(GeoJSONPoint location) {
        super.setLocation(location);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setTags(String[] tags) {
        super.setTags(tags);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setEmail(String email) {
        super.setEmail(email);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setNotes(String notes) {
        super.setNotes(notes);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setManufacturer(String manufacturer) {
        super.setManufacturer(manufacturer);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setModel(String model) {
        super.setModel(model);
        return this;
    }

    @Override
    public ElectricVehicleFleetGroupAsset setChildAssetType(String childAssetType) {
        super.setChildAssetType(childAssetType);
        return this;
    }
}
