package org.openremote.manager.graphql;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.openremote.model.http.RequestParams;

@Path("/graphql")
public interface GraphQlWebResource {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response postGraphQL(@BeanParam RequestParams requestParams, GraphQlWebResourceImpl.GraphQLRequest requestBody);
}

