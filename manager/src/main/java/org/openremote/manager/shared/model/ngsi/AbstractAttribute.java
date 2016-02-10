package org.openremote.manager.shared.model.ngsi;

import elemental.json.JsonValue;

public abstract class AbstractAttribute {

    final protected String name;

    public AbstractAttribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract JsonValue getValue();

    public abstract AbstractAttribute setValue(JsonValue value);
}
