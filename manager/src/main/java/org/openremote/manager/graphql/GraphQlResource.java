package org.openremote.manager.graphql;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
@Path("graphql")
@Tag(name = "GraphQL", description = "GraphQL Test")
public interface GraphQlResource {
    @Path("test")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @jakarta.ws.rs.Produces(MediaType.APPLICATION_JSON)
    Response execute(String body) throws Exception;

    @GET
    @Path("playground")
    @Produces(MediaType.TEXT_HTML)
    Response playground(@Context UriInfo uriInfo) throws Exception;
}
