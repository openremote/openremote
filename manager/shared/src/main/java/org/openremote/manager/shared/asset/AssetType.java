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
package org.openremote.manager.shared.asset;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeType;
import org.openremote.model.Attributes;
import org.openremote.model.Metadata;

import java.util.ArrayList;
import java.util.List;

import static org.openremote.manager.shared.asset.AssetAttributeMeta.*;
import static org.openremote.model.AttributeType.INTEGER;
import static org.openremote.model.AttributeType.STRING;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we want to depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetType {

    CUSTOM(null, true, null),

    TENANT("urn:openremote:asset:tenant", false, null),

    // TODO Arbitrary group of assets? Semantics?
    GROUP("urn:openremote:asset:group", true, null),

    BUILDING("urn:openremote:asset:building", true, new Attributes().put(
        new Attribute("area", INTEGER)
            .setMetadata(new Metadata()
                .add(createMetadataItem(LABEL, Json.create("Surface area")))
                .add(createMetadataItem(DESCRIPTION, Json.create("Floor area of building measured in m²")))
                .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/area")))
            ),
        new Attribute("geoStreet", STRING)
            .setMetadata(new Metadata()
                .add(createMetadataItem(LABEL, Json.create("Street")))
                .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoStreet")))
            ),
        new Attribute("geoPostalCode", AttributeType.INTEGER)
            .setMetadata(new Metadata()
                .add(createMetadataItem(LABEL, Json.create("Postal Code")))
                .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoPostalCode")))
            ),
        new Attribute("geoCity", STRING)
            .setMetadata(new Metadata()
                .add(createMetadataItem(LABEL, Json.create("City")))
                .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCity")))
            ),
        new Attribute("geoCountry", STRING)
            .setMetadata(new Metadata()
                .add(createMetadataItem(LABEL, Json.create("Country")))
                .add(createMetadataItem(ABOUT, Json.create("http://project-haystack.org/tag/geoCountry")))
            )
    ).getJsonObject()),

    FLOOR("urn:openremote:asset:floor", true, null),

    ROOM("urn:openremote:asset:room", true, null),

    AGENT("urn:openremote:asset:agent", true, null),

    THING("urn:openremote:asset:thing", true, null);

    final protected String value;
    final protected boolean editable;
    final protected JsonObject defaultAttributes;

    AssetType(String value, boolean editable, JsonObject defaultAttributes) {
        this.value = value;
        this.editable = editable;
        this.defaultAttributes = defaultAttributes;
    }

    public String getValue() {
        return value;
    }

    public boolean isEditable() {
        return editable;
    }

    public JsonObject getDefaultAttributes() {
        return defaultAttributes;
    }

    public static AssetType[] editable() {
        List<AssetType> list = new ArrayList<>();
        for (AssetType assetType : values()) {
            if (assetType.isEditable())
                list.add(assetType);
        }
        return list.toArray(new AssetType[list.size()]);
    }

    public static AssetType getByValue(String value) {
        if (value == null)
            return CUSTOM;
        for (AssetType assetType : values()) {
            if (value.equals(assetType.getValue()))
                return assetType;
        }
        return CUSTOM;
    }

    public static boolean isLeaf(AssetType wellKnownType) {
        return false;
        // TODO return wellKnownType != null && wellKnownType.equals(DEVICE);
    }
}
