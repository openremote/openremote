package org.openremote.model.rules.flow;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;
import jakarta.ws.rs.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Flow", description = "Discover the node definitions available to the visual flow-rules editor")
@Path("flow")
public interface FlowResource {
    @GET
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAllNodeDefinitions", summary = "Retrieve all node definitions",
        description = "Returns every supported flow node definition except the internal LOG_OUTPUT node.")
    @OpenApiResponses.Ok
    Node[] getAllNodeDefinitions(@BeanParam RequestParams requestParams);

    @GET
    @Path("{type}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAllNodeDefinitionsByType", summary = "Retrieve all node definitions by type",
        description = "Returns supported flow node definitions whose category matches the requested node type.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    Node[] getAllNodeDefinitionsByType(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Flow node category to filter by.", example = "INPUT") @PathParam("type") NodeType type);

    @GET
    @Path("{name}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getNodeDefinition", summary = "Retrieve a node definition by name",
        description = "Resolves one flow node definition by its model name.")
    @OpenApiResponses.Ok
    @OpenApiResponses.ServerError
    Node getNodeDefinition(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Exact model name of the flow node definition.", example = "Attribute") @PathParam("name") String name);
}
