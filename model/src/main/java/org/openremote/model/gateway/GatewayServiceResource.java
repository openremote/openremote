package org.openremote.model.gateway;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Gateway", description = "Operations on gateways")
@Path("gateway")
public interface GatewayServiceResource {

    /**
     * TODO: write docs
     */
    @GET
    @Path("tunnel/{realm}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    @Operation(operationId = "getAllActiveTunnelInfos", summary = "Retrieve all active gateway tunnel information of a realm")
    GatewayTunnelInfo[] getAllActiveTunnelInfos(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    /**
     * TODO: write docs
     */
    @GET
    @Path("tunnel/{realm}/{id}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getGatewayActiveTunnelInfos", summary = "Retrieve the active gateway tunnel information of gateway in a realm")
    GatewayTunnelInfo[] getGatewayActiveTunnelInfos(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("id") String gatewayId);

    @GET
    @Path("tunnel/{realm}/{id}/{target}/{targetPort}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getActiveTunnelInfo", summary = "Retrieve the gateway tunnel information of tunnel for a gateway in a realm")
    GatewayTunnelInfo getActiveTunnelInfo(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("id") String gatewayId, @PathParam("target") String target, @PathParam("targetPort") int targetPort);

    /**
     * TODO: write docs
     */
    @POST
    @Path("tunnel")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "startTunnel", summary = "Start a tunnel for a gateway")
    GatewayTunnelInfo startTunnel(GatewayTunnelInfo tunnelInfo);

    /**
     * TODO: write docs
     */
    @DELETE
    @Path("tunnel")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "stopTunnel", summary = "Stop a tunnel for a gateway")
    void stopTunnel(GatewayTunnelInfo tunnelInfo);
}
