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

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.*;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.model.Constants.NAMESPACE;
import static org.openremote.model.asset.AssetMeta.*;

public class AssetAttribute extends Attribute {

    final protected String assetId;

    public static AssetAttribute createEmpty() {
        return new AssetAttribute(null, Json.createObject());
    }

    public AssetAttribute(String name) {
        super(name);
        this.assetId = null;
    }

    public AssetAttribute(String name, AttributeType type) {
        this(null, name, type);
    }

    public AssetAttribute(String name, AttributeType type, JsonValue value) {
        this(null, name, type, value);
    }

    public AssetAttribute(String name, JsonObject jsonObject) {
        this(null, name, jsonObject);
    }

    public AssetAttribute(String assetId, String name, AttributeType type) {
        super(name, type);
        this.assetId = assetId;
    }

    public AssetAttribute(String assetId, String name, AttributeType type, JsonValue value) {
        super(name, type, value);
        this.assetId = assetId;
    }

    public AssetAttribute(String assetId, String name, JsonObject jsonObject) {
        super(name, jsonObject);
        this.assetId = assetId;
    }

    public AssetAttribute(AssetAttribute assetAttribute) {
        super(assetAttribute);
        this.assetId = assetAttribute.getAssetId();
    }

    @Override
    public AssetAttribute setMeta(Meta meta) {
        return (AssetAttribute) super.setMeta(meta);
    }

    public String getAssetId() {
        return assetId;
    }

    public AttributeRef getReference() {
        if (getAssetId() == null) {
            throw new IllegalStateException("Asset identifier not set on: " + this);
        }
        return new AttributeRef(getAssetId(), getName());
    }

    /**
     * @return The current value.
     */
    public AttributeState getState() {
        return new AttributeState(
            getReference(),
            getValue()
        );
    }

    /**
     * @return The current value and its timestamp represented as an attribute event.
     */
    public AttributeEvent getStateEvent() {
        return new AttributeEvent(
            getState(),
            getValueTimestamp()
        );
    }

    public boolean hasMetaItem(AssetMeta assetMeta) {
        return hasMetaItem(assetMeta.getUrn());
    }

    public MetaItem firstMetaItem(AssetMeta assetMeta) {
        return firstMetaItem(assetMeta.getUrn());
    }

    public List<MetaItem> getMetaItems(AssetMeta assetMeta) {
        return getMetaItems(assetMeta.getUrn());
    }

    public String getLabel() {
        return hasMetaItem(LABEL) ? firstMetaItem(LABEL).getValueAsString() : getName();
    }

    public AssetAttribute setLabel(String label) {
        return setMeta(
            getMeta().replace(AssetMeta.LABEL, new MetaItem(AssetMeta.LABEL, Json.create(label)))
        );
    }

    public boolean isExecutable() {
        return hasMetaItem(EXECUTABLE) ? firstMetaItem(EXECUTABLE).getValueAsBoolean() : false;
    }

    public AssetAttribute setExecutable(boolean executable) {
        if (executable) {
            return setMeta(
                getMeta()
                    .replace(EXECUTABLE, new MetaItem(EXECUTABLE, Json.create(true)))
            );
        } else {
            return setMeta(
                getMeta().removeAll(EXECUTABLE)
            );
        }
    }

    public boolean isShowOnDashboard() {
        return hasMetaItem(SHOWN_ON_DASHBOARD) && firstMetaItem(SHOWN_ON_DASHBOARD).isValueTrue();
    }

    public String getFormat() {
        return hasMetaItem(FORMAT) ? firstMetaItem(FORMAT).getValueAsString() : null;
    }

    public String getDescription() {
        return hasMetaItem(DESCRIPTION) ? firstMetaItem(DESCRIPTION).getValueAsString() : null;
    }

    public boolean isEnabled() {
        // Default to true
        return hasMetaItem(ENABLED) ? firstMetaItem(AssetMeta.ENABLED).isValueTrue() : true;
    }

    public AssetAttribute setEnabled(boolean enabled) {
        return setMeta(
            getMeta().replace(AssetMeta.ENABLED.getUrn(), new MetaItem(AssetMeta.ENABLED, Json.create(enabled)))
        );
    }

    public boolean isProtected() {
        return hasMetaItem(PROTECTED) && firstMetaItem(PROTECTED).isValueTrue();
    }

    public boolean isReadOnly() {
        return hasMetaItem(READ_ONLY) && firstMetaItem(READ_ONLY).isValueTrue();
    }

    public boolean isStoreDatapoints() {
        return hasMetaItem(STORE_DATA_POINTS) && firstMetaItem(STORE_DATA_POINTS).isValueTrue();
    }

    public boolean isRuleState() {
        return hasMetaItem(RULE_STATE) && firstMetaItem(RULE_STATE).isValueTrue();
    }

    public boolean isRuleEvent() {
        return hasMetaItem(RULE_EVENT) && firstMetaItem(RULE_EVENT).isValueTrue();
    }

    public String getRuleEventExpires() {
        return hasMetaItem(RULE_EVENT_EXPIRES) ? firstMetaItem(RULE_EVENT_EXPIRES).getValueAsString() : null;
    }

    public boolean isAgentLinked() {
        return hasMetaItem(AGENT_LINK);
    }

    public static Predicate<Attribute> hasAssetMetaItem(String name) {
        return attribute -> attribute.hasMetaItem(name);
    }

    public static Predicate<Attribute> hasAssetMetaItem(AssetMeta assetMeta) {
        return attribute -> attribute.hasMetaItem(assetMeta.getUrn());
    }

    public static Predicate<MetaItem> isAssetMetaItem(AssetMeta assetMeta) {
        return isMetaItem(assetMeta.getUrn());
    }

    public static Predicate<MetaItem> isAssetMetaItem(String name) {
        return isMetaItem(name);
    }

    public static Function<Asset, AssetAttribute> findAssetAttribute(String name) {
        return asset -> getAssetAttributeFromJson(asset.getId(), name).apply(asset.getAttributes());
    }

    public static Predicate<Asset> containsAssetAttributeNamed(String name) {
        return asset -> findAssetAttribute(name).apply(asset) != null;
    }

    public static Predicate<Asset> isAssetAttribute(String name, Predicate<AssetAttribute> predicate) {
        return asset -> {
            AssetAttribute attribute = findAssetAttribute(name).apply(asset);
            return attribute != null && predicate.test(attribute);
        };
    }

    public static Predicate<AssetAttribute> isMatchingAttributeEvent(AttributeEvent event) {
        return attribute -> attribute.getAssetId().equals(event.getEntityId())
            && attribute.getName().equals(event.getAttributeName());
    }

    /**
     * Returns either an attribute with protected-only meta items, or <code>null</code> if
     * the attribute is private.
     */
    public static Function<Stream<AssetAttribute>, Stream<AssetAttribute>> filterProtectedAssetAttribute() {
        return attributeStream ->
            attributeStream
                .filter(AssetAttribute::isProtected)
                .filter(Attribute::hasMeta)
                .map(attribute -> {
                    // Any meta item of the attribute, if it's in our namespace, must be protected READ to be included
                    Meta protectedMeta = new Meta();
                    for (MetaItem metaItem : attribute.getMeta().all()) {
                        if (!metaItem.getName().startsWith(NAMESPACE))
                            continue;

                        AssetMeta wellKnownMeta = AssetMeta.byUrn(metaItem.getName());
                        if (wellKnownMeta != null && wellKnownMeta.getAccess().protectedRead) {
                            protectedMeta.add(
                                new MetaItem(metaItem.getName(), metaItem.getValue())
                            );
                        }
                    }
                    if (protectedMeta.size() > 0)
                        attribute.setMeta(protectedMeta);

                    return attribute;
                });
    }

    public static Function<JsonObject, Stream<AssetAttribute>> getAssetAttributesFromJson(String assetId) {
        return jsonObject -> {
            if (jsonObject == null || jsonObject.keys().length == 0) {
                return Stream.empty();
            }
            Stream.Builder<AssetAttribute> sb = Stream.builder();
            String[] keys = jsonObject.keys();
            for (String key : keys) {
                sb.add(new AssetAttribute(assetId, key, jsonObject.getObject(key)));
            }
            return sb.build();
        };
    }

    public static Function<JsonObject, AssetAttribute> getAssetAttributeFromJson(String assetId, String attributeName) {
        return jsonObject -> {
            if (jsonObject == null || jsonObject.keys().length == 0 || !jsonObject.hasKey(attributeName)) {
                return null;
            }
            return new AssetAttribute(assetId, attributeName, jsonObject.getObject(attributeName));
        };
    }

    /**
     * Maps the attribute stream to a {@link JsonObject}, duplicate attribute names are not
     * allowed (internally this is a hash map in Elemental, thus not allowing duplicate
     * JSON object property names).
     */
    public static Function<Stream<AssetAttribute>, JsonObject> getAssetAttributesAsJson() {
        return attributeStream -> {
            List<AssetAttribute> list = attributeStream.collect(Collectors.toList());
            if (list.size() == 0)
                return null;
            JsonObject jsonObject = Json.createObject();
            for (AssetAttribute attribute : list) {
                jsonObject.put(attribute.getName(), attribute.getJsonObject());
            }
            return jsonObject;
        };
    }

}
