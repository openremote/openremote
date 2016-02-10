package org.openremote.manager.shared.model.ngsi;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class Attribute extends AbstractAttribute {

    final protected JsonObject jsonObject;

    public Attribute(String name, JsonObject jsonObject) {
        super(name);
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    @Override
    public JsonValue getValue() {
        return jsonObject.hasKey("value") ? jsonObject.get("value") : null;
    }

    @Override
    public Attribute setValue(JsonValue value) {
        jsonObject.put("value", value);
        return this;
    }

    public String getType() {
        return jsonObject.hasKey("type") ? jsonObject.getString("type") : null;
    }

    public Attribute setType(String type) {
        jsonObject.put("type", type);
        return this;
    }

    public Metadata getMetadata() {
        return jsonObject.hasKey("metadata") ? new Metadata(jsonObject.getObject("metadata")) : null;
    }

    public Attribute setMetadata(Metadata metadata) {
        jsonObject.put("metadata", metadata.getJsonObject());
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        return jsonObject.equals(attribute.jsonObject);
    }

    @Override
    public int hashCode() {
        return jsonObject.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + jsonObject.toJson();
    }
}
