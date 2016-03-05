package org.openremote.manager.server.map;

import elemental.json.JsonObject;
import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.rest.MapRestService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

public class MapResource extends WebResource implements MapRestService {

    private static final Logger LOG = Logger.getLogger(MapResource.class.getName());

    protected final MapService mapService;

    public MapResource(MapService mapService) {
        this.mapService = mapService;
    }

    @RolesAllowed({"admin", "map:read"})
    public JsonObject getOptions() {
        String tileUrl = uriInfo.getBaseUriBuilder().clone()
            .replacePath(getRealm()).path("map/tile").build().toString() + "/{z}/{x}/{y}";
        return mapService.getMapSettings(tileUrl);
    }

    public Response getTile(@PathParam("zoom") int zoom, @PathParam("column") int column, @PathParam("row") int row) {
        try {
            byte[] tile = mapService.getMapTile(zoom, column, row);
            if (tile != null) {
                return Response.ok().encoding("gzipped").entity(tile).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
}
