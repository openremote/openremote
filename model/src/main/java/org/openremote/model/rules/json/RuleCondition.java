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

import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.DateTimePredicate;

/**
 * Consists of a timer expression (interval, absolute, CRON etc.) or query for filtering
 * {@link org.openremote.model.rules.AssetState}s, if both are specified then the timer is used.
 * <p>
 * For timer based trigger it evaluates to true if the timer expression matches the current time. For query based trigger
 * it evaluates to true when one or more {@link org.openremote.model.rules.AssetState}s (referencing unique
 * {@link org.openremote.model.asset.Asset}s) are returned after applying the query to the
 * {@link org.openremote.model.rules.AssetState}s in the RuleEngine that this rule is loaded into.
 * <p>
 * The {@link #tag} is used to name the {@link org.openremote.model.asset.Asset}s that are filtered by the query and can
 * be used in the rule RHS to perform actions on these specific assets.
 */
public class RuleCondition {

    public String timer;
    public DateTimePredicate datetime;
    public AssetQuery assets;
    public String tag;
    public RuleConditionReset reset;
}
