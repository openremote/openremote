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
package org.openremote.manager.rules;

import org.jeasy.rules.api.Rule;
import org.jeasy.rules.core.RuleBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.openremote.manager.rules.RulesEngine.RULES_FIRED_LOG;

/**
 * Call {@link #add()} to add rules.
 */
public class RulesBuilder {

    @FunctionalInterface
    public interface Condition {
        Object evaluate(RulesFacts facts);
    }

    public interface Action {
        void execute(RulesFacts facts);
    }

    public static class Builder {
        protected String name = org.jeasy.rules.api.Rule.DEFAULT_NAME;
        protected String description = org.jeasy.rules.api.Rule.DEFAULT_DESCRIPTION;
        protected int priority = org.jeasy.rules.api.Rule.DEFAULT_PRIORITY;
        protected Condition condition = facts -> false;
        protected Action action = facts -> {
        };

        /**
         * Required (short) rule name.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Optional longer description.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Lower priority rules execute before higher priority rules.
         * Default priority is <code>Integer.MAX_VALUE - 1</code>.
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder when(Condition condition) {
            this.condition = condition;
            return this;
        }

        public Builder then(Action action) {
            this.action = action;
            return this;
        }
    }

    final protected List<Builder> builders = new ArrayList<>();

    public Builder add() {
        Builder builder = new Builder();
        builders.add(builder);
        return builder;
    }

    public Rule[] build() {
        List<Rule> rules = new ArrayList<>();
        for (Builder builder : builders) {
            Rule rule = new RuleBuilder()
                .name(builder.name)
                .description(builder.description)
                .priority(builder.priority)
                .when(facts -> {
                    Object result;
                    try {
                        result = builder.condition.evaluate((RulesFacts) facts);
                    } catch (Exception ex) {
                        throw new RuntimeException("Error evaluating condition of rule '" + builder.name + "': " + ex.getMessage(), ex);
                    }
                    if (result instanceof Boolean) {
                        return (boolean)result;
                    } else {
                        throw new IllegalArgumentException("Error evaluating condition of rule '" + builder.name + "': result is not boolean but " + result);
                    }
                })
                .then(facts -> builder.action.execute((RulesFacts) facts))
                .build();

            rules.add(rule);
        }
        return rules.toArray(new Rule[rules.size()]);
    }
}
