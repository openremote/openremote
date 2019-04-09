/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.asset;

import org.openremote.model.attribute.*;

/**
 * The built-in and well-known asset model.
 */
public class DefaultAssetModelProvider implements AssetModelProvider {

    @Override
    public MetaItemDescriptor[] getMetaItemDescriptors() {
        return MetaItemType.values();
    }

    @Override
    public AssetDescriptor[] getAssetDescriptors() {
        return AssetType.values();
    }

    @Override
    public AttributeDescriptor[] getAttributeTypeDescriptors() {
        return AttributeType.values();
    }

    @Override
    public AttributeValueDescriptor[] getAttributeValueDescriptors() {
        return AttributeValueType.values();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
