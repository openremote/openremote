/*
 * Copyright 2016, OpenRemote Inc.
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

import elemental.json.JsonType;
import elemental.json.JsonValue;
import org.openremote.model.Attribute;
import org.openremote.model.MetadataItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Asset attribute meta item name is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we want to depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetAttributeMeta {

    /**
     * A human-friendly string that can be displayed in UI instead of the raw attribute name.
     */
    LABEL("urn:openremote:asset:meta:label", true, JsonType.STRING),

    /**
     * Format string that can be used to render the attribute value, see {@link java.util.Formatter}.
     */
    FORMAT("urn:openremote:asset:meta:format", true, JsonType.STRING),

    /**
     * A human-friendly string describing the purpose of an attribute, useful when rendering editors.
     */
    DESCRIPTION("urn:openremote:asset:meta:description", true, JsonType.STRING),

    /**
     * Points to semantic description of the attribute, typically a URI.
     */
    ABOUT("urn:openremote:asset:meta:about", true, JsonType.STRING),

    /**
     * Marks the attribute as read-only.
     */
    READ_ONLY("urn:openremote:asset:meta:readOnly", true, JsonType.BOOLEAN),

    /**
     * Default value that might be used when editing an attribute.
     */
    DEFAULT("urn:openremote:asset:meta:default", false, JsonType.STRING),

    /**
     * Minimum range constraint for numeric attribute values.
     */
    RANGE_MIN("urn:openremote:asset:meta:rangeMin", true, JsonType.NUMBER),

    /**
     * Maximum range constraint for numeric attribute values.
     */
    RANGE_MAX("urn:openremote:asset:meta:rangeMax", true, JsonType.NUMBER),

    /**
     * Step increment/decrement constraint for numeric attribute values.
     */
    STEP("urn:openremote:asset:meta:step", true, JsonType.NUMBER),

    /**
     * Regex (Java syntax) constraint for string attribute values.
     */
    PATTERN("urn:openremote:asset:meta:pattern", true, JsonType.STRING);

    final protected String name;
    final protected boolean editable; // Can the user edit an asset and apply this meta item?
    final protected JsonType valueType;

    AssetAttributeMeta(String name, boolean editable, JsonType valueType) {
        this.name = name;
        this.editable = editable;
        this.valueType = valueType;
    }

    public String getName() {
        return name;
    }

    public boolean isEditable() {
        return editable;
    }

    public JsonType getValueType() {
        return valueType;
    }

    public static AssetAttributeMeta[] editable() {
        List<AssetAttributeMeta> list = new ArrayList<>();
        for (AssetAttributeMeta meta : values()) {
            if (meta.isEditable())
                list.add(meta);
        }
        return list.toArray(new AssetAttributeMeta[list.size()]);
    }

    public static Boolean isEditable(String name) {
        for (AssetAttributeMeta assetAttributeMeta : editable()) {
            if (assetAttributeMeta.getName().equals(name))
                return assetAttributeMeta.isEditable();
        }
        return null;
    }

    public static MetadataItem createMetadataItem(AssetAttributeMeta name, JsonValue value) {
        return new MetadataItem(name.getName(), value);
    }

    public static MetadataItem getFirst(Attribute attribute, AssetAttributeMeta meta) {
        return attribute.hasMetaItem(meta.getName())
            ? attribute.getMetadata().first(meta.getName())
            : null;
    }

    // In theory, meta item values can be of any JSON type. In practice, these are the types we can edit.
    public enum ValueType {
        STRING("String", JsonType.STRING),
        DECIMAL("Decimal", JsonType.NUMBER),
        BOOLEAN("Boolean", JsonType.BOOLEAN);

        ValueType(String name, JsonType valueType) {
            this.name = name;
            this.valueType = valueType;
        }

        public final String name;
        public final JsonType valueType;

        public static ValueType byValueType(JsonType valueType) {
            for (ValueType vt : values()) {
                if (vt.valueType == valueType)
                    return vt;
            }
            return null;
        }
    }

}
