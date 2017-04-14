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

import elemental.json.JsonObject;
import elemental.json.JsonValue;
import org.openremote.model.*;

import java.util.List;

import static org.openremote.model.asset.AssetMeta.*;
import static org.openremote.model.asset.AssetMeta.AGENT_LINK;

public class AssetAttribute extends Attribute<AssetAttribute> {
    final protected String assetId;

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

    public String getAssetId() {
        return assetId;
    }

    public AttributeRef getReference() {
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
        return hasMetaItem(assetMeta.getName());
    }

    public MetaItem firstMetaItem(AssetMeta assetMeta) {
        return firstMetaItem(assetMeta.getName());
    }

    public List<MetaItem> getMetaItems(AssetMeta assetMeta) {
        return getMetaItems(assetMeta.getName());
    }

    public String getLabel() {
        return hasMetaItem(LABEL) ? firstMetaItem(LABEL).getValueAsString() : getName();
    }

    public String getFormat() {
        return hasMetaItem(FORMAT) ? firstMetaItem(FORMAT).getValueAsString() : null;
    }

    public boolean isShowOnDashboard() {
        return hasMetaItem(SHOWN_ON_DASHBOARD) && firstMetaItem(SHOWN_ON_DASHBOARD).isValueTrue();
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
}
