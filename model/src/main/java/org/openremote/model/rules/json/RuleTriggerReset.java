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
 * This defines when an {@link org.openremote.model.rules.AssetState} becomes eligible for triggering the rule again once
 * if has triggered a rule. If a reset is not specified then {@link #noLongerMatches} is assumed.
 * <p>
 * There is an implicit OR condition between each option.
 */
public class RuleTriggerReset {

    /**
     * Never reset i.e. fire once only for a given {@link org.openremote.model.rules.AssetState}.
     */
    public boolean never;

    /**
     * Simple timer expression e.g. '1h' that is started when
     */
    public String timer;

    /**
     * When the {@link org.openremote.model.rules.AssetState} no longer evaluates to true.
     */
    public boolean noLongerMatches;

    /**
     * When the timestamp of the {@link org.openremote.model.rules.AssetState} changes in comparison to the timestamp
     * at the time when it matched.
     */
    public boolean timestampChanges;

    /**
     * When the value of the {@link org.openremote.model.rules.AssetState} changes in comparison to the value at the
     * time when it matched.
     */
    public boolean valueChanges;
}
