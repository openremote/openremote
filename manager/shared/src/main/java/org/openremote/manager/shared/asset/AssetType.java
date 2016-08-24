package org.openremote.manager.shared.asset;

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
