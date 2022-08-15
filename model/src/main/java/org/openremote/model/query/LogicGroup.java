/*
 * Copyright 2018, OpenRemote Inc.
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
package org.openremote.model.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogicGroup<T> {

    public enum Operator {
        AND,
        OR
    }

    public LogicGroup() {
    }

    @SafeVarargs
    public LogicGroup(T... items) {
        this(Arrays.asList(items));
    }

    public LogicGroup(List<T> items) {
        this.items = items;
    }

    @SafeVarargs
    public LogicGroup(Operator operator, T... items) {
        this(operator, Arrays.asList(items));
    }

    public LogicGroup(Operator operator, List<T> items) {
        this(operator, null, items);
    }

    @SafeVarargs
    public LogicGroup(Operator operator, List<LogicGroup<T>> groups, T... items) {
        this(operator, groups, Arrays.asList(items));
    }

    public LogicGroup(Operator operator, List<LogicGroup<T>> groups, List<T> items) {
        this.operator = operator;
        this.items = items;
        this.groups = groups;
    }

    /**
     * If not specified then {@link Operator#AND} is assumed.
     */
    public Operator operator;

    /**
     * Defines the items that the {@link #operator} should be applied to
     */
    public List<T> items;

    /**
     * Nested groups allow for more complex logic with a mix of operators
     */
    public List<LogicGroup<T>> groups;


    public List<T> getItems() {
        return items == null ? new ArrayList<>() : items;
    }

    @Override
    public String toString() {
        return LogicGroup.class.getSimpleName() + "{" +
            "operator=" + operator +
            ", items=" + (items == null ? "[]" : Arrays.toString(items.toArray())) +
            ", groups=" + (groups == null ? "[]" : Arrays.toString(groups.toArray())) +
            '}';
    }

    public static <S> List<S> getItemsRecursive(LogicGroup<S> group) {
        List<S> items = new ArrayList<>();
        if (group.items != null) {
            items.addAll(group.items);
        }
        if (group.groups != null) {
            group.groups.forEach(g -> items.addAll(getItemsRecursive(g)));
        }
        return items;
    }
}
