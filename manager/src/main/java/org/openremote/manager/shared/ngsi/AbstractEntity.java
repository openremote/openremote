/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.shared.ngsi;

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

    public AbstractEntity<A> setId(String id) {
        if (id == null)
            jsonObject.remove("id");
        else
            jsonObject.put("id", id);
        return this;
    }

    public String getType() {
        return jsonObject.hasKey("type") ? jsonObject.getString("type") : null;
    }

    public AbstractEntity<A> setType(String type) {
        if (type == null)
            jsonObject.remove("type");
        else
            jsonObject.put("type", type);
        return this;
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

    public abstract AbstractEntity<A> addAttribute(A attribute);

    public abstract AbstractEntity<A> removeAttribute(String name);

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
