package org.openremote.manager.shared.ngsi;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class MetadataElement {

    final protected String name;
    final protected JsonObject jsonObject;

    public MetadataElement(String name, JsonObject jsonObject) {
        this.name = name;
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public String getName() {
        return name;
    }

    public JsonValue getValue() {
        return jsonObject.hasKey("value") ? jsonObject.get("value") : null;
    }

    public MetadataElement setValue(JsonValue value) {
        jsonObject.put("value", value);
        return this;
    }

    public String getType() {
        return jsonObject.hasKey("type") ? jsonObject.getString("type") : null;
    }

    public MetadataElement setType(String type) {
        jsonObject.put("type", type);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetadataElement metadata = (MetadataElement) o;

        return jsonObject.equals(metadata.jsonObject);
    }

    @Override
    public int hashCode() {
        return jsonObject.hashCode();
    }
}
