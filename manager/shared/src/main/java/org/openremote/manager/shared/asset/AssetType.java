package org.openremote.manager.shared.asset;

import elemental.json.Json;
import elemental.json.JsonString;

import java.util.ArrayList;
import java.util.List;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we want to depend on in our platform.
 */
public enum AssetType {

    CUSTOM(null),
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
}
