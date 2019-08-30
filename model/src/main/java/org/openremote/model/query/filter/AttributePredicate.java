/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.query.filter;

import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

// TODO: Incorporate meta predicates here
public class AttributePredicate {

    public StringPredicate name;
    public boolean exists;
    public ValuePredicate value;

    public AttributePredicate() {
    }

    public AttributePredicate(String name) {
        this(new StringPredicate(name));
    }

    public AttributePredicate(StringPredicate name) {
        this.name = name;
    }

    public AttributePredicate(ValuePredicate value) {
        this.value = value;
    }

    public AttributePredicate(StringPredicate name, ValuePredicate value) {
        this.name = name;
        this.value = value;
    }

    public AttributePredicate name(StringPredicate name) {
        this.name = name;
        return this;
    }

    public AttributePredicate value(ValuePredicate value) {
        this.value = value;
        return this;
    }

    public ObjectValue toModelValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("name", name.toModelValue());
        objectValue.put("value", value.toModelValue());
        return objectValue;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "name=" + name +
            ", value=" + value +
            '}';
    }
}
