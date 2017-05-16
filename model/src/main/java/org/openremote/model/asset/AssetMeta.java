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

import org.openremote.model.ValidationFailure;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.HasUniqueResourceName;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.openremote.model.Constants.ASSET_META_NAMESPACE;
import static org.openremote.model.asset.AssetMeta.AssetMetaValidationFailure.VALUE_DOES_NOT_MATCH_ASSET_META_TYPE;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

/**
 * Asset attribute meta item name is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we can depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetMeta implements HasUniqueResourceName {

    /**
     * Marks an attribute of an agent asset as a {@link org.openremote.model.asset.agent.ProtocolConfiguration}.
     * The attribute value is a protocol URN.
     */
    PROTOCOL_CONFIGURATION(ASSET_META_NAMESPACE + ":protocolConfiguration", new Access(false, false, true), ValueType.BOOLEAN),

    /**
     * Links the attribute to an agent's {@link org.openremote.model.asset.agent.ProtocolConfiguration}, connecting it
     * to a sensor and/or actuator.
     */
    AGENT_LINK(ASSET_META_NAMESPACE + ":agentLink", new Access(false, false, true), ValueType.ARRAY),

    /**
     * A human-friendly string that can be displayed in UI instead of the raw attribute name.
     */
    LABEL(ASSET_META_NAMESPACE + ":label", new Access(true, true, true), ValueType.STRING),

    /**
     * If there is a dashboard, some kind of attribute overview, should this attribute be shown.
     */
    SHOW_ON_DASHBOARD(ASSET_META_NAMESPACE + ":showOnDashboard", new Access(true, true, true), ValueType.BOOLEAN),

    /**
     * Format string that can be used to render the attribute value, see https://github.com/alexei/sprintf.js.
     */
    FORMAT(ASSET_META_NAMESPACE + ":format", new Access(true, false, true), ValueType.STRING),

    /**
     * A human-friendly string describing the purpose of an attribute, useful when rendering editors.
     */
    DESCRIPTION(ASSET_META_NAMESPACE + ":description", new Access(true, false, true), ValueType.STRING),

    /**
     * Points to semantic description of the attribute, typically a URI.
     */
    ABOUT(ASSET_META_NAMESPACE + ":about", new Access(true, false, true), ValueType.STRING),

    /**
     * Marks the attribute as read-only for clients. South-bound changes of the attribute are not possible.
     * North-bound attribute updates made by protocols and changes made by rules are possible.
     */
    READ_ONLY(ASSET_META_NAMESPACE + ":readOnly", new Access(true, false, true), ValueType.BOOLEAN),

    /**
     * Marks the attribute as protected and accessible to restricted users, see {@link UserAsset}.
     */
    PROTECTED(ASSET_META_NAMESPACE + ":protected", new Access(false, false, true), ValueType.BOOLEAN),

    /**
     * Default value that might be used when editing an attribute.
     */
    DEFAULT(ASSET_META_NAMESPACE + ":default", new Access(false, false, false), ValueType.STRING),

    /**
     * Minimum range constraint for numeric attribute values.
     */
    RANGE_MIN(ASSET_META_NAMESPACE + ":rangeMin", new Access(true, false, true), ValueType.NUMBER),

    /**
     * Maximum range constraint for numeric attribute values.
     */
    RANGE_MAX(ASSET_META_NAMESPACE + ":rangeMax", new Access(true, false, true), ValueType.NUMBER),

    /**
     * Step increment/decrement constraint for numeric attribute values.
     */
    STEP(ASSET_META_NAMESPACE + ":step", new Access(true, false, true), ValueType.NUMBER),

    /**
     * Regex (Java syntax) constraint for string attribute values.
     */
    PATTERN(ASSET_META_NAMESPACE + ":pattern", new Access(true, false, true), ValueType.STRING),

    /**
     * Should attribute values be stored in time series database
     */
    STORE_DATA_POINTS(ASSET_META_NAMESPACE + ":storeDataPoints", new Access(true, false, true), ValueType.BOOLEAN),

    /**
     * Should attribute writes be processed by the rules engines as {@link AssetState} facts in knowledge sessions,
     * with a lifecycle that reflects the state of the asset attribute. The state facts in the rules sessions are kept
     * in sync with asset changes: For an attribute there will always be a single fact that is updated
     * when the attribute is updated. If you want two types of facts in your rules knowledge session for a single
     * attribute, with state and event behavior, combine this with {@link #RULE_EVENT}.
     */
    RULE_STATE(ASSET_META_NAMESPACE + ":ruleState", new Access(true, false, true), ValueType.BOOLEAN),

    /**
     * Should attribute writes be processed by the rules engines as events in knowledge sessions. Any attribute
     * update will be inserted as an {@link AssetEvent} fact in the rules sessions, these events are expired
     * automatically after a defined time and/or if they can no longer be matched by rule LHS time constraints.
     * If you want two types of facts in your rules knowledge session for a single attribute, with state and event
     * behavior, combine this with {@link #RULE_STATE}.
     */
    RULE_EVENT(ASSET_META_NAMESPACE + ":ruleEvent", new Access(true, false, true), ValueType.BOOLEAN),

    /**
     * Set maximum lifetime of {@link AssetEvent} facts in knowledge sessions, for example "1h30m". The rules
     * engine will remove {@link AssetEvent} facts from the rules sessions if they are older than this value
     * (using event source timestamp, not event processing time).
     */
    RULE_EVENT_EXPIRES(ASSET_META_NAMESPACE + ":ruleEventExpires", new Access(true, false, true), ValueType.STRING),

    /**
     * Enable flag to be used by asset attributes that could require this functionality (e.g. {@link org.openremote.model.asset.agent.ProtocolConfiguration})
     */
    ENABLED(ASSET_META_NAMESPACE + ":enabled", new Access(true, false, true), ValueType.BOOLEAN),

    /**
     * Marks an attribute as being executable so it then supports values of type {@link AttributeExecuteStatus}.
     */
    EXECUTABLE(ASSET_META_NAMESPACE + ":executable", new Access(true, false, true), ValueType.BOOLEAN);

    public enum AssetMetaValidationFailure implements ValidationFailure {
        VALUE_DOES_NOT_MATCH_ASSET_META_TYPE
    }

    final protected String urn;
    final protected Access access;
    final protected ValueType valueType;

    AssetMeta(String urn, Access access, ValueType valueType) {
        this.urn = urn;
        this.access = access;
        this.valueType = valueType;
    }

    public String getUrn() {
        return urn;
    }

    public Access getAccess() {
        return access;
    }

    public ValueType getValueType() {
        return valueType;
    }

    /**
     * If this is a well-known item, the value must match the defined type.
     * Third-party items are always valid.
     */
    public Optional<ValidationFailure> isValidValue(Value value) {
        if (value == null) {
            return Optional.empty();
        }
        if (getValueType() != value.getType()) {
            return Optional.of(VALUE_DOES_NOT_MATCH_ASSET_META_TYPE);
        }
        return Optional.empty();
    }

    public static AssetMeta[] editable() {
        List<AssetMeta> list = new ArrayList<>();
        for (AssetMeta meta : values()) {
            if (meta.getAccess().editable)
                list.add(meta);
        }

        list.sort(Comparator.comparing(Enum::name));

        return list.toArray(new AssetMeta[list.size()]);
    }

    public static boolean isEditable(String urn) {
        for (AssetMeta assetMeta : editable()) {
            if (assetMeta.getUrn().equals(urn))
                return assetMeta.getAccess().editable;
        }
        return true;
    }

    public static Optional<AssetMeta> getAssetMeta(String urn) {
        if (isNullOrEmpty(urn))
            return Optional.empty();

        for (AssetMeta assetMeta : values()) {
            if (assetMeta.getUrn().equals(urn))
                return Optional.of(assetMeta);
        }
        return Optional.empty();
    }

    public static Optional<ValidationFailure> getValidationFailure(MetaItem item) {
        return item.getName()
            .flatMap(AssetMeta::getAssetMeta)
            .map(assetMeta -> new Pair<>(assetMeta, item.getValue()))
            .filter(pair -> pair.value.isPresent())
            .flatMap(pair -> pair.key.isValidValue(pair.value.get()));
    }

    /**
     * Some well-known items can be protected readable.
     *
     * @see Access#protectedRead
     */
    public static boolean isMetaItemProtectedReadable(MetaItem metaItem) {
        return getAssetMeta(metaItem.getName().orElse(null))
            .map(meta -> meta.getAccess().protectedRead)
            .orElse(false);
    }

    /**
     * Some well-known items can be protected writable.
     *
     * @see Access#protectedWrite
     */
    public static boolean isMetaItemProtectedWritable(MetaItem metaItem) {
        return getAssetMeta(metaItem.getName().orElse(null))
            .map(meta -> meta.getAccess().protectedWrite)
            .orElse(false);
    }

    /**
     * In theory, meta item values can be of any {@link ValueType}. In practice, these
     * are the types we can edit/have editor UI for.
     */
    public enum EditableType {
        STRING("Text", ValueType.STRING),
        NUMBER("Number", ValueType.NUMBER),
        BOOLEAN("On/Off toggle", ValueType.BOOLEAN);

        EditableType(String label, ValueType valueType) {
            this.label = label;
            this.valueType = valueType;
        }

        public final String label;
        public final ValueType valueType;

        public static EditableType byValueType(ValueType valueType) {
            for (EditableType vt : values()) {
                if (vt.valueType == valueType)
                    return vt;
            }
            return null;
        }
    }

    public static class Access {

        /**
         * When restricted clients read protected asset attributes, should this meta item be included?
         */
        final public boolean protectedRead;

        /**
         * When restricted clients write protected asset attributes, should this meta item be included?
         */
        final public boolean protectedWrite;

        /**
         * Can a user add this meta item when editing an asset?
         */
        final public boolean editable;

        public Access(boolean protectedRead, boolean protectedWrite, boolean editable) {
            this.protectedRead = protectedRead;
            this.protectedWrite = protectedWrite;
            this.editable = editable;
        }
    }


}
