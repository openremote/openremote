package org.openremote.model.gateway;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import org.openremote.model.Constants;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.*;

@Tag(name = "Gateway", description = "Inspect and control TCP tunnels through connected gateway assets")
@Path("gateway")
@OpenApiResponses.Authenticated
public interface GatewayServiceResource {

    /**
     * TODO: write docs
     */
    @GET
    @Path("tunnel/{realm}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    @Operation(operationId = "getAllActiveTunnelInfos", summary = "Retrieve all active gateway tunnels in a realm",
        description = "Returns active tunnels for all gateway assets in the requested realm. Restricted users cannot list an entire realm.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    GatewayTunnelInfo[] getAllActiveTunnelInfos(@BeanParam RequestParams requestParams,
                                                @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm);

    /**
     * TODO: write docs
     */
    @GET
    @Path("tunnel/{realm}/{id}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getGatewayActiveTunnelInfos", summary = "Retrieve the active tunnels of a gateway",
        description = "Returns active tunnels for one gateway asset. Restricted users may query only a linked gateway.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    GatewayTunnelInfo[] getGatewayActiveTunnelInfos(@BeanParam RequestParams requestParams,
                                                    @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
                                                    @Parameter(description = "Gateway asset identifier.", example = EXAMPLE_ASSET_ID) @PathParam("id") String gatewayId);

    @GET
    @Path("tunnel/{realm}/{id}/{target}/{targetPort}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getActiveTunnelInfo", summary = "Retrieve one active gateway tunnel",
        description = "Returns the tunnel matching realm, gateway, target host, and target port, or null when no active tunnel matches.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @ApiResponse(responseCode = "204", description = "No active tunnel matches the requested endpoint")
    GatewayTunnelInfo getActiveTunnelInfo(@BeanParam RequestParams requestParams,
                                          @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
                                          @Parameter(description = "Gateway asset identifier.", example = EXAMPLE_ASSET_ID) @PathParam("id") String gatewayId,
                                          @Parameter(description = "Host or IP address reachable from the gateway.", example = "192.168.1.20") @PathParam("target") String target,
                                          @Parameter(description = "TCP port on the target host.", example = "22") @PathParam("targetPort") int targetPort);

    /**
     * TODO: write docs
     */
    @POST
    @Path("tunnel")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "startTunnel", summary = "Start a tunnel for a gateway",
        description = "Requests a TCP tunnel through a connected gateway and returns its allocated local endpoint. When realm is omitted, the authenticated realm is used.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    @ApiResponse(responseCode = "417", description = "The gateway could not establish the requested tunnel")
    GatewayTunnelInfo startTunnel(
        @RequestBody(required = true, description = "Gateway, realm, target host, and target port for the requested tunnel. Realm defaults to the authenticated realm.") GatewayTunnelInfo tunnelInfo);

    /**
     * TODO: write docs
     */
    @DELETE
    @Path("tunnel")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "stopTunnel", summary = "Stop a tunnel for a gateway",
        description = "Stops the tunnel identified by the supplied gateway, target, and port information. When realm is omitted, the authenticated realm is used.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    @OpenApiResponses.ServerError
    void stopTunnel(
        @RequestBody(required = true, description = "Tunnel identity returned by startTunnel, including gateway and allocated endpoint information.") GatewayTunnelInfo tunnelInfo);
}
