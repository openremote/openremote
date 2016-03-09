package org.openremote.manager.shared.ngsi;

import elemental.json.JsonObject;

import java.util.LinkedHashSet;
import java.util.Set;

public class Metadata {

    final protected JsonObject jsonObject;

    public Metadata(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public MetadataElement[] getElements() {
        Set<MetadataElement> elements = new LinkedHashSet<>();

        String[] keys = jsonObject.keys();
        for (String key : keys) {
            MetadataElement element = new MetadataElement(key, jsonObject.getObject(key));
            elements.add(element);
        }

        return elements.toArray(new MetadataElement[elements.size()]);
    }

    public MetadataElement getElement(String name) {
        return hasElement(name) ? new MetadataElement(name, jsonObject.getObject(name)) : null;
    }

    public boolean hasElement(String name) {
        return jsonObject.hasKey(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Metadata metadata = (Metadata) o;

        return jsonObject.equals(metadata.jsonObject);
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
