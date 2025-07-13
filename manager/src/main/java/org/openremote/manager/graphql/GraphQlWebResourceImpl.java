package org.openremote.manager.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.model.http.RequestParams;

import java.util.Map;
import java.util.HashMap;
import javax.json.bind.annotation.JsonbProperty;

@Singleton
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
        @JsonbProperty("query")
        public String query;
        @JsonbProperty("operationName")
        public String operationName;
        @JsonbProperty("variables")
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
}
