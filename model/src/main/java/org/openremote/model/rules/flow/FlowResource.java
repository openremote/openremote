package org.openremote.model.rules.flow;

import jsinterop.annotations.JsType;
import org.openremote.model.http.RequestParams;
import org.openremote.model.http.SuccessStatusCode;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("flow")
@JsType(isNative = true)
public interface FlowResource {
    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    Node[] getAllNodeDefinitions(@BeanParam RequestParams requestParams);

    @GET
    @Path("{type}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    Node[] getAllNodeDefinitionsByType(@BeanParam RequestParams requestParams, @PathParam("type") NodeType type);

    @GET
    @Path("{name}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    Node getNodeDefinition(@BeanParam RequestParams requestParams, @PathParam("name") String name);
}
