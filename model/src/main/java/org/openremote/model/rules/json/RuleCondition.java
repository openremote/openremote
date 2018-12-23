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
package org.openremote.model.rules.json;

public class RuleCondition<T> {

    /**
     * If not specified then {@link RuleOperator#AND} is assumed.
     */
    public RuleOperator operator;

    /**
     * Defines the predicates to apply using the {@link #operator}
     */
    public T[] predicates;

    /**
     * Nested conditions allow for more complex logic with a mix of operators
     */
    public RuleCondition<T>[] conditions;
}
