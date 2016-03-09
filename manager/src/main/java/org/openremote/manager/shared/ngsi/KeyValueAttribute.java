package org.openremote.manager.shared.ngsi;

import elemental.json.JsonValue;

public class KeyValueAttribute extends AbstractAttribute {

    protected JsonValue value;

    public KeyValueAttribute(String name, JsonValue value) {
        super(name);
        this.value = value;
    }

    @Override
    public JsonValue getValue() {
        return value;
    }

    @Override
    public KeyValueAttribute setValue(JsonValue value) {
        this.value = value;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyValueAttribute that = (KeyValueAttribute) o;

        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getName() + ":" + getValue().toJson();
    }

}
