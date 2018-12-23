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
 * There are two types of trigger supported and only one should be specified if both are specified then {@link #asset}
 * trigger will be used.
 * <ul>
 * <li>{@link #asset} see {@link AssetPredicate}</li>
 * <li>{@link #timer} Allows timer expressions to be specified and thus creating a time based rule (e.g. "1h")</li>
 * </ul>
 */
public class RuleTrigger {

    public AssetPredicate asset;
    public String timer;
}
