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
import org.openremote.model.rules.SunPositionTrigger;

/**
 * Consists of one of the following triggers in order of precedence:
 * <ol>
 * <li>{@link #duration} - ISO8601 duration expression (e.g. 'PT1H') after which the condition becomes true - 1min precision</li>
 * <li>{@link #cron} - CRON expression (e.g. "*&#47;5 * * * *") in UTC at which time the condition becomes true - 1min precision</li>
 * <li>{@link #sun} - {@link SunPositionTrigger} at which time the condition becomes true - 1min precision</li>
 * <li>{@link #assets} - {@link AssetQuery} to be applied to the {@link org.openremote.model.rules.AssetState}s
 * available within the rule engine this rule is loaded into. Evaluates to true when one or more
 * {@link org.openremote.model.rules.AssetState}s (referencing unique {@link org.openremote.model.asset.Asset}s) are
 * returned after applying the query.</li>
 * </ol>
 * <p>
 * The {@link #tag} is used to name the {@link org.openremote.model.asset.Asset}s that are filtered by the query and can
 * be used in the rule RHS to perform actions on these specific assets.
 */
public class RuleCondition {

    /**
     * ISO8601 duration expression (e.g. PT1H)
     */
    public String duration;

    /**
     * CRON expression in UTC (e.g. *&#47;5 * * * *)
     */
    public String cron;

    /**
     * {@link SunPositionTrigger}
     */
    public SunPositionTrigger sun;

    /**
     * {@link AssetQuery}
     */
    public AssetQuery assets;

    /**
     * Can be used to reference the {@link #assets} that cause this condition to trigger
     */
    public String tag;

    public boolean hasTimeTrigger() {
        return duration != null || cron != null || sun != null;
    }
}
