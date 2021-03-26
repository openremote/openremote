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
import java.util.Date;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class BuildingAsset extends CityAsset {

    public static final AttributeDescriptor<String> STREET = new AttributeDescriptor<>("street", ValueType.TEXT);
    public static final AttributeDescriptor<String> POSTAL_CODE = new AttributeDescriptor<>("postalCode", ValueType.TEXT);
    public static final AttributeDescriptor<Integer> AREA = new AttributeDescriptor<>("area", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_METRE, UNITS_SQUARED);
    public static final AttributeDescriptor<String> COUNTRY = CityAsset.COUNTRY.withOptional(true);
    public static final AttributeDescriptor<String> CITY = CityAsset.CITY.withOptional(false);

    public static final AssetDescriptor<BuildingAsset> DESCRIPTOR = new AssetDescriptor<>("office-building", "4b5966", BuildingAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected BuildingAsset() {
    }

    public BuildingAsset(String name) {
        super(name);
    }

    public Optional<String> getStreet() {
        return getAttributes().getValue(STREET);
    }

    public Optional<Integer> getArea() {
        return getAttributes().getValue(AREA);
    }

    public Optional<String> getPostalCode() {
        return getAttributes().getValue(POSTAL_CODE);
    }

    @Override
    public BuildingAsset setId(String id) {
        super.setId(id);
        return this;
    }

    @Override
    public BuildingAsset setName(String name) throws IllegalArgumentException {
        super.setName(name);
        return this;
    }

    @Override
    public BuildingAsset setAccessPublicRead(boolean accessPublicRead) {
        super.setAccessPublicRead(accessPublicRead);
        return this;
    }

    @Override
    public BuildingAsset setParent(Asset<?> parent) {
        super.setParent(parent);
        return this;
    }

    @Override
    public BuildingAsset setParentId(String parentId) {
        super.setParentId(parentId);
        return this;
    }

    @Override
    public BuildingAsset setRealm(String realm) {
        super.setRealm(realm);
        return this;
    }

    @Override
    public BuildingAsset setAttributes(AttributeMap attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public Asset<?> setAttributes(Attribute<?>... attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public BuildingAsset setAttributes(Collection<Attribute<?>> attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public BuildingAsset setLocation(GeoJSONPoint location) {
        super.setLocation(location);
        return this;
    }

    @Override
    public BuildingAsset setTags(String[] tags) {
        super.setTags(tags);
        return this;
    }

    @Override
    public BuildingAsset setEmail(String email) {
        super.setEmail(email);
        return this;
    }

    @Override
    public BuildingAsset setNotes(String notes) {
        super.setNotes(notes);
        return this;
    }

    @Override
    public BuildingAsset setModel(String model) {
        super.setModel(model);
        return this;
    }

    @Override
    public BuildingAsset addAttributes(Attribute<?>... attributes) {
        super.addAttributes(attributes);
        return this;
    }

    @Override
    public BuildingAsset addOrReplaceAttributes(Attribute<?>... attributes) {
        super.addOrReplaceAttributes(attributes);
        return this;
    }

    @Override
    public BuildingAsset setManufacturer(String manufacturer) {
        super.setManufacturer(manufacturer);
        return this;
    }
}
