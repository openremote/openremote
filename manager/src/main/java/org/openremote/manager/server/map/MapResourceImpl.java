package org.openremote.manager.server.map;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.rpc.RequestData;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.rpc.TestRequestData;

import javax.ws.rs.WebApplicationException;
import java.util.logging.Logger;

public class MapResourceImpl extends WebResource implements MapResource {

    private static final Logger LOG = Logger.getLogger(MapResourceImpl.class.getName());

    protected final MapService mapService;

    public MapResourceImpl(MapService mapService) {
        this.mapService = mapService;
    }

    @Override
    public JsonObject getSettings(RequestData requestData) {
        String tileUrl = uriInfo.getBaseUriBuilder().clone()
            .replacePath(getRealm()).path("map/tile").build().toString() + "/{z}/{x}/{y}";
        return mapService.getMapSettings(tileUrl);
    }

    // TODO: Remove this - only for demo purposes
    @Override
    public JsonArray getTest(TestRequestData params) {
        JsonArray jsonArray = Json.createArray();
        jsonArray.set(0, params.zoom);
        jsonArray.set(1, params.row);
        jsonArray.set(2, params.column);
        return jsonArray;
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
