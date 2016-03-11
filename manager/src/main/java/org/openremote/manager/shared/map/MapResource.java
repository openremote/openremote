package org.openremote.manager.shared.map;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import jsinterop.annotations.JsType;
import org.jboss.resteasy.annotations.Form;
import org.openremote.manager.shared.rpc.RequestData;
import org.openremote.manager.shared.rpc.TestRequestData;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("map")
@JsType(isNative = true)
public interface MapResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "map:read"})
    JsonObject getSettings(@Form RequestData requestData);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "map:read"})
    @Path("test/{zoom}/{column}/{row}")
    JsonArray getTest(@Form TestRequestData restParams);

    @GET
    @Produces("application/vnd.mapbox-vector-tile")
    @Path("tile/{zoom}/{column}/{row}")
    byte[] getTile(@PathParam("zoom")int zoom, @PathParam("column")int column, @PathParam("row")int row) throws Exception;
}
