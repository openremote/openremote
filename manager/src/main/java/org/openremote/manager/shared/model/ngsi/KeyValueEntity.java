package org.openremote.manager.shared.model.ngsi;

import elemental.json.JsonObject;

import java.util.LinkedHashSet;
import java.util.Set;

public class KeyValueEntity extends AbstractEntity<KeyValueAttribute> {

    public KeyValueEntity(JsonObject jsonObject) {
        super(jsonObject);
    }

    @Override
    public KeyValueAttribute[] getAttributes() {
        Set<KeyValueAttribute> attributes = new LinkedHashSet<>();

        String[] keys = jsonObject.keys();
        for (String key : keys) {
            if (key.equals("id") || key.equals("type"))
                continue;
            KeyValueAttribute attribute = new KeyValueAttribute(key, jsonObject.get(key));
            attributes.add(attribute);
        }

        return attributes.toArray(new KeyValueAttribute[attributes.size()]);
    }

    @Override
    public KeyValueAttribute getAttribute(String name) {
        return hasAttribute(name) ? new KeyValueAttribute(name, jsonObject.get(name)) : null;
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
