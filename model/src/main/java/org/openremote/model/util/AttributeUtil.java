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
package org.openremote.model.util;

import com.google.gwt.regexp.shared.RegExp;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.AttributeRef;
import org.openremote.model.Meta;
import org.openremote.model.MetaItem;
import org.openremote.model.asset.AbstractAttributeWrapper;
import org.openremote.model.asset.AttributeWrapperFilter;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;

import java.util.*;
import java.util.stream.Collectors;

import static org.openremote.model.Constants.NAMESPACE;

public class AttributeUtil {
    /**
     * Attribute names should be very simple, as we use them in SQL path expressions, etc. and must manually escape.
     */
    public static final String ATTRIBUTE_NAME_PATTERN = "^\\w+$";
    protected static final RegExp attributeNamePattern = RegExp.compile(ATTRIBUTE_NAME_PATTERN);

    private AttributeUtil() {
    }

    /**
     * Returns only attributes and meta that are not private.
     */
    public static List<AssetAttribute> filterByProtected(List<AssetAttribute> attributes) {
        if (attributes == null) {
            return Collections.emptyList();
        }

        List<AssetAttribute> filteredAttributes = new ArrayList<>(attributes);

        for (AssetAttribute attribute : attributes) {

            if (!attribute.isProtected()) {
                filteredAttributes.remove(attribute);
                continue;
            }

            if (!attribute.hasMeta())
                continue;

            // Any meta item of the attribute, if it's in our namespace, must be protected READ to be included
            Meta protectedMeta = new Meta();
            for (MetaItem metaItem : attribute.getMeta().all()) {
                if (!metaItem.getName().startsWith(NAMESPACE))
                    continue;

                AssetMeta wellKnownMeta = AssetMeta.byName(metaItem.getName());
                if (wellKnownMeta != null && wellKnownMeta.getAccess().protectedRead) {
                    protectedMeta.add(
                        new MetaItem(metaItem.getName(), metaItem.getValue())
                    );
                }
            }
            if (protectedMeta.size() > 0)
                attribute.setMeta(protectedMeta);
        }

        return filteredAttributes;
    }

    public static List<AssetAttribute> getAssetAttributesFromJson(String assetId, JsonObject jsonObject) {
        if (jsonObject == null || jsonObject.keys().length == 0) {
            return new ArrayList<>();
        }

        List<AssetAttribute> assetAttributes = new ArrayList<>();
        String[] keys = jsonObject.keys();
        for (String key : keys) {
            assetAttributes.add(new AssetAttribute(assetId, key, jsonObject.getObject(key)));
        }
        return assetAttributes;
    }

    public static JsonObject getAssetAttributesAsJson(List<AssetAttribute> attributes) {
        if (attributes == null) {
            return null;
        }

        JsonObject jsonObject = Json.createObject();
        for (AssetAttribute attribute : attributes) {
            jsonObject.put(attribute.getName(), attribute.getJsonObject());
        }

        return jsonObject;
    }

    public static boolean nameIsValid(String name) {
        return name != null && name.length() > 0 && attributeNamePattern.test(name);
    }


    /**
     * Filter attributes that contain one or more meta items of the specified asset meta type.
     */
    public static List<AssetAttribute> filterByMeta(List<AssetAttribute> attributes, AssetMeta meta) {
        return filterByMeta(attributes, meta.getName());
    }

    /**
     * Filter attributes that contain one or more meta items with the specified name.
     */
    public static List<AssetAttribute> filterByMeta(List<AssetAttribute> attributes, String metaName) {
        return attributes
            .stream()
            .filter(assetAttribute -> assetAttribute.hasMetaItem(metaName))
            .collect(Collectors.toList());
    }

    /**
     * Filter attributes that contain one or more meta items of the specified asset meta type and with the
     * specified value.
     *
     * Value equality uses {@link JsonUtil#equals(JsonValue, JsonValue)} method.
     */
    public static List<AssetAttribute> filterByMeta(List<AssetAttribute> attributes, AssetMeta meta, JsonValue value) {
        return filterByMeta(attributes, meta.getName(), value);
    }

    /**
     * Filter attributes that contain one or more meta items with the specified name and value.
     *
     * Value equality uses {@link JsonUtil#equals(JsonValue, JsonValue)} method.
     */
    public static List<AssetAttribute> filterByMeta(List<AssetAttribute> attributes, String metaName, JsonValue value) {
        return attributes
            .stream()
            .filter(assetAttribute -> assetAttribute.getMetaItems(metaName, value).size() > 0)
            .collect(Collectors.toList());
    }

    public static boolean contains(List<AssetAttribute> attributes, String attributeName) {
        return attributes.stream().anyMatch(assetAttribute -> assetAttribute.getName().equals(attributeName));
    }

    public static AssetAttribute getAttributeByName(List<AssetAttribute> attributes, String attributeName) {
        if (attributes == null || attributeName == null) {
            return null;
        }

        return attributes
            .stream()
            .filter(assetAttribute -> assetAttribute.getName().equals(attributeName))
            .findFirst()
            .orElse(null);
    }

    public static void remove(List<AssetAttribute> attributes, String name) {
        attributes.removeIf(assetAttribute -> assetAttribute.getName().equals(name));
    }

    public static AttributeRef getAttributeRef(String assetId, String attributeName) {
        return new AttributeRef(assetId, attributeName);
    }

    public static AttributeRef getAttributeRef(String assetId, AbstractAttributeWrapper attributeWrapper) {
        return getAttributeRef(assetId, attributeWrapper.getAttribute().getName());
    }

    public static List<String> getNames(List<AssetAttribute> attributes) {
        return attributes
            .stream()
            .map(attribute -> attribute.getName())
            .collect(Collectors.toList());
    }
}
