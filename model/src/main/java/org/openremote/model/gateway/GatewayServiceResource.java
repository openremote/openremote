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
     * TODO: write docs
     */
    @GET
    @Path("tunnel/{realm}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    GatewayTunnelInfo[] getActiveTunnelInfos(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    /**
     * TODO: write docs
     */
    @GET
    @Path("tunnel/{realm}/{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    GatewayTunnelInfo getActiveTunnelInfoByGatewayId(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("id") String gatewayId);

    /**
     * TODO: write docs
     */
    @POST
    @Path("tunnel")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    GatewayTunnelInfo startTunnel(GatewayTunnelInfo tunnelInfo);

    /**
     * TODO: write docs
     */
    @DELETE
    @Path("tunnel")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    void stopTunnel(GatewayTunnelInfo tunnelInfo);
}
