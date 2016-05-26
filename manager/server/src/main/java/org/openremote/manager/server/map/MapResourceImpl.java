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
package org.openremote.manager.server.map;

import elemental.json.JsonObject;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.map.MapResource;

import javax.ws.rs.WebApplicationException;
import java.util.logging.Logger;

public class MapResourceImpl extends WebResource implements MapResource {

    private static final Logger LOG = Logger.getLogger(MapResourceImpl.class.getName());

    protected final MapService mapService;

    public MapResourceImpl(MapService mapService) {
        this.mapService = mapService;
    }

    @Override
    public JsonObject getSettings(RequestParams requestParams) {
        String tileUrl = uriInfo.getBaseUriBuilder().clone()
            .replacePath(getRealm()).path("map/tile").build().toString() + "/{z}/{x}/{y}";
        return mapService.getMapSettings(tileUrl);
    }

    @Override
    public byte[] getTile(int zoom, int column, int row) {
        try {
            byte[] tile = mapService.getMapTile(zoom, column, row);
            if (tile != null) {
                return tile;
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
}
