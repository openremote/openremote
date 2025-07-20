package org.openremote.manager.graphql;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

@Path("/graphql")
public interface GraphQlWebResource {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "writeAttributeValues", summary = "Update attribute values")
    Response postGraphQL(@BeanParam RequestParams requestParams, GraphQlWebResourceImpl.GraphQLRequest requestBody);

    @GET
    @Path("/sandbox")
    @Produces(MediaType.TEXT_HTML)
    public Response getGraphQLSandbox();
}

