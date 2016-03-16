package org.openremote.manager.server.assets;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.openremote.manager.shared.http.PATCH;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.EntryPoint;
import org.openremote.manager.shared.ngsi.params.EntityListParams;
import org.openremote.manager.shared.ngsi.params.EntityParams;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * http://telefonicaid.github.io/fiware-orion/api/v2/
 */
@Path("v2")
public interface ContextBrokerResource {

    @GET
    @Produces(APPLICATION_JSON)
    EntryPoint getEntryPoint();

    @GET
    @Path("entities")
    @Produces(APPLICATION_JSON)
    JsonArray getEntities(@BeanParam EntityListParams entityListParams);

    @POST
    @Path("entities")
    @Consumes(APPLICATION_JSON)
    Response postEntity(Entity entity);

    @GET
    @Path("entities/{entityId}")
    @Produces(APPLICATION_JSON)
    JsonObject getEntity(@PathParam("entityId") String entityId, @BeanParam EntityParams entityParams);

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
