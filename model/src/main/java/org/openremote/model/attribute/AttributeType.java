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
package org.openremote.model.attribute;

import org.openremote.model.asset.AssetMeta;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Optional;

public enum AttributeType implements AttributeDescriptor {

    LOCATION("location", AttributeValueType.GEO_JSON_POINT, new MetaItem(AssetMeta.LABEL, Values.create("Location"))),

    CONSOLE_NAME("consoleName", AttributeValueType.STRING),

    CONSOLE_VERSION("consoleVersion", AttributeValueType.STRING),

    CONSOLE_PLATFORM("consolePlatform", AttributeValueType.STRING),

    CONSOLE_PROVIDERS("consoleProviders", AttributeValueType.OBJECT),

    EMAIL("email", AttributeValueType.EMAIL, new MetaItem(AssetMeta.LABEL, Values.create("Email")));

    final protected String name;
    final protected AttributeValueType attributeValueType;
    final protected MetaItem[] defaultMetaItems;

    AttributeType(String name, AttributeValueType attributeValueType, MetaItem... defaultMetaItems) {
        this.name = name;
        this.attributeValueType = attributeValueType;
        this.defaultMetaItems = defaultMetaItems;
    }

    public static Optional<AttributeDescriptor> getByValue(String name) {
        if (name == null)
            return Optional.empty();

        for (AttributeDescriptor descriptor : values()) {
            if (name.equals(descriptor.getName()))
                return Optional.of(descriptor);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AttributeValueType getValueType() {
        return attributeValueType;
    }

    @Override
    public MetaItem[] getDefaultMetaItems() {
        return defaultMetaItems;
    }

    @Override
    public Value getDefaultValue() {
        return null;
    }
}
