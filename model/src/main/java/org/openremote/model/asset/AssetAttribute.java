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

import static org.openremote.model.asset.AssetAttribute.Functions.removeAllMetaItem;
import static org.openremote.model.asset.AssetAttribute.Functions.replaceMetaItem;
import static org.openremote.model.asset.AssetMeta.*;

public class AssetAttribute extends Attribute {

    public static final class Functions {

        private Functions() {
        }

        public static Function<AssetAttribute, AssetAttribute> copyAssetAttribute() {
            return a -> new AssetAttribute(a.getAssetId(), a.getName(), Json.parse(a.getJsonObject().toJson()));
        }

        public static <A extends Attribute> Predicate<A> hasMetaItem(AssetMeta assetMeta) {
            return Attribute.Functions.hasMetaItem(assetMeta.getUrn());
        }

        public static Predicate<AssetAttribute> hasMetaItem(AssetMeta assetMeta, JsonValue value) {
            return Attribute.Functions.hasMetaItem(assetMeta.getUrn(), value);
        }

        public static <T extends JsonValue> Predicate<AssetAttribute> hasMetaItem(AssetMeta assetMeta, Class<T> clazz) {
            return Attribute.Functions.hasMetaItem(assetMeta.getUrn(), clazz);
        }

        public static Function<AssetAttribute, Optional<MetaItem>> getMetaItem(AssetMeta meta) {
            return Attribute.Functions.getMetaItem(meta.getUrn());
        }

        public static <T extends JsonValue> Function<AssetAttribute, Optional<MetaItem>> getMetaItem(AssetMeta assetMeta, Class<T> clazz) {
            return Attribute.Functions.getMetaItem(assetMeta.getUrn(), clazz);
        }

        public static Function<AssetAttribute, Optional<MetaItem>> getMetaItem(AssetMeta assetMeta, JsonValue value) {
            return Attribute.Functions.getMetaItem(assetMeta.getUrn(), value);
        }

        public static Function<AssetAttribute, Optional<AttributeRef>> getAttributeLink(AssetMeta meta) {
            return Attribute.Functions.getAttributeLink(meta.getUrn());
        }

        public static <T extends Attribute> Predicate<T> isAttributeLink(AssetMeta meta) {
            return Attribute.Functions.isAttributeLink(meta.getUrn());
        }

        public static Function<AssetAttribute, AssetAttribute> setAttributeLink(AssetMeta meta, AttributeRef attributeRef) {
            return Attribute.Functions.setAttributeLink(meta.getUrn(), attributeRef);
        }

        public static Function<AssetAttribute, AssetAttribute> removeAttributeLink(AssetMeta meta) {
            return Attribute.Functions.removeAttributeLink(meta.getUrn());
        }

        public static Function<AssetAttribute, AssetAttribute> replaceMetaItem(AssetMeta meta, MetaItem item) {
            return attribute -> attribute.setMeta(
                attribute.getMeta().replace(meta.getUrn(), item)
            );
        }

        public static Function<AssetAttribute, AssetAttribute> removeAllMetaItem(AssetMeta meta) {
            return attribute -> attribute.setMeta(
                attribute.getMeta().removeAll(meta.getUrn())
            );
        }

        /**
         * Whether the item is one of {@link AssetMeta}.
         */
        public static Predicate<MetaItem> isMetaItemWellKnown() {
            return item -> getWellKnownAssetMeta(item.getName()).isPresent();
        }

        /**
         * Only well-known items can be protected readable.
         *
         * @see AssetMeta.Access#protectedRead
         */
        public static Predicate<MetaItem> isMetaItemProtectedReadable() {
            return item -> getWellKnownAssetMeta(item.getName()).map(meta -> meta.getAccess().protectedRead).orElse(false);
        }

        /**
         * Only well-known items can be protected readable.
         *
         * @see AssetMeta.Access#protectedWrite
         */
        public static Predicate<MetaItem> isMetaItemProtectedWritable() {
            return item -> getWellKnownAssetMeta(item.getName()).map(meta -> meta.getAccess().protectedWrite).orElse(false);
        }

        public static Predicate<AssetAttribute> matches(AttributeEvent event) {
            return attribute -> attribute.getAssetId().map(assetId -> assetId.equals(event.getEntityId())).orElse(false)
                && attribute.getName().equals(event.getAttributeName());
        }

        /**
         * Returns non-private attributes with non-private meta items.
         *
         * @see #isProtected()
         * @see #isMetaItemProtectedReadable()
         */
        public static Function<Stream<AssetAttribute>, Stream<AssetAttribute>> filterProtectedAssetAttribute() {
            return attributeStream -> attributeStream
                .filter(AssetAttribute::isProtected)
                .map(copyAssetAttribute())
                .map(attribute -> {
                    attribute.setMeta(
                        attribute.getMetaItemStream()
                            .filter(isMetaItemProtectedReadable())
                            .collect(Meta.ITEM_COPY_COLLECTOR)
                    );
                    return attribute;
                });
        }

        public static Function<JsonObject, Optional<AssetAttribute>> getFromJson(String assetId, String attributeName) {
            return jsonObject -> {
                if (jsonObject == null || jsonObject.keys().length == 0 || !jsonObject.hasKey(attributeName)) {
                    return Optional.empty();
                }
                return Optional.of(new AssetAttribute(Optional.ofNullable(assetId), attributeName, jsonObject.getObject(attributeName)));
            };
        }

        public static Function<JsonObject, Optional<AssetAttribute>> getFromJson(String assetId, String attributeName, AttributeType type) {
            return jsonObject -> {
                if (jsonObject == null || jsonObject.keys().length == 0 || !jsonObject.hasKey(attributeName)) {
                    return Optional.empty();
                }
                AssetAttribute attribute = new AssetAttribute(Optional.ofNullable(assetId), attributeName, jsonObject.getObject(attributeName));
                Optional<AttributeType> attributeType = attribute.getType();
                return attributeType.isPresent() && attributeType.get() == type ? Optional.of((attribute)) : Optional.empty();
            };
        }

        public static Function<JsonObject, Stream<AssetAttribute>> getAssetAttributesFromJson(String assetId) {
            return jsonObject -> {
                if (jsonObject == null || jsonObject.keys().length == 0) {
                    return Stream.empty();
                }
                Stream.Builder<AssetAttribute> sb = Stream.builder();
                String[] keys = jsonObject.keys();
                for (String key : keys) {
                    sb.add(new AssetAttribute(Optional.ofNullable(assetId), key, jsonObject.getObject(key)));
                }
                return sb.build();
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

    final protected Optional<String> assetId;

    public AssetAttribute(String name, AttributeType type) {
        this(Optional.empty(), name, type);
    }

    public AssetAttribute(String name) {
        this(Optional.empty(), name);
    }

    public AssetAttribute(String name, AttributeType type, JsonValue value) {
        this(Optional.empty(), name, type, value);
    }

    public AssetAttribute(Optional<String> assetId, String name) {
        super(name);
        this.assetId = assetId;
    }

    public AssetAttribute(Optional<String> assetId, String name, AttributeType type) {
        super(name, type);
        this.assetId = assetId;
    }

    public AssetAttribute(Optional<String> assetId, String name, JsonObject jsonObject) {
        super(name, jsonObject);
        this.assetId = assetId;
    }

    public AssetAttribute(Optional<String> assetId, String name, AttributeType type, JsonValue value) {
        super(name, type, value);
        this.assetId = assetId;
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
        return getMetaItem(assetMeta.getUrn());
    }

    public String getLabel() {
        return firstMetaItem(LABEL).map(AbstractValueHolder::getValueAsString).orElse(getName());
    }

    public AssetAttribute setLabel(String label) {
        return replaceMetaItem(AssetMeta.LABEL, new MetaItem(AssetMeta.LABEL, Json.create(label))).apply(this);
    }

    public boolean isExecutable() {
        return firstMetaItem(EXECUTABLE).map(AbstractValueHolder::isValueTrue).orElse(false);
    }

    public AssetAttribute setExecutable(boolean executable) {
        if (executable) {
            return replaceMetaItem(
                AssetMeta.EXECUTABLE,
                new MetaItem(AssetMeta.EXECUTABLE, Json.create(true))
            ).apply(this);
        } else {
            return removeAllMetaItem(EXECUTABLE)
                .apply(this);
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

    public boolean isAgentLinked() {
        return Functions.hasMetaItem(AGENT_LINK).test(this);
    }
}
