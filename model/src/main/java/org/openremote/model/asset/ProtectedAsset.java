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
import org.openremote.model.Attribute;
import org.openremote.model.Attributes;
import org.openremote.model.Meta;
import org.openremote.model.MetaItem;

import java.util.Date;

import static org.openremote.model.Constants.NAMESPACE;

/**
 * Filters attributes of an asset as to only contain protected attributes, and their
 * protected metadata (see {@link AssetMeta#PROTECTED}, {@link AssetMeta.Access}).
 * <p>
 * Note that third-party metadata items (not in the
 * {@link org.openremote.model.Constants#NAMESPACE}) are never included on
 * a protected attribute!
 */
public class ProtectedAsset extends Asset {

    static JsonObject filterProtectedAttributes(JsonObject unfilteredAttributes) {
        Attributes filteredAttributes = new Attributes();
        Attributes attributes = new Attributes(unfilteredAttributes);
        for (Attribute attribute : attributes.get()) {

            // An attribute must be protected to be included
            if (!attribute.isProtected()) {
                continue;
            }

            Attribute protectedAttribute = attribute.copy();
            filteredAttributes.put(protectedAttribute);

            if (!protectedAttribute.hasMeta())
                continue;

            // Any meta item of the attribute, if it's in our namespace, must be protected READ to be included
            Meta protectedMeta = new Meta();
            for (MetaItem metaItem : protectedAttribute.getMeta().all()) {
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
                protectedAttribute.setMeta(protectedMeta);

        }
        return filteredAttributes.getJsonObject();
    }

    public ProtectedAsset() {
    }

    public ProtectedAsset(String name, AssetType type) {
        super(name, type);
    }

    public ProtectedAsset(String realm, String name, AssetType type) {
        super(realm, name, type);
    }

    public ProtectedAsset(String realmId, String name, String type) {
        super(realmId, name, type);
    }

    public ProtectedAsset(String id, long version, Date createdOn, String name, String type,
                          String parentId, String parentName, String parentType, String[] path,
                          String realmId, String tenantRealm, String tenantDisplayName,
                          JsonObject attributes) {
        super(
            id, version, createdOn, name, type,
            parentId, parentName, parentType, path,
            realmId, tenantRealm, tenantDisplayName,
            filterProtectedAttributes(attributes)
        );
    }

    public ProtectedAsset(Asset parent) {
        super(parent);
    }

    @Override
    public void setAttributes(JsonObject attributes) {
        super.setAttributes(filterProtectedAttributes(attributes));
    }
}
