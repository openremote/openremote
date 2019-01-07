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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RuleCondition<T> {

    public RuleCondition() {
    }

    public RuleCondition(RuleOperator operator, T[] predicates, RuleCondition<T>[] conditions) {
        this.operator = operator;
        this.predicates = predicates;
        this.conditions = conditions;
    }

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

    public static <T> List<T> flatten(List<RuleCondition<T>> conditions) {
        List<T> flattened = new ArrayList<>();
        for (RuleCondition<T> ruleCondition : conditions) {
            flattened.addAll(Arrays.asList(ruleCondition.predicates));
            if (ruleCondition.conditions != null) {
                flattened.addAll(flatten(Arrays.asList(ruleCondition.conditions)));
            }
        }
        return flattened;
    }
}
