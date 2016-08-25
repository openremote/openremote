package org.openremote.manager.shared.asset;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains
 * the well-known URIs for functionality we want to depend on in our platform.
 */
public enum AssetType {

    GENERIC("urn:openremote:asset:generic"),
    BUILDING("urn:openremote:asset:building"),
    ROOM("urn:openremote:asset:room");

    final protected String value;

    AssetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
