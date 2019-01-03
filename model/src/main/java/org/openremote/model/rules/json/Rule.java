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

import org.openremote.model.rules.json.predicate.AssetPredicate;

/**
 * A declarative rule definition (a.k.a. JSON rules); consists of:
 * <p>
 * <h2>LHS</li>
 * <p>
 * {@link #when}* - This can be either a {@link RuleTrigger#timer} for time based rules or an {@link AssetPredicate}
 * which will be used to filter {@link org.openremote.model.rules.AssetState}s currently available in the rule engine
 * that this rule is loaded into. The {@link AssetPredicate} will evaluate to true when one or more
 * {@link org.openremote.model.rules.AssetState}s are returned by applying the filter. The returned
 * {@link org.openremote.model.rules.AssetState}s are then used as context for the RHS and also the
 * {@link RuleTriggerReset} (if specified).
 * <p>
 * {@link #and} - Optional condition that can be used to further restrict the LHS but without affecting the
 * {@link org.openremote.model.rules.AssetState}s that form the context for the RHS. If this is specified then the
 * {@link #when} must evaluate to true and this condition must evaluate to true.
 * <h2>RHS</h2>
 * <p>
 * {@link #then}* - Defines a series of {@link RuleAction}s to perform when the rule triggers.
 * <h2>Reset</h2>
 * <p>
 * {@link #reset}* - Optional logic used to reset the rule, this is applied to each
 * {@link org.openremote.model.rules.AssetState} that was returned by the {@link #when}. If no reset is specified then
 * it is assumed that the rule is fire once only per matched {@link org.openremote.model.rules.AssetState}.
 * <b>NOTE: Rule trigger history is not persisted so on system restart this information is lost and a rule will be
 * able to fire again.</b>
 * <p>
 * <b>* = Required</b>
 */
public class Rule {

    public String name;
    public String description;
    public int priority = Integer.MAX_VALUE-1;
    public RuleTrigger when;
    public RuleCondition<AssetPredicate> and;
    public RuleAction[] then;
    public RuleTriggerReset reset;
}
