package org.openremote.manager.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.*;
import graphql.schema.idl.SchemaPrinter;
import graphql.scalars.ExtendedScalars;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.*;
import org.openremote.container.web.WebResource;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.util.ValueUtil;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.openremote.model.syslog.SyslogCategory.API;

/**
 * A GraphQL service that builds a schema by scanning JAX-RS resource types.
 * It handles complex types, circular references, and simplifies Map outputs.
 */
public final class GraphQlService implements ContainerService {

    private static final System.Logger LOG = System.getLogger(GraphQlService.class.getName() + "." + API.name());
    private static final GraphQLScalarType JSON_SCALAR = ExtendedScalars.Json;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<TypeKey, GraphQLType> typeCache = new ConcurrentHashMap<>();

    private GraphQLSchema schema;
    private List<WebResource> resources = List.of();
    private GraphQlWebResourceImpl graphQlEndpoint;

    // ──────────────────────────────────────────────────────────────────────────
    // Configuration & Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    public void setResources(WebResource[] resources) { this.resources = List.of(resources); }
    public void setGraphQlResource(GraphQlWebResourceImpl endpoint) { this.graphQlEndpoint = endpoint; }
    @Override public int getPriority() { return ManagerWebService.PRIORITY + 200; }
    @Override public void init(Container container) {}
    @Override public void stop(Container container) {}

    @Override
    public void start(Container container) throws Exception {
        LOG.log(System.Logger.Level.INFO, "Bootstrapping GraphQL service with type scanning…");
        schema = buildSchema();
        graphQlEndpoint.setGraphQL(GraphQL.newGraphQL(schema).build());
        dumpSchema(schema);
        LOG.log(System.Logger.Level.INFO, "GraphQL service ready.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Schema Generation
    // ──────────────────────────────────────────────────────────────────────────

    private GraphQLSchema buildSchema() {
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("Query");
        GraphQLObjectType.Builder mutationType = GraphQLObjectType.newObject().name("Mutation");

        discoverResourceInterfaces().forEach(iface ->
            Arrays.stream(iface.getDeclaredMethods())
                .filter(method -> !method.isBridge())
                .forEach(method -> {
                    try {
                        GraphQLFieldDefinition field = buildField(method);
                        if (method.isAnnotationPresent(GET.class)) {
                            queryType.field(field);
                        } else {
                            mutationType.field(field);
                        }
                    } catch (Exception e) {
                        LOG.log(System.Logger.Level.WARNING, "Skipping method " + method.getName() + " due to schema build error", e);
                    }
                })
        );

        // Finalize the schema
        GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema().query(queryType);
        if (mutationType.build().getFieldDefinitions().size() > 0) {
            schemaBuilder.mutation(mutationType);
        }

        // Add all concrete types from the cache to the schema
        Set<GraphQLType> additionalTypes = new HashSet<>(typeCache.values());
        additionalTypes.add(JSON_SCALAR);
        schemaBuilder.additionalTypes(additionalTypes);

        return schemaBuilder.build();
    }

    private GraphQLFieldDefinition buildField(Method method) {
        String fieldName = resolveFieldName(method);
        GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition()
            .name(fieldName)
            .description(getOperationSummary(method))
            .type(getOutputType(method.getGenericReturnType()));

        for (Parameter parameter : method.getParameters()) {
            if (isInjectable(parameter)) continue;
            builder.argument(buildArgument(parameter));
        }

        builder.dataFetcher(env -> invoke(method, env));
        return builder.build();
    }

    private GraphQLArgument buildArgument(Parameter parameter) {
        return GraphQLArgument.newArgument()
            .name(resolveArgName(parameter))
            .type(getInputType(parameter.getParameterizedType()))
            .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Type Mapping
    // ──────────────────────────────────────────────────────────────────────────

    private GraphQLOutputType getOutputType(Type type) {
        return (GraphQLOutputType) mapType(type, false, new HashSet<>());
    }

    private GraphQLInputType getInputType(Type type) {
        return (GraphQLInputType) mapType(type, true, new HashSet<>());
    }

    private GraphQLType mapType(Type type, boolean isInput, Set<Type> visited) {
        // Handle collections and arrays first
        if (type instanceof ParameterizedType pt && Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
            Type valueType = pt.getActualTypeArguments()[0];
            return GraphQLList.list(mapType(valueType, isInput, visited));
        }
        if (type instanceof Class<?> cls && cls.isArray()) {
            return GraphQLList.list(mapType(cls.getComponentType(), isInput, visited));
        }

        // Handle Maps
        if (type instanceof ParameterizedType pt && Map.class.isAssignableFrom((Class<?>) pt.getRawType())) {
            if (isInput) return JSON_SCALAR; // Input maps are treated as JSON
            Type valueType = pt.getActualTypeArguments()[1]; // Get the value type of the map
            return GraphQLList.list(mapType(valueType, isInput, visited)); // Represent as a list of values
        }

        // Resolve the raw class for the type
        Class<?> rawClass = getRawClass(type);
        if (rawClass == null) return JSON_SCALAR;

        // Handle simple scalars
        if (isScalar(rawClass)) return mapScalar(rawClass);

        // Handle enums
        if (rawClass.isEnum()) {
            return buildEnumType(rawClass);
        }

        // Handle complex object types
        return isInput ? buildInputObjectType(rawClass, visited) : buildOutputObjectType(rawClass, visited);
    }

    private GraphQLType buildOutputObjectType(Class<?> type, Set<Type> visited) {
        TypeKey key = new TypeKey(type, false);
        if (typeCache.containsKey(key)) return typeCache.get(key);

        if (visited.contains(type)) {
            return GraphQLTypeReference.typeRef(sanitizeTypeName(type.getSimpleName()));
        }
        visited.add(type);

        String name = sanitizeTypeName(type.getSimpleName());
        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(name);

        // Put a reference in the cache to handle recursion
        typeCache.put(key, GraphQLTypeReference.typeRef(name));

        for (Field field : getAllInstanceFields(type)) {
            builder.field(GraphQLFieldDefinition.newFieldDefinition()
                .name(field.getName())
                .type(getOutputType(field.getGenericType())));
        }

        GraphQLObjectType objectType = builder.build();
        typeCache.put(key, objectType); // Replace reference with the concrete type
        visited.remove(type);
        return objectType;
    }

    private GraphQLType buildInputObjectType(Class<?> type, Set<Type> visited) {
        TypeKey key = new TypeKey(type, true);
        if (typeCache.containsKey(key)) return typeCache.get(key);

        if (visited.contains(type)) {
            return GraphQLTypeReference.typeRef(sanitizeTypeName(type.getSimpleName()) + "Input");
        }
        visited.add(type);

        String name = sanitizeTypeName(type.getSimpleName()) + "Input";
        GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject().name(name);

        typeCache.put(key, GraphQLTypeReference.typeRef(name));

        for (Field field : getAllInstanceFields(type)) {
            builder.field(GraphQLInputObjectField.newInputObjectField()
                .name(field.getName())
                .type(getInputType(field.getGenericType())));
        }

        GraphQLInputObjectType inputObjectType = builder.build();
        typeCache.put(key, inputObjectType);
        visited.remove(type);
        return inputObjectType;
    }

    private GraphQLType buildEnumType(Class<?> enumClass) {
        TypeKey key = new TypeKey(enumClass, false);
        return typeCache.computeIfAbsent(key, k -> {
            String name = sanitizeTypeName(enumClass.getSimpleName());
            GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name(name);
            for (Object enumConstant : enumClass.getEnumConstants()) {
                builder.value(enumConstant.toString());
            }
            return builder.build();
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Data Fetching & Invocation
    // ──────────────────────────────────────────────────────────────────────────

    private Object invoke(Method method, DataFetchingEnvironment env) throws Exception {
        Object target = resourceInstance(method.getDeclaringClass());
        Object[] args = buildArguments(method, env);
        LOG.log(System.Logger.Level.DEBUG, () -> "Invoking " + method.getName() + " with " + Arrays.toString(args));
        Object result = method.invoke(target, args);

        // If the result is a Map, return its values as per the requirement
        if (result instanceof Map) {
            return new ArrayList<>(((Map<?, ?>) result).values());
        }
        return result;
    }

    private Object[] buildArguments(Method method, DataFetchingEnvironment env) {
        return Arrays.stream(method.getParameters())
            .map(param -> {
                if (isInjectable(param)) {
                    return env.getGraphQlContext().get("requestParams");
                }
                Object value = env.getArgument(resolveArgName(param));
                if (value == null) return null;
                // Use Jackson to convert from Map-like input to the target Java type
                return OBJECT_MAPPER.convertValue(value, param.getType());
            })
            .toArray();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Reflection & Naming Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Class<?> getRawClass(Type type) {
        if (type instanceof Class<?> cls) return cls;
        if (type instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        return null;
    }

    private boolean isScalar(Class<?> cls) {
        return cls.isPrimitive() || cls.equals(String.class) || Number.class.isAssignableFrom(cls) || cls.equals(Boolean.class)
            || Date.class.isAssignableFrom(cls) || java.time.temporal.Temporal.class.isAssignableFrom(cls);
    }

    private GraphQLScalarType mapScalar(Class<?> cls) {
        if (cls.equals(String.class)) return Scalars.GraphQLString;
        if (cls.equals(Boolean.class) || cls.equals(boolean.class)) return Scalars.GraphQLBoolean;
        if (cls.equals(Integer.class) || cls.equals(int.class)) return Scalars.GraphQLInt;
        if (cls.equals(Long.class) || cls.equals(long.class)) return Scalars.GraphQLFloat;
        if (cls.equals(Float.class) || cls.equals(float.class) || cls.equals(Double.class) || cls.equals(double.class)) return Scalars.GraphQLFloat;
        return Scalars.GraphQLString; // Default
    }

    private List<Field> getAllInstanceFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> t = type; t != null && t != Object.class; t = t.getSuperclass()) {
            for (Field field : t.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private String sanitizeTypeName(String name) {
        return name.replaceAll("[\\[\\]<>,\\s]", "");
    }

    private String resolveFieldName(Method method) {
        String summary = getOperationSummary(method);
        if (summary != null && !summary.isBlank()) {
            String prefix = method.getDeclaringClass().getSimpleName();
            return Character.toLowerCase(prefix.charAt(0)) + prefix.substring(1) + '_' + summary.replaceAll("\\s+", "");
        }
        return method.getDeclaringClass().getSimpleName() + '_' + method.getName();
    }

    private String resolveArgName(Parameter param) {
        if (param.isAnnotationPresent(QueryParam.class)) return param.getAnnotation(QueryParam.class).value();
        if (param.isAnnotationPresent(PathParam.class)) return param.getAnnotation(PathParam.class).value();
        if (param.isAnnotationPresent(HeaderParam.class)) return param.getAnnotation(HeaderParam.class).value();
        if (param.isAnnotationPresent(CookieParam.class)) return param.getAnnotation(CookieParam.class).value();
        if (param.isAnnotationPresent(FormParam.class)) return param.getAnnotation(FormParam.class).value();
        return param.getName();
    }

    private String getOperationSummary(Method method) {
        Operation op = method.getAnnotation(Operation.class);
        return (op != null) ? op.summary() : null;
    }

    private boolean isInjectable(Parameter parameter) {
        return "requestParams".equals(parameter.getName());
    }

    private Set<Class<?>> discoverResourceInterfaces() {
        if (resources.isEmpty()) throw new IllegalStateException("No JAX-RS resources registered.");
        return resources.stream()
            .flatMap(r -> Arrays.stream(r.getClass().getInterfaces()))
            .collect(Collectors.toSet());
    }

    private Object resourceInstance(Class<?> iface) {
        return resources.stream()
            .filter(iface::isInstance)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No resource instance found for interface: " + iface.getName()));
    }

    private void dumpSchema(GraphQLSchema schema) {
        try {
            ExecutionResult json = GraphQL.newGraphQL(schema).build().execute(IntrospectionQuery.INTROSPECTION_QUERY);
            Files.writeString(Paths.get("schema.json"), ValueUtil.JSON.writeValueAsString(json.getData()));
            Files.writeString(Paths.get("schema.graphqls"), new SchemaPrinter().print(schema));
            LOG.log(System.Logger.Level.INFO, "GraphQL schema artifacts written to schema.json and schema.graphqls");
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Failed to write GraphQL schema artifacts", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Cache Key
    // ──────────────────────────────────────────────────────────────────────────
    private static final class TypeKey {
        private final Class<?> type;
        private final boolean isInput;

        TypeKey(Class<?> type, boolean isInput) {
            this.type = type;
            this.isInput = isInput;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypeKey typeKey = (TypeKey) o;
            return isInput == typeKey.isInput && type == typeKey.type; // Identity comparison for class
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(type) * 31 + Boolean.hashCode(isInput);
        }
    }
}
