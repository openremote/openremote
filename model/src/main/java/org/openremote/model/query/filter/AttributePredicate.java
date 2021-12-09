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

import org.openremote.model.attribute.Attribute;
import org.openremote.model.query.LogicGroup;
import org.openremote.model.value.NameHolder;

import java.util.Arrays;

/**
 * Adds additional predicate logic to {@link NameValuePredicate}, allowing predicating on {@link Attribute#getMeta}
 * presence/absence and/or values; there is an implicit OR between meta predicates (and condition can be achieved by
 * creating multiple {@link AttributePredicate}s in an {@link LogicGroup.Operator#AND} {@link LogicGroup}. Can also
 * predicate on the previous value of the {@link Attribute} which is only relevant when applied to {@link
 * org.openremote.model.rules.AssetState}.
 */
public class AttributePredicate extends NameValuePredicate {

    public NameValuePredicate[] meta;
    public ValuePredicate previousValue;

    public AttributePredicate() {
    }

    public AttributePredicate(NameHolder nameHolder, ValuePredicate value) {
        super(nameHolder, value);
    }

    public AttributePredicate(String name, ValuePredicate value) {
        super(name, value);
    }

    public AttributePredicate(StringPredicate name, ValuePredicate value) {
        super(name, value);
    }

    public AttributePredicate(NameHolder nameHolder, ValuePredicate value, boolean negated, Path path) {
        super(nameHolder, value, negated, path);
    }

    public AttributePredicate(String name, ValuePredicate value, boolean negated, Path path) {
        super(name, value, negated, path);
    }

    public AttributePredicate(StringPredicate name, ValuePredicate value, boolean negated, Path path) {
        super(name, value, negated, path);
    }

    @Override
    public AttributePredicate name(StringPredicate name) {
        this.name = name;
        return this;
    }

    @Override
    public AttributePredicate name(String name) {
        this.name = new StringPredicate(name);
        return this;
    }

    @Override
    public AttributePredicate name(NameHolder name) {
        this.name = new StringPredicate(name);
        return this;
    }

    @Override
    public AttributePredicate value(ValuePredicate value) {
        this.value = value;
        return this;
    }

    @Override
    public AttributePredicate negate() {
        super.negate();
        return this;
    }

    @Override
    public AttributePredicate path(Path path) {
        super.path(path);
        return this;
    }

    public AttributePredicate previousValue(ValuePredicate previousValue) {
        this.previousValue = previousValue;
        return this;
    }

    public AttributePredicate meta(NameValuePredicate... meta) {
        this.meta = meta;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "name=" + name +
            ", value=" + value +
            ", negated=" + negated +
            ", path=" + (path == null ? "null" : Arrays.toString(path.paths)) +
            ", meta=" + Arrays.toString(meta) +
            ", previousValue=" + previousValue +
            '}';
    }
}
