package org.openremote.model.rules.flow;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.http.RequestParams;
import jakarta.ws.rs.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Flow", description = "Operations on flows")
@Path("flow")
public interface FlowResource {
    @GET
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAllNodeDefinitions", summary = "Retrieve all node definitions")
    Node[] getAllNodeDefinitions(@BeanParam RequestParams requestParams);

    @GET
    @Path("{type}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAllNodeDefinitionsByType", summary = "Retrieve all node definitions by type")
    Node[] getAllNodeDefinitionsByType(@BeanParam RequestParams requestParams, @PathParam("type") NodeType type);

    @GET
    @Path("{name}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getNodeDefinition", summary = "Retrieve a node definition by name")
    Node getNodeDefinition(@BeanParam RequestParams requestParams, @PathParam("name") String name);
}
