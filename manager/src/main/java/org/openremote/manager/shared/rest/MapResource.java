package org.openremote.manager.shared.rest;

import elemental.json.JsonObject;
import jsinterop.annotations.JsType;
import org.jboss.resteasy.annotations.Form;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static jsinterop.annotations.JsPackage.GLOBAL;

@Path("map")
@JsType(isNative = true, namespace = GLOBAL, name = "MapResource")
public interface MapResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "map:read"})
    JsonObject getSettings(@Form ClientInvocation clientInvocation);

    @GET
    @Produces("application/vnd.mapbox-vector-tile")
    @Path("tile/{zoom}/{column}/{row}")
    byte[] getTile(@PathParam("zoom")int zoom, @PathParam("column")int column, @PathParam("row")int row) throws Exception;
}
