package org.openremote.manager.shared.contextbroker;

import jsinterop.annotations.JsType;
import org.jboss.resteasy.annotations.Form;
import org.openremote.manager.shared.http.PATCH;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.EntryPoint;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("contextbroker")
@JsType(isNative = true)
public interface ContextBrokerResource {

    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    EntryPoint getEntryPoint(@Form RequestParams requestParams);

    @GET
    @Path("entities")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    Entity[] getEntities(@Form RequestParams requestParams);

    @POST
    @Path("entities")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(201)
    @RolesAllowed({"write:assets"})
    Response postEntity(@Form RequestParams requestParams, Entity entity);

    @GET
    @Path("entities/{entityId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    Entity getEntity(@Form RequestParams requestParams, @PathParam("entityId") String entityId);

    @DELETE
    @Path("entities/{entityId}")
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    Response deleteEntity(@Form RequestParams requestParams, @PathParam("entityId") String entityId);

    @PUT
    @Path("entities/{entityId}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    Response putEntity(@Form RequestParams requestParams, @PathParam("entityId") String entityId, Entity entity);

    @PATCH
    @Path("entities/{entityId}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed({"write:assets"})
    Response patchEntity(@Form RequestParams requestParams, @PathParam("entityId") String entityId, Entity entity);

}
