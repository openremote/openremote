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
package org.openremote.model.rules.json.predicate;

import org.openremote.model.query.filter.ParentPredicate;
import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.rules.json.RuleCondition;

/**
 * <p>
 * This predicate is designed to filter the {@link org.openremote.model.rules.AssetState}s and will evaluate to true
 * if after applying the filtering one or more {@link org.openremote.model.rules.AssetState}s are returned.
 * <p>
 * The {@link org.openremote.model.asset.Asset}s associated with the returned {@link org.openremote.model.rules.AssetState}s
 * are then used as the context for any {@link org.openremote.model.rules.json.RuleTriggerReset} to control when the
 * {@link AssetPredicate} is allowed to be applied again to the same asset. They can also optionally be used as context
 * in the {@link org.openremote.model.rules.json.RuleActionWithTarget.Target}, to apply an action to the matched
 * {@link org.openremote.model.asset.Asset}s.
 * <p>
 * There is an implicit AND between properties and an implicit OR between each value of a property.
 */
public class AssetPredicate {

    public enum MatchOrder {
        NAME,
        NAME_REVERSED,
        ATTRIBUTE,
        ATTRIBUTE_REVERSE,
        NAME_AND_ATTRIBUTE,
        NAME_AND_ATTRIBUTE_REVERSED,
        ATTRIBUTE_AND_NAME,
        ATTRIBUTE_AND_NAME_REVERSED
    }

    public String[] ids;
    public StringPredicate[] names;
    public ParentPredicate[] parents;
    public PathPredicate[] paths;
    public TenantPredicate tenant;
    public String[] userIds;
    public StringPredicate[] types;
    public RuleCondition<AttributePredicate> attributes;

    /**
     * To be used in combination with {@link #matchLimit} to control the ordering of {@link org.openremote.model.rules.AssetState}s
     * before applying the limit.
     */
    public MatchOrder matchOrder;

    /**
     * Means that only this number of matching {@link org.openremote.model.rules.AssetState} will be passed through as the context.
     */
    public int matchLimit;
}
