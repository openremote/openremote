package org.openremote.manager.shared.model.ngsi;

import elemental.json.JsonObject;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractEntity<A extends AbstractAttribute> {

    final protected JsonObject jsonObject;

    public AbstractEntity(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public String getId() {
        return jsonObject.hasKey("id") ? jsonObject.getString("id") : null;
    }

    public void setId(String id) {
        jsonObject.put("id", id);
    }

    public String getType() {
        return jsonObject.hasKey("type") ? jsonObject.getString("type") : null;
    }

    public void setType(String type) {
        jsonObject.put("type", type);
    }

    public boolean hasAttribute(String name) {
        return !name.equals("id") && !name.equals("type") && jsonObject.hasKey(name);
    }

    public ModelValidationError[] validate() {
        return validate(true, true);
    }

    public ModelValidationError[] validate(boolean validateId, boolean validateType) {
        Set<ModelValidationError> errors = new LinkedHashSet<>();
        if (validateId)
            validateId(errors);
        if (validateType)
            validateType(errors);
        validateAttributes(errors);
        return errors.toArray(new ModelValidationError[errors.size()]);
    }

    public abstract A[] getAttributes();

    public abstract A getAttribute(String name);

    protected abstract void validateAttributes(Set<ModelValidationError> errors);

    protected void validateId(Set<ModelValidationError> errors) {
        ModelProblem[] problems = Model.validateField(getId());
        for (ModelProblem problem : problems) {
            errors.add(new ModelValidationError("id", problem));
        }
    }

    protected void validateType(Set<ModelValidationError> errors) {
        ModelProblem[] problems = Model.validateField(getType());
        for (ModelProblem problem : problems) {
            errors.add(new ModelValidationError("type", problem));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractEntity that = (AbstractEntity) o;

        return jsonObject.equals(that.jsonObject);

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
