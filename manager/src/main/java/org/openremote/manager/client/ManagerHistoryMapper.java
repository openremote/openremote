/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.client;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.flows.FlowsPlace;
import org.openremote.manager.client.map.MapPlace;

@WithTokenizers(
    {
        AssetsPlace.Tokenizer.class,
        MapPlace.Tokenizer.class,
        FlowsPlace.Tokenizer.class
    }
)
public interface ManagerHistoryMapper extends PlaceHistoryMapper {
}
