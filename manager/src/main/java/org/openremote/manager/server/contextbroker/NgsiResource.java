package org.openremote.manager.server.contextbroker;

import org.openremote.manager.shared.http.PATCH;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.EntryPoint;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("v2")
public interface NgsiResource {

    @GET
    @Produces(APPLICATION_JSON)
    EntryPoint getEntryPoint();

    @GET
    @Path("entities")
    @Produces(APPLICATION_JSON)
    Entity[] getEntities();

    @POST
    @Path("entities")
    @Consumes(APPLICATION_JSON)
    Response postEntity(Entity entity);

    @GET
    @Path("entities/{entityId}")
    @Produces(APPLICATION_JSON)
    Entity getEntity(@PathParam("entityId") String entityId);

    @DELETE
    @Path("entities/{entityId}")
    Response deleteEntity(@PathParam("entityId") String entityId);

    @PUT
    @Path("entities/{entityId}")
    @Consumes(APPLICATION_JSON)
    Response putEntity(@PathParam("entityId") String entityId, Entity entity);

    @PATCH
    @Path("entities/{entityId}")
    @Consumes(APPLICATION_JSON)
    Response patchEntity(@PathParam("entityId") String entityId, Entity entity);

}
