package org.openremote.manager.server2.identity;

import org.openremote.container.web.WebResource;
import org.openremote.manager.server.identity.ClientInstall;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
        return identityService.getClientInstall(getRealm(), clientId);
    }
}
