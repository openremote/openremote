package org.openremote.manager.server.map;

import elemental.json.JsonObject;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.ClientInvocation;
import org.openremote.manager.shared.map.MapResource;

import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Logger;

public class MapResourceImpl extends WebResource implements MapResource {

    private static final Logger LOG = Logger.getLogger(MapResourceImpl.class.getName());

    protected final MapService mapService;

    public MapResourceImpl(MapService mapService) {
        this.mapService = mapService;
    }

    @Override
    public JsonObject getSettings(ClientInvocation clientInvocation) {
        String tileUrl = uriInfo.getBaseUriBuilder().clone()
            .replacePath(getRealm()).path("map/tile").build().toString() + "/{z}/{x}/{y}";
        return mapService.getMapSettings(tileUrl);
    }

    @Override
    public byte[] getTile(@PathParam("zoom") int zoom, @PathParam("column") int column, @PathParam("row") int row) {
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
