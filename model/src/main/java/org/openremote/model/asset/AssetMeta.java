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
import org.openremote.model.MetaItem;
import org.openremote.model.units.AttributeUnits;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.openremote.model.Constants.ASSET_META_NAMESPACE;

/**
 * Asset attribute meta item name is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we can depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetMeta {

    /**
     * Links the attribute to an agent's {@link org.openremote.model.asset.agent.ProtocolConfiguration}, connecting it
     * to a sensor and/or actuator.
     */
    AGENT_LINK(ASSET_META_NAMESPACE + ":agentLink", new Access(false, false, true), JsonType.ARRAY),

    /**
     * A human-friendly string that can be displayed in UI instead of the raw attribute name.
     */
    LABEL(ASSET_META_NAMESPACE + ":label", new Access(true, true, true), JsonType.STRING),

    /**
     * If there is a dashboard, some kind of attribute overview, should this attribute be shown.
     */
    SHOWN_ON_DASHBOARD(ASSET_META_NAMESPACE + ":showOnDashboard", new Access(true, true, true), JsonType.BOOLEAN),

    /**
     * Format string that can be used to render the attribute value, see https://github.com/alexei/sprintf.js.
     */
    FORMAT(ASSET_META_NAMESPACE + ":format", new Access(true, false, true), JsonType.STRING),

    /**
     * A human-friendly string describing the purpose of an attribute, useful when rendering editors.
     */
    DESCRIPTION(ASSET_META_NAMESPACE + ":description", new Access(true, false, true), JsonType.STRING),

    /**
     * Points to semantic description of the attribute, typically a URI.
     */
    ABOUT(ASSET_META_NAMESPACE + ":about", new Access(true, false, true), JsonType.STRING),

    /**
     * Marks the attribute as read-only.
     */
    READ_ONLY(ASSET_META_NAMESPACE + ":readOnly", new Access(true, false, true), JsonType.BOOLEAN),

    /**
     * Marks the attribute as protected and accessible to restricted users, see {@link UserAsset}.
     */
    PROTECTED(ASSET_META_NAMESPACE + ":protected", new Access(false, false, true), JsonType.BOOLEAN),

    /**
     * Default value that might be used when editing an attribute.
     */
    DEFAULT(ASSET_META_NAMESPACE + ":default", new Access(false, false, false), JsonType.STRING),

    /**
     * Minimum range constraint for numeric attribute values.
     */
    RANGE_MIN(ASSET_META_NAMESPACE + ":rangeMin", new Access(true, false, true), JsonType.NUMBER),

    /**
     * Maximum range constraint for numeric attribute values.
     */
    RANGE_MAX(ASSET_META_NAMESPACE + ":rangeMax", new Access(true, false, true), JsonType.NUMBER),

    /**
     * Step increment/decrement constraint for numeric attribute values.
     */
    STEP(ASSET_META_NAMESPACE + ":step", new Access(true, false, true), JsonType.NUMBER),

    /**
     * Regex (Java syntax) constraint for string attribute values.
     */
    PATTERN(ASSET_META_NAMESPACE + ":pattern", new Access(true, false, true), JsonType.STRING),

    /**
     * Indicates the units (data sub-type) of the attribute value (should be a valid
     * {@link AttributeUnits} string representation).
     */
    UNITS(ASSET_META_NAMESPACE + ":units", new Access(true, false, true), JsonType.STRING),

    /**
     * Should attribute values be stored in time series database
     */
    STORE_DATA_POINTS(ASSET_META_NAMESPACE + ":storeDataPoints", new Access(true, false, true), JsonType.BOOLEAN),

    /**
     * Should attribute writes be processed by the rules engines as facts in knowledge sessions, with a lifecycle
     * that reflects the state of the asset attribute (the {@link AssetState} facts in the rules sessions are kept
     * in sync with asset changes).
     */
    RULE_STATE(ASSET_META_NAMESPACE + ":ruleState", new Access(true, false, true), JsonType.BOOLEAN),

    /**
     * Should attribute writes be processed by the rules engines as events in knowledge sessions with limited
     * lifecycle that reflects how the event is processed (the {@link AssetEvent} facts
     * in the rules sessions are expired automatically after a certain time and/or if they can no longer be matched
     * by time operations).
     */
    RULE_EVENT(ASSET_META_NAMESPACE + ":ruleEvent", new Access(true, false, true), JsonType.BOOLEAN),

    /**
     * Override rules event expiration, for example "1h30m". Remove {@link AssetEvent}
     * facts from the rules sessions if they are older than this value (using event source timestamp, not event
     * processing time).
     */
    RULE_EVENT_EXPIRES(ASSET_META_NAMESPACE + ":ruleEventExpires", new Access(true, false, true), JsonType.STRING),

    /**
     * Enable flag to be used by asset attributes that could require this functionality (e.g. ProtocolConfiguration)
     */
    ENABLED(ASSET_META_NAMESPACE + ":enabled", new Access(true, false, true), JsonType.BOOLEAN),

    /**
     * Identifies a meta item as a {@link org.openremote.model.asset.macro.MacroAction}.
     */
    MACRO_ACTION(ASSET_META_NAMESPACE + ":macroAction", new Access(true, false, true), JsonType.ARRAY),

    /**
     * Marks an attribute as being an executable command
     */
    COMMAND(ASSET_META_NAMESPACE + ":command", new Access(true, false, true), JsonType.BOOLEAN);

    final protected String urn;
    final protected Access access;
    final protected JsonType valueType;

    AssetMeta(String urn, Access access, JsonType valueType) {
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

    public JsonType getValueType() {
        return valueType;
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

    public static Boolean isEditable(String urn) {
        for (AssetMeta assetMeta : editable()) {
            if (assetMeta.getUrn().equals(urn))
                return assetMeta.getAccess().editable;
        }
        return null;
    }

    public static AssetMeta byUrn(String urn) {
        for (AssetMeta assetMeta : values()) {
            if (assetMeta.getUrn().equals(urn))
                return assetMeta;
        }
        return null;
    }

    public static MetaItem createMetaItem(AssetMeta assetMeta, JsonValue value) {
        return new MetaItem(assetMeta.getUrn(), value);
    }

    /**
     * In theory, meta item values can be of any JSON type. In practice, these are the
     * types we can edit/have editor UI for. We inspect the actual JSON value and
     */
    public enum EditableType {
        STRING("String", JsonType.STRING),
        NUMBER("Number", JsonType.NUMBER),
        BOOLEAN("Boolean", JsonType.BOOLEAN);

        EditableType(String label, JsonType valueType) {
            this.label = label;
            this.valueType = valueType;
        }

        public final String label;
        public final JsonType valueType;

        public static EditableType byValueType(JsonType valueType) {
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
