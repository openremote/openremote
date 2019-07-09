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

import org.openremote.model.query.NewAssetQuery;

/**
 * A declarative rule definition a.k.a. JSON rules; consists of:
 * <p>
 * <h2>LHS</li>
 * <p>
 * {@link #when} - A series of grouped {@link RuleCondition}s to which bitwise operations can be performed based on the
 * {@link LogicGroup#operator} of each group. These are used to filter {@link org.openremote.model.rules.AssetState}s
 * in the RuleEngine that this rule is loaded into to determine which {@link org.openremote.model.asset.Asset}s match;
 * if one or more assets match then the RHS {@link #then} will be triggered. Once a rule is triggered the
 * {@link org.openremote.model.rules.AssetState}s that triggered the rule cannot trigger the rule again until it no longer
 * matches (within the context of the {@link RuleCondition} i.e. an {@link org.openremote.model.rules.AssetState} can
 * re-trigger a rule if it is matched by a different {@link RuleCondition})
 * <p>
 * The {@link #otherwise} {@link RuleAction}s are applied to the {@link org.openremote.model.asset.Asset}s filtered by
 * the query that don't match the {@link NewAssetQuery#attributes} predicates.
 * {@link #and} - Optional condition that can be used to further restrict the LHS but without affecting the
 * {@link org.openremote.model.rules.AssetState}s that form the context for the RHS. If this is specified then the
 * {@link #when} must evaluate to true and this condition must evaluate to true.
 * <h2>RHS</h2>
 * <p>
 * {@link #then} - Defines a series of {@link RuleAction}s to perform when the bitwise result of applying all
 * {@link RuleCondition}s in the {@link #when} is true.
 * <p>
 * {@link #otherwise} - Defines a series of {@link RuleAction}s to perform when there is one or more asset that matched  rule doesn't match the assets specified
 * in the {@link Rule#when}. The list of assets this applies to is the assets filtered by applying the
 * {@link Rule#when} but excluding the {@link NewAssetQuery#attributes} predicates and excluding any assets that match
 * the entire {@link Rule#when}. If the number of these assets is greater than 0 then these {@link RuleAction}s will be
 * executed.
 * <h2>Reset</h2>
 * <p>
 * {@link #reset} - Optional logic used to reset the rule, this is applied to each
 * {@link org.openremote.model.rules.AssetState} that was returned by the {@link #when}. If no reset is specified then
 * it is assumed that the rule is fire once only per matched {@link org.openremote.model.rules.AssetState}.
 * <b>NOTE: Rule trigger history is not persisted so on system restart this information is lost and a rule will be
 * able to fire again.</b>
 */
public class Rule {

    public String name;
    public String description;
    public int priority = Integer.MAX_VALUE-1;
    public LogicGroup<RuleCondition> when;
    public RuleAction[] then;
    public RuleAction[] otherwise;
    public RuleConditionReset reset;
    public RuleAction[] onStart;
    public RuleAction[] onStop;
}
