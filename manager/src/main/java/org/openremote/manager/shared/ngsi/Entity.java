package org.openremote.manager.shared.ngsi;

import elemental.json.JsonObject;

import java.util.LinkedHashSet;
import java.util.Set;

public class Entity extends AbstractEntity<Attribute> {

    public Entity(JsonObject jsonObject) {
        super(jsonObject);
    }

    @Override
    public Attribute[] getAttributes() {
        Set<Attribute> attributes = new LinkedHashSet<>();
        String[] keys = jsonObject.keys();
        for (String key : keys) {
            if (hasAttribute(key)) {
                Attribute attribute = new Attribute(key, jsonObject.getObject(key));
                attributes.add(attribute);
            }
        }
        return attributes.toArray(new Attribute[attributes.size()]);
    }

    @Override
    public Attribute getAttribute(String name) {
        return hasAttribute(name) ? new Attribute(name, jsonObject.getObject(name)) : null;
    }

    @Override
    public Entity addAttribute(Attribute attribute) {
        jsonObject.put(attribute.getName(), attribute.getJsonObject());
        return this;
    }

    @Override
    public Entity removeAttribute(String name) {
        if (hasAttribute(name)) {
            jsonObject.remove(name);
        }
        return this;
    }

    @Override
    protected void validateAttributes(Set<ModelValidationError> errors) {
        for (Attribute attribute : getAttributes()) {
            ModelProblem[] problems = Model.validateField(attribute.getName());
            for (ModelProblem problem : problems) {
                errors.add(new ModelValidationError("attributeName", problem));
            }
        }
    }
}
