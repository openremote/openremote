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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.model.Constants.NAMESPACE;
import static org.openremote.model.asset.AssetMeta.*;

public class AssetAttribute extends Attribute {

    final protected Optional<String> assetId;

    public static AssetAttribute createEmpty() {
        return new AssetAttribute(null, Json.createObject());
    }

    public AssetAttribute(String name) {
        super(name);
        this.assetId = Optional.empty();
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
        this.assetId = Optional.ofNullable(assetId);
    }

    public AssetAttribute(String assetId, String name, AttributeType type, JsonValue value) {
        super(name, type, value);
        this.assetId = Optional.ofNullable(assetId);
    }

    public AssetAttribute(String assetId, String name, JsonObject jsonObject) {
        super(name, jsonObject);
        this.assetId = Optional.ofNullable(assetId);
    }

    public AssetAttribute(AssetAttribute assetAttribute) {
        super(assetAttribute);
        this.assetId = assetAttribute.getAssetId();
    }

    @Override
    public AssetAttribute setMeta(Meta meta) {
        return (AssetAttribute) super.setMeta(meta);
    }

    public Optional<String> getAssetId() {
        return assetId;
    }

    public AttributeRef getReference() {
        return getAssetId()
            .map(assetId -> new AttributeRef(assetId, getName()))
            .orElseThrow(() -> new IllegalStateException("Asset identifier not set on: " + this));
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

    public Optional<MetaItem> firstMetaItem(AssetMeta assetMeta) {
        return firstMetaItem(assetMeta.getUrn());
    }

    public String getLabel() {
        return firstMetaItem(LABEL).map(AbstractValueHolder::getValueAsString).orElse(getName());
    }

    public AssetAttribute setLabel(String label) {
        return setMeta(
            getMeta().replace(AssetMeta.LABEL, new MetaItem(AssetMeta.LABEL, Json.create(label)))
        );
    }

    public boolean isExecutable() {
        return firstMetaItem(EXECUTABLE).map(AbstractValueHolder::isValueTrue).orElse(false);
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
        return firstMetaItem(SHOWN_ON_DASHBOARD).map(AbstractValueHolder::isValueTrue).orElse(false);
    }

    public Optional<String> getFormat() {
        return firstMetaItem(FORMAT).map(AbstractValueHolder::getValueAsString);
    }

    public Optional<String> getDescription() {
        return firstMetaItem(DESCRIPTION).map(AbstractValueHolder::getValueAsString);
    }

    /**
     * Defaults to <code>true</code> if there is no {@link AssetMeta#ENABLED} item.
     */
    public boolean isEnabled() {
        return firstMetaItem(ENABLED).map(AbstractValueHolder::isValueTrue).orElse(true);
    }

    public AssetAttribute setEnabled(boolean enabled) {
        return setMeta(
            getMeta().replace(AssetMeta.ENABLED.getUrn(), new MetaItem(AssetMeta.ENABLED, Json.create(enabled)))
        );
    }

    public boolean isProtected() {
        return firstMetaItem(PROTECTED).map(AbstractValueHolder::isValueTrue).orElse(false);
    }

    public boolean isReadOnly() {
        return firstMetaItem(READ_ONLY).map(AbstractValueHolder::isValueTrue).orElse(false);
    }

    public boolean isStoreDatapoints() {
        return firstMetaItem(STORE_DATA_POINTS).map(AbstractValueHolder::isValueTrue).orElse(false);
    }

    public boolean isRuleState() {
        return firstMetaItem(RULE_STATE).map(AbstractValueHolder::isValueTrue).orElse(false);
    }

    public boolean isRuleEvent() {
        return firstMetaItem(RULE_EVENT).map(AbstractValueHolder::isValueTrue).orElse(false);
    }

    public Optional<String> getRuleEventExpires() {
        return firstMetaItem(RULE_EVENT_EXPIRES).map(AbstractValueHolder::getValueAsString);
    }

    public  static Predicate<Attribute> hasMetaItem(AssetMeta assetMeta) {
        return hasMetaItem(assetMeta.getUrn());
    }

    public static Predicate<MetaItem> isAssetMetaItem(AssetMeta assetMeta) {
        return isMetaItem(assetMeta.getUrn());
    }

    public static Function<Asset, Optional<AssetAttribute>> findAssetAttribute(String name) {
        return asset -> getAssetAttributeFromJson(asset.getId(), name).apply(asset.getAttributes());
    }

    public static Predicate<Asset> containsAssetAttributeNamed(String name) {
        return asset -> findAssetAttribute(name).apply(asset).isPresent();
    }

    public static Predicate<Asset> isAssetAttribute(String name, Predicate<AssetAttribute> predicate) {
        return asset -> {
            Optional<AssetAttribute> attribute = findAssetAttribute(name).apply(asset);
            return attribute.isPresent() && predicate.test(attribute.get());
        };
    }

    public static Predicate<AssetAttribute> isMatchingAttributeEvent(AttributeEvent event) {
        return attribute -> attribute.getAssetId().map(assetId -> assetId.equals(event.getEntityId())).orElse(false)
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
                        boolean protectedRead =
                            AssetMeta.byUrn(metaItem.getName()).map(meta -> meta.getAccess().protectedRead).orElse(false);
                        if (protectedRead) {
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

    public static Function<JsonObject, Optional<AssetAttribute>> getAssetAttributeFromJson(String assetId, String attributeName) {
        return jsonObject -> {
            if (jsonObject == null || jsonObject.keys().length == 0 || !jsonObject.hasKey(attributeName)) {
                return Optional.empty();
            }
            return Optional.of(new AssetAttribute(assetId, attributeName, jsonObject.getObject(attributeName)));
        };
    }

    /**
     * Maps the attribute stream to a {@link JsonObject}, duplicate attribute names are not
     * allowed (internally this is a hash map in Elemental, thus not allowing duplicate
     * JSON object property names).
     */
    public static Function<Stream<AssetAttribute>, Optional<JsonObject>> getAssetAttributesAsJson() {
        return attributeStream -> {
            List<AssetAttribute> list = attributeStream.collect(Collectors.toList());
            if (list.size() == 0)
                return Optional.empty();
            JsonObject jsonObject = Json.createObject();
            for (AssetAttribute attribute : list) {
                jsonObject.put(attribute.getName(), attribute.getJsonObject());
            }
            return Optional.of(jsonObject);
        };
    }

}
