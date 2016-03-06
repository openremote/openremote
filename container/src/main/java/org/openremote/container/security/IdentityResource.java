package org.openremote.container.security;

import org.openremote.container.web.WebResource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("identity")
public class IdentityResource extends WebResource {

    final protected IdentityService identityService;

    public IdentityResource(IdentityService identityService) {
        this.identityService = identityService;
    }

    @GET
    @Path("install/{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ClientInstall getClientInstall(@PathParam("clientId") String clientId) {
        SecuredClientApplication clientApplication =
            identityService.getSecuredClientApplication(getRealm(), clientId);
        if (clientApplication == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        return clientApplication.clientInstall;
    }
}
