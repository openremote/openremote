package org.openremote.manager.shared.asset;

import elemental.json.Json;
import elemental.json.JsonString;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we want to depend on in our platform.
 */
public enum AssetType {

    GENERIC("urn:openremote:asset:generic"),
    AGENT("urn:openremote:asset:agent"),
    BUILDING("urn:openremote:asset:building"),
    FLOOR("urn:openremote:asset:floor"),
    ROOM("urn:openremote:asset:room"),
    DEVICE("urn:openremote:asset:device");

    final protected String value;

    AssetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public JsonString getJsonValue() {
        return Json.create(getValue());
    }
}
