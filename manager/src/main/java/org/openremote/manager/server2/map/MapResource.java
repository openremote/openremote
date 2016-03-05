package org.openremote.manager.server2.map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.container.web.WebResource;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("map")
public class MapResource extends WebResource {

    private static final Logger LOG = Logger.getLogger(MapResource.class.getName());

    protected final MapService mapService;

    public MapResource(MapService mapService) {
        this.mapService = mapService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "map:read"})
    public ObjectNode getMapSettings() {
        String tileUrl = uriInfo.getBaseUriBuilder().clone()
            .replacePath(getRealm()).path("map/tile").build().toString() + "/{z}/{x}/{y}";
        return mapService.getMapSettings(tileUrl);
    }

    @GET
    @Path("/tile/{zoom}/{column}/{row}")
    @Produces("application/vnd.mapbox-vector-tile")
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
