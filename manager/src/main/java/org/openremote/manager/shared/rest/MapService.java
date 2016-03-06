package org.openremote.manager.shared.rest;

import elemental.json.JsonObject;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("map")
@JsType(isNative = true)
public interface MapService {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    JsonObject getOptions();

    @GET
    @Produces("application/vnd.mapbox-vector-tile")
    @Path("tile/{zoom}/{column}/{row}")
    Response getTile(@PathParam("zoom")int zoom, @PathParam("column")int column, @PathParam("row")int row) throws Exception;
}
