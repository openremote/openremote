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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

public class LogicGroup<T> {

    public enum Operator {
        AND,
        OR
    }

    public LogicGroup() {
    }

    public LogicGroup(T[] items) {
        this.items = items;
    }

    public LogicGroup(Operator operator, T[] items, LogicGroup<T>[] groups) {
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
    @JsonIgnore
    public T[] items;

    /**
     * Nested groups allow for more complex logic with a mix of operators
     */
    @JsonIgnore
    public LogicGroup<T>[] groups;

    @JsonProperty("items")
    public T[] getItems() {
        return items;
    }

    @JsonProperty("items")
    public void setItems(List<T> items) {
        this.items = items == null ? null : items.toArray((T[])new Object[0]);
    }

    @JsonProperty("groups")
    public LogicGroup<T>[] getGroups() {
        return groups;
    }

    @JsonProperty("groups")
    public void setGroups(List<LogicGroup<T>> groups) {
        this.groups = groups == null ? null : groups.toArray((LogicGroup<T>[])new Object[0]);
    }

    @Override
    public String toString() {
        return LogicGroup.class.getSimpleName() + "{" +
            "operator=" + operator +
            ", items=" + Arrays.toString(items) +
            ", groups=" + Arrays.toString(groups) +
            '}';
    }
}
