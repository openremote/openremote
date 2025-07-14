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
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset=\"utf-8\" />
          <title>GraphiQL</title>
          <link rel=\"stylesheet\" href=\"https://unpkg.com/graphiql/graphiql.min.css\" />
        </head>
        <body style=\"margin:0;\">
          <div id=\"graphiql\" style=\"height: 100vh;\"></div>
          <script crossorigin src=\"https://unpkg.com/react/umd/react.production.min.js\"></script>
          <script crossorigin src=\"https://unpkg.com/react-dom/umd/react-dom.production.min.js\"></script>
          <script crossorigin src=\"https://unpkg.com/graphiql/graphiql.min.js\"></script>
          <script>
            const graphQLFetcher = graphQLParams =>
              fetch('/graphql', {
                method: 'post',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(graphQLParams),
              })
                .then(response => response.json())
                .catch(() => response.text());
            ReactDOM.render(
              React.createElement(GraphiQL, { fetcher: graphQLFetcher }),
              document.getElementById('graphiql'),
            );
          </script>
        </body>
        </html>
        """;
        return Response.ok(graphiqlHtml).build();
    }
}
