package org.openremote.model.asset;

import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;

import java.util.stream.Collectors;

/**
 * GraphQL type for representing a key-value entry from an AttributeMap.
 */
public class AttributeEntry {
    private String key;
    private Attribute value;

    public AttributeEntry(String key, Attribute value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Attribute getValue() {
        return value;
    }

    public void setValue(Attribute value) {
        this.value = value;
    }

    public static AttributeEntry[] getAttributeEntryList(AttributeMap map) {
        return map.entrySet().stream().map(it -> new AttributeEntry(it.getKey(), it.getValue())).toArray(AttributeEntry[]::new);
    }
}

