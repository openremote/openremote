package org.openremote.manager.server2.identity;

import org.openremote.container.web.WebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;

@Path("identity")
public class IdentityResource extends WebResource {

    private static final Logger LOG = Logger.getLogger(IdentityResource.class.getName());

    @Context
    javax.ws.rs.core.UriInfo uriInfo;

    final protected KeycloakClient keycloakClient;

    public IdentityResource(KeycloakClient keycloakClient) {
        this.keycloakClient = keycloakClient;
    }

    @GET
    @Path("install/or-manager")
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@QueryParam("realm") String realm, @QueryParam("foo") String foo) {
        LOG.info("### ---------------------- " + uriInfo.getRequestUri());
        LOG.info("### ---------------------- " + uriInfo.getQueryParameters());
        LOG.info("### REALM: " + realm);
        LOG.info("### APPLICATION: " + getApplication().getContainer());
        LOG.info("### FOO: " + foo);
        LOG.info("### KEYCLOAK: " + keycloakClient);
        return "OK";
    }
}
