package org.openremote.manager.graphql;

import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.io.IOUtils;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.util.ValueUtil;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GraphQlResourceImpl extends ManagerWebResource implements GraphQlResource {
    private final TimerService TimerService;
    private final ManagerIdentityService IdentityService;
    private final GraphQlService GraphQlService;

    public GraphQlResourceImpl(TimerService timerService, ManagerIdentityService identityService, GraphQlService graphQlService) {
        super(timerService, identityService);

        this.TimerService = timerService;
        this.IdentityService = identityService;
        this.GraphQlService = graphQlService;

    }

    public Response execute(String body) throws Exception {
        ObjectNode request = (ObjectNode) ValueUtil.JSON.readTree(body);
        String query = request.get("query").asText();
        Map<String, Object> variables = ValueUtil.JSON.convertValue(
                request.get("variables"), Map.class);
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .build();
        ExecutionResult result =  this.GraphQlService.graphQL.execute(input);
        return Response.ok(result.toSpecification()).build();
    }

    public Response playground(@Context UriInfo uriInfo) throws Exception {
        String html = IOUtils.toString(
                getClass().getResourceAsStream("/graphql/playground.html"),
                StandardCharsets.UTF_8);
        html = html.replace("${GRAPHQL_ENDPOINT}",
                uriInfo.getBaseUriBuilder().path("graphql").build().toString());
        return Response.ok(html).build();
    }
}
