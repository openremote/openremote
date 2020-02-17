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

/**
 * Controls if/when a rule can be eligible for firing again;
 */
public class RuleRecurrence {

    /**
     * Defines the scope of the recurrence
     */
    public enum Scope {

        /**
         * Recurrence applies to each asset individually (i.e. timer starts from when rule triggered, each asset that
         * triggered the rule will have their own timer based on {@link #mins} value)
         */
        PER_ASSET,

        /**
         * Recurrence applies globally (i.e. timer starts from when rule triggered, there is a single timer based on
         * {@link #mins} value, this rule will not fire again until timer expires)
         */
        GLOBAL
    }

    /**
     * If not defined defaults to {@link Scope#PER_ASSET}
     */
    public Scope scope;

    /**
     * How many mins until firing is allowed to recur; null=NEVER, 0=ALWAYS, >0=N mins from last firing.
     */
    public Long mins;
}