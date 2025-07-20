package org.openremote.manager.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.model.http.RequestParams;

import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonProperty;

@Singleton
@Path("/graphql")
public class GraphQlWebResourceImpl implements GraphQlWebResource {

    private GraphQL graphQL = null;
    private GraphQlService graphQlService;
    private TimerService timerService;

    public GraphQlWebResourceImpl() {}

    public GraphQlWebResourceImpl(TimerService timerService, GraphQlService graphQlService, GraphQLSchema schema) {
        this.graphQlService = graphQlService;
        this.timerService = timerService;
        this.graphQL = null;
    }

    public static class GraphQLRequest {
        @JsonProperty("query")
        public String query;
        @JsonProperty("operationName")
        public String operationName;
        @JsonProperty("variables")
        public Map<String, Object> variables;
    }

    public void setGraphQL(GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    public Response postGraphQL(RequestParams requestParams, GraphQLRequest requestBody) {
        if (requestBody == null || requestBody.query == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing GraphQL query").build();
        }

        // Ensure requestParams is always present in variables
        Map<String, Object> variables = requestBody.variables != null ? new HashMap<>(requestBody.variables) : new HashMap<>();
        // Add requestParams as a copy of all variables (or empty map if none)
        variables.put("requestParams", requestParams);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(requestBody.query)
                .graphQLContext(Map.of("reqParams", requestParams))
                .operationName(requestBody.operationName)
                .variables(variables)
                .build();
        ExecutionResult executionResult = graphQL.execute(executionInput);
        Map<String, Object> result = executionResult.toSpecification();
        return Response.ok(result).build();
    }

    public Response getGraphQLSandbox() {
        String graphiqlHtml = """
<div id="sandbox" style="position:absolute;top:0;right:0;bottom:0;left:0"></div>
<script src="https://embeddable-sandbox.cdn.apollographql.com/_latest/embeddable-sandbox.umd.production.min.js"></script>
<script>
 new window.EmbeddedSandbox({
   target: "#sandbox",
   // Pass through your server href if you are embedding on an endpoint.
   // Otherwise, you can pass whatever endpoint you want Sandbox to start up with here.
   initialEndpoint: "",
   initialState: {pollForSchemaUpdates: false;}
   
 
 });
 // advanced options: https://www.apollographql.com/docs/studio/explorer/sandbox#embedding-sandbox
</script>
        """;
        return Response.ok(graphiqlHtml).type(MediaType.TEXT_HTML_TYPE).build();
    }
}
