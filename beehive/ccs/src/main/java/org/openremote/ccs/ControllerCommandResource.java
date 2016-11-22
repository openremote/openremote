package org.openremote.ccs;

import jsinterop.annotations.JsType;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("command")
@JsType(isNative = true)
public interface ControllerCommandResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"account-owner", "service-admin"})
    Response saveCommand(@Context HttpServletRequest request, String jsonString);

    /**
     * Mark the controllerCommand with the given id as DONE<p>
     * REST Url: /rest/command/{commandId}
     *
     * @return ResultObject with String "ok"
     */
    @DELETE
    @Path("{commandId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"account-owner", "service-admin"})
    Response ackControllerCommands(@Context HttpServletRequest request, @PathParam("commandId") String commandId);

    /**
     * Return a list of all not finished ControllerCommands<p>
     * REST Url: /rest/commands/{controllerOid} -> return all not finished controller commands for the given controllerOid
     *
     * @return a List of ControllerCommand
     */
    @GET
    @Path("controller/{controllerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"account-owner", "service-admin"})
    Response loadControllerCommands(@Context HttpServletRequest request, @PathParam("controllerId") String controllerId);

}
