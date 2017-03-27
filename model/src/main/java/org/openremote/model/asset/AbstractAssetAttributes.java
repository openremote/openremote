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
import org.openremote.model.Attributes;
import org.openremote.model.Meta;
import org.openremote.model.MetaItem;

import static org.openremote.model.Constants.NAMESPACE;

public abstract class AbstractAssetAttributes<CHILD extends AbstractAssetAttributes<CHILD, A>, A extends AbstractAssetAttribute> extends Attributes<CHILD, A> {

    final public String assetId;

    public AbstractAssetAttributes() {
        this.assetId = null;
    }

    public AbstractAssetAttributes(JsonObject jsonObject) {
        super(jsonObject);
        this.assetId = null;
    }

    public AbstractAssetAttributes(CHILD attributes) {
        super(attributes);
        this.assetId = null;
    }

    public AbstractAssetAttributes(String assetId) {
        this.assetId = assetId;
    }

    public AbstractAssetAttributes(String assetId, JsonObject jsonObject) {
        super(jsonObject);
        this.assetId = assetId;
    }

    public AbstractAssetAttributes(String assetId, CHILD attributes) {
        super(attributes);
        this.assetId = assetId;
    }

    public AbstractAssetAttributes(Asset asset) {
        super(asset.getAttributes());
        this.assetId = asset.getId();
    }

    public String getAssetId() {
        if (assetId == null) {
            throw new IllegalStateException("Asset identifier not set on: " + this);
        }
        return assetId;
    }

    /* TODO Do we need an "enabled" attribute on many asset types?
    @SuppressWarnings("unchecked")
    public CHILD setEnabled(boolean enabled) {
        if (hasAttribute("enabled")) {
            get("enabled").setValueAsBoolean(enabled);
        } else {
            A enabledAttribute = createAttribute("enabled", Json.createObject());
            enabledAttribute.setType(AttributeType.BOOLEAN);
            enabledAttribute.setValue(Json.create(enabled));
            put(enabledAttribute);
        }
        return (CHILD) this;
    }
    */

    /**
     * Remove all attributes and meta items that are private.
     */
    public void filterProtected() {
        for (A attribute : get()) {

            if (!attribute.isProtected()) {
                remove(attribute.getName());
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
    }
}
