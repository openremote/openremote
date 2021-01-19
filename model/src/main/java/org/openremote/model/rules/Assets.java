/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.rules;

import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.TsIgnore;

import java.util.stream.Stream;

/**
 * Facade for writing rules RHS actions, supporting asset queries within the scope
 * of the rule engine, and dispatching of {@link AttributeEvent} as rule consequence.
 */
@TsIgnore
public abstract class Assets {

    abstract public Stream<Asset<?>> getResults(AssetQuery assetQuery);

    abstract public Assets dispatch(AttributeEvent... event);

    abstract public Assets dispatch(String assetId, String attributeName, Object value);

    abstract public Assets dispatch(String assetId, String attributeName);
}
