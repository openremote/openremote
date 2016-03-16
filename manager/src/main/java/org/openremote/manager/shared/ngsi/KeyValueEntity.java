package org.openremote.manager.shared.ngsi;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

import java.util.LinkedHashSet;
import java.util.Set;

public class KeyValueEntity extends AbstractEntity<KeyValueAttribute> {

    public static KeyValueEntity[] from(JsonArray jsonArray) {
        KeyValueEntity[] array = new KeyValueEntity[jsonArray.length()];
        for (int i = 0; i < array.length; i++) {
            array[i] = new KeyValueEntity(jsonArray.get(i));
        }
        return array;
    }

    public static KeyValueEntity from(JsonObject jsonObject) {
        return new KeyValueEntity(jsonObject);
    }

    public KeyValueEntity(JsonObject jsonObject) {
        super(jsonObject);
    }

    @Override
    public KeyValueAttribute[] getAttributes() {
        Set<KeyValueAttribute> attributes = new LinkedHashSet<>();
        String[] keys = jsonObject.keys();
        for (String key : keys) {
            if (hasAttribute(key)) {
                KeyValueAttribute attribute = new KeyValueAttribute(key, jsonObject.get(key));
                attributes.add(attribute);
            }
        }
        return attributes.toArray(new KeyValueAttribute[attributes.size()]);
    }

    @Override
    public KeyValueAttribute getAttribute(String name) {
        return hasAttribute(name) ? new KeyValueAttribute(name, jsonObject.get(name)) : null;
    }

    @Override
    public KeyValueEntity addAttribute(KeyValueAttribute attribute) {
        jsonObject.put(attribute.getName(), attribute.getValue());
        return this;
    }

    @Override
    public KeyValueEntity removeAttribute(String name) {
        if (hasAttribute(name)) {
            jsonObject.remove(name);
        }
        return this;
    }

    @Override
    protected void validateAttributes(Set<ModelValidationError> errors) {
        for (KeyValueAttribute attribute : getAttributes()) {
            ModelProblem[] problems = Model.validateField(attribute.getName());
            for (ModelProblem problem : problems) {
                errors.add(new ModelValidationError("attributeName", problem));
            }
        }
    }

}
