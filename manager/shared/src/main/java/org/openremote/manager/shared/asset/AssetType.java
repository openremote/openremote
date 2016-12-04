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
import elemental.json.JsonString;

import java.util.ArrayList;
import java.util.List;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we want to depend on in our platform.
 *
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetType {

    CUSTOM(null),
    TENANT("urn:openremote:asset:tenant", false),
    AGENT("urn:openremote:asset:agent"),
    DEVICE("urn:openremote:asset:device", false),
    BUILDING("urn:openremote:asset:building"),
    FLOOR("urn:openremote:asset:floor"),
    ROOM("urn:openremote:asset:room");

    final protected String value;
    final protected boolean editable;

    AssetType(String value, boolean editable) {
        this.value = value;
        this.editable = editable;
    }

    AssetType(String value) {
        this(value, true);
    }

    public String getValue() {
        return value;
    }

    public boolean isEditable() {
        return editable;
    }

    public JsonString getJsonValue() {
        return Json.create(getValue());
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
        return wellKnownType != null && wellKnownType.equals(DEVICE);
    }
}
