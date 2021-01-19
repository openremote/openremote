package org.openremote.model.rules.flow;

import org.openremote.model.http.RequestParams;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("flow")
public interface FlowResource {
    @GET
    @Produces(APPLICATION_JSON)
Node[] getAllNodeDefinitions(@BeanParam RequestParams requestParams);

    @GET
    @Path("{type}")
    @Produces(APPLICATION_JSON)
Node[] getAllNodeDefinitionsByType(@BeanParam RequestParams requestParams, @PathParam("type") NodeType type);

    @GET
    @Path("{name}")
    @Produces(APPLICATION_JSON)
Node getNodeDefinition(@BeanParam RequestParams requestParams, @PathParam("name") String name);
}
