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

import javax.persistence.Entity;
import java.util.Collection;

@Entity
public class ElectricityProducerWindAsset extends ElectricityProducerAsset {

    public static final AssetDescriptor<ElectricityProducerWindAsset> DESCRIPTOR = new AssetDescriptor<>("wind-turbine", "4B87EA", ElectricityProducerWindAsset.class);

    ElectricityProducerWindAsset() {
    }

    public ElectricityProducerWindAsset(String name) {
        super(name);
    }


    @Override
    public ElectricityProducerWindAsset setId(String id) {
        super.setId(id);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setName(String name) throws IllegalArgumentException {
        super.setName(name);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setAccessPublicRead(boolean accessPublicRead) {
        super.setAccessPublicRead(accessPublicRead);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setParent(Asset<?> parent) {
        super.setParent(parent);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setParentId(String parentId) {
        super.setParentId(parentId);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setRealm(String realm) {
        super.setRealm(realm);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setAttributes(AttributeMap attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public Asset<?> setAttributes(Attribute<?>... attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setAttributes(Collection<Attribute<?>> attributes) {
        super.setAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset addAttributes(Attribute<?>... attributes) {
        super.addAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset addOrReplaceAttributes(Attribute<?>... attributes) {
        super.addOrReplaceAttributes(attributes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setLocation(GeoJSONPoint location) {
        super.setLocation(location);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setTags(String[] tags) {
        super.setTags(tags);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setEmail(String email) {
        super.setEmail(email);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setNotes(String notes) {
        super.setNotes(notes);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setManufacturer(String manufacturer) {
        super.setManufacturer(manufacturer);
        return this;
    }

    @Override
    public ElectricityProducerWindAsset setModel(String model) {
        super.setModel(model);
        return this;
    }
}
