package org.openremote.model.gateway;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Gateway")
@Path("gateway")
public interface GatewayServiceResource {

    /**
     *
     */
    @GET
    @Path("tunnel/{realm}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    TunnelInfo[] getActiveTunnelInfos(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    @GET
    @Path("tunnel/{realm}/{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    TunnelInfo getActiveTunnelInfoById(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("id") String tunnelId);

    @POST
    @Path("tunnel")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    void startTunnel(TunnelInfo tunnelInfo);

    @DELETE
    @Path("tunnel")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    void stopTunnel(TunnelInfo tunnelInfo);
}
