/*
 * Copyright 2017, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.rules;

import java.util.stream.Stream;

import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.TsIgnore;

/**
 * Facade for writing rules RHS actions, supporting asset queries within the scope of the rule
 * engine, and dispatching of {@link AttributeEvent} as rule consequence.
 */
@TsIgnore
public abstract class Assets {

  public abstract Stream<Asset<?>> getResults(AssetQuery assetQuery);

  public abstract Assets dispatch(AttributeEvent... event);

  public abstract Assets dispatch(String assetId, String attributeName, Object value);

  public abstract Assets dispatch(String assetId, String attributeName);
}
