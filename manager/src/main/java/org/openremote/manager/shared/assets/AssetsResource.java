package org.openremote.manager.shared.assets;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import jsinterop.annotations.JsType;
import org.jboss.resteasy.annotations.Form;
import org.openremote.manager.shared.http.PATCH;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.params.EntityListParams;
import org.openremote.manager.shared.ngsi.params.EntityParams;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * http://telefonicaid.github.io/fiware-orion/api/v2/
 */
@Path("assets")
@JsType(isNative = true)
public interface AssetsResource {

    @GET
    @Path("entities")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    JsonArray getEntities(@Form RequestParams requestParams, @Form EntityListParams entityListParams);

    @POST
    @Path("entities")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(201)
    @RolesAllowed({"write:assets"})
    void postEntity(@Form RequestParams requestParams, Entity entity);

    @GET
    @Path("entities/{entityId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    JsonObject getEntity(@Form RequestParams requestParams, @PathParam("entityId") String entityId, @Form EntityParams entityParams);

    @DELETE
    @Path("entities/{entityId}")
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    void deleteEntity(@Form RequestParams requestParams, @PathParam("entityId") String entityId);

    @PUT
    @Path("entities/{entityId}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    void putEntity(@Form RequestParams requestParams, @PathParam("entityId") String entityId, Entity entity);

    @PATCH
    @Path("entities/{entityId}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    void patchEntity(@Form RequestParams requestParams, @PathParam("entityId") String entityId, Entity entity);

}
