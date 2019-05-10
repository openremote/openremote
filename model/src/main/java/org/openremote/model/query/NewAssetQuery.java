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

import org.openremote.model.query.filter.*;
import org.openremote.model.rules.json.RuleActionTarget;
import org.openremote.model.rules.json.RuleCondition;

/**
 * <p>
 * This predicate is designed to filter the {@link org.openremote.model.rules.AssetState}s and will evaluate to true
 * if after applying the filtering one or more {@link org.openremote.model.rules.AssetState}s are returned.
 * <p>
 * The {@link org.openremote.model.asset.Asset}s associated with the returned {@link org.openremote.model.rules.AssetState}s
 * are then used as the context for any {@link org.openremote.model.rules.json.RuleTriggerReset} to control when the
 * {@link NewAssetQuery} is allowed to be applied again to the same asset. They can also optionally be used as context
 * in the {@link RuleActionTarget}, to apply an action to the matched
 * {@link org.openremote.model.asset.Asset}s.
 * <p>
 * There is an implicit AND between properties and an implicit OR between each value of a property.
 */
// TODO: Merge with existing AssetQuery
// TODO: types and attributes are linked and shouldn't be independently specified
public class NewAssetQuery {

    public String[] ids;
    public StringPredicate[] names;
    public ParentPredicate[] parents;
    public PathPredicate[] paths;
    public TenantPredicate tenant;
    public String[] userIds;
    public StringPredicate[] types;
    public RuleCondition<AttributePredicate> attributes;

    /**
     * To be used in combination with {@link #limit} to control the ordering of {@link org.openremote.model.asset.Asset}s
     * before applying the limit.
     */
    public BaseAssetQuery.OrderBy orderBy;

    /**
     * Means that number of matching {@link org.openremote.model.asset.Asset}s will be limited to this amount.
     */
    public int limit;
}
