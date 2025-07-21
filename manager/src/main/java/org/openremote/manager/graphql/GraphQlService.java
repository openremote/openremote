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
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.util.ValueUtil;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.openremote.model.syslog.SyslogCategory.API;

public class GraphQlService implements ContainerService {
    /**
     * Scans for JAX-RS resource classes and generates a GraphQL schema based on their endpoints.
     * No manual schema file or code changes required.
     */

    private static final System.Logger LOG = System.getLogger(GraphQlService.class.getName() + "." + API.name());

    private GraphQLSchema schema;

    private List<WebResource> resources = null;
    private GraphQlWebResourceImpl graphQlResource;

    private static final GraphQLScalarType JSON_SCALAR = ExtendedScalars.Json;

    public void setResources(WebResource[] resources) {
        this.resources = List.of(resources);
    }

    @Override
    public int getPriority() {
        return ManagerWebService.PRIORITY+200;
    }

    public GraphQLSchema getSchema() {return schema;}

    private final Map<Class<?>, GraphQLOutputType> typeCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, GraphQLInputType> inputTypeCache = new ConcurrentHashMap<>();

    public GraphQLSchema generateSchemaFromApis() {
        Set<Class<?>> resourceClasses = getJaxRsResources();
        GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject().name("Query");
        GraphQLObjectType.Builder mutationType = GraphQLObjectType.newObject().name("Mutation");

        for (Class<?> resource : resourceClasses) {
            for (Method method : resource.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GET.class)) {
                    queryType.field(buildField(method));
                } else {
                    mutationType.field(buildField(method));
                }
            }
        }

        GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema().query(queryType.build());
        GraphQLObjectType mutation = mutationType.build();
        if (!mutation.getFieldDefinitions().isEmpty()) {
            schemaBuilder.mutation(mutation);
        }

        // Register JSON scalar

        schemaBuilder.additionalType(JSON_SCALAR);
//        schemaBuilder.additionalTypes(((Set<GraphQLScalarType>) Arrays.stream(ExtendedScalars.class.getDeclaredFields()).map(x -> (GraphQLScalarType) x.get()).collect(Collectors.toSet())));
        return schemaBuilder.build();
    }

    private GraphQLFieldDefinition buildField(Method method) {
        // Try to extract @Operation annotation for custom name/description
        String resourceName = method.getDeclaringClass().getSimpleName();
        String methodName;
        String description = null;
        Operation operation = method.getAnnotation(Operation.class);
        if (operation != null && !operation.operationId().isEmpty()) {
            // Use lowercased resource name for prefix
            methodName = resourceName.substring(0, 1).toLowerCase() + resourceName.substring(1) + "_" + operation.operationId();
            if (!operation.summary().isEmpty()) {
                description = operation.summary();
            }
        } else {
            methodName = resourceName + "_" + method.getName();
        }
        GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition().name(methodName);
        if (description != null) {
            builder.description(description);
        }

        // Add arguments for each method parameter
        Parameter[] params = method.getParameters();
        for (Parameter param : params) {
            String argName = null;
            if (param.isAnnotationPresent(QueryParam.class)) {
                argName = param.getAnnotation(QueryParam.class).value();
            } else if (param.isAnnotationPresent(PathParam.class)) {
                argName = param.getAnnotation(PathParam.class).value();
            } else if (param.isAnnotationPresent(HeaderParam.class)) {
                argName = param.getAnnotation(HeaderParam.class).value();
            } else if (param.isAnnotationPresent(CookieParam.class)) {
                argName = param.getAnnotation(CookieParam.class).value();
            } else if (param.isAnnotationPresent(FormParam.class)) {
                argName = param.getAnnotation(FormParam.class).value();
            } else {
                argName = param.getName();
            }
            // Override any query input named "requestParams"; do not expose it as a user input.
            // requestParams are filled in dynamically using the user's request context in the GraphQl resource.
            if ("requestParams".equals(argName)) {
                continue;
            }
            // Use input type if not a primitive/String
            Class<?> paramType = param.getType();
            GraphQLInputType gqlInputType;
            if (paramType.equals(String.class) || paramType.isPrimitive() || paramType.isEnum() || Number.class.isAssignableFrom(paramType) || paramType.equals(Boolean.class)) {
                gqlInputType = Scalars.GraphQLString;
            } else {
                gqlInputType = getGraphQLInputType(paramType);
            }
            builder.argument(GraphQLArgument.newArgument()
                .name(argName)
                .type(gqlInputType)
                .build());
        }

        // Map return type
        Class<?> returnType = method.getReturnType();
        boolean useJsonScalar = false;
        if (returnType.equals(void.class) || returnType.equals(Void.class)) {
            builder.type(Scalars.GraphQLString); // or custom Void scalar
        } else if (returnType.isArray()) {
            builder.type(GraphQLList.list(getGraphQLType(returnType.getComponentType())));
        } else if (Collection.class.isAssignableFrom(returnType)) {
            Type genericReturnType = method.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType) {
                Type itemType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
                if (itemType instanceof Class<?>) {
                    builder.type(GraphQLList.list(getGraphQLType((Class<?>) itemType)));
                } else {
                    builder.type(GraphQLList.list(Scalars.GraphQLString));
                }
            } else {
                builder.type(GraphQLList.list(Scalars.GraphQLString));
            }
        } else {
            builder.type(getGraphQLType(returnType));
        }
        builder.dataFetcher(env -> {
            try {
                // Find the correct resource instance from the resources set
                Object resourceInstance = null;
                for (Object res : resources) {
                    if (method.getDeclaringClass().isInstance(res)) {
                        resourceInstance = res;
                        break;
                    }
                }
                if (resourceInstance == null) {
                    throw new RuntimeException("No resource instance found for " + method.getDeclaringClass());
                }

                Parameter[] params2 = method.getParameters();
                Object[] args = new Object[params2.length];
                args[0] = env.getGraphQlContext().get("reqParams");
                ObjectMapper objectMapper = new ObjectMapper();
                for (int i = 1; i < params2.length; i++) {
                    Parameter param = params2[i];
                    Object value = null;
                    // Support for @QueryParam, @PathParam, @HeaderParam, @CookieParam, @FormParam, @Context, @DefaultValue
                    if (param.isAnnotationPresent(QueryParam.class)) {
                        String name1 = param.getAnnotation(QueryParam.class).value();
                        value = env.getArgument(name1);
                    } else if (param.isAnnotationPresent(PathParam.class)) {
                        String name = param.getAnnotation(PathParam.class).value();
                        value = env.getArgument(name);
                    } else if (param.isAnnotationPresent(HeaderParam.class)) {
                        String name = param.getAnnotation(HeaderParam.class).value();
                        value = env.getArgument(name);
                    } else if (param.isAnnotationPresent(CookieParam.class)) {
                        String name = param.getAnnotation(CookieParam.class).value();
                        value = env.getArgument(name);
                    } else if (param.isAnnotationPresent(FormParam.class)) {
                        String name = param.getAnnotation(FormParam.class).value();
                        value = env.getArgument(name);
                    } else if (param.isAnnotationPresent(DefaultValue.class)) {
                        String defaultValue = param.getAnnotation(DefaultValue.class).value();
                        value = env.getArgument(param.getName());
                        if (value == null) value = defaultValue;
                    } else {
                        // Fallback: try by parameter name
                        value = env.getArgument(param.getName());
                    }
                    // Convert Map to POJO if needed
                    if (value != null && param.getType() != null &&
                        !(param.getType().isPrimitive() || param.getType().equals(String.class) || param.getType().isEnum() || Number.class.isAssignableFrom(param.getType()) || param.getType().equals(Boolean.class)) &&
                        value instanceof Map) {
                        value = objectMapper.convertValue(value, param.getType());
                    }
                    args[i] = value;
                }
                LOG.log(System.Logger.Level.WARNING, "Invoking method: {0} with args: {1}", method.toGenericString(), Arrays.toString(args));
                Object result = method.invoke(resourceInstance, args);
                // Unwrap InvocationTargetException to expose the real cause
                if (result instanceof Throwable) {
                    throw (Throwable) result;
                }
                // Convert AttributeMap to List<Attribute> (discard keys, ensure real Attribute objects)
                if (result != null && result.getClass().getName().equals("org.openremote.model.attribute.AttributeMap")) {
                    try {
                        AttributeMap map = (AttributeMap) result;
                        List<Attribute<?>> list = map.values().stream().toList();
                        return list;
//                        java.lang.reflect.Method valuesMethod = result.getClass().getMethod("values");
//                        Object values = valuesMethod.invoke(result);
//                        if (values instanceof java.util.Collection) {
//                            List<Attribute<?>> attributeList = new java.util.ArrayList<>();
//                            for (Object value : (java.util.Collection<?>) values) {
//                                // Only add if it's an actual Attribute object
//                                if (value != null && value.getClass().getName().equals("org.openremote.model.attribute.Attribute")) {
//                                    attributeList.add(value);
//                                }
//                            }
//                            return attributeList;
//                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to convert AttributeMap to List<Attribute>", e);
                    }
                }
                // Convert 'attributes' field to a list for each Asset in arrays/collections
                if (result != null) {
                    if (result.getClass().isArray()) {
                        Object[] arr = (Object[]) result;
                        List<Object> newList = new ArrayList<>(arr.length);
                        for (Object item : arr) {
                            newList.add(convertAssetAttributesToList(item));
                        }
                        return newList;
                    } else if (result instanceof Collection) {
                        Collection<?> coll = (Collection<?>) result;
                        List<Object> newList = new ArrayList<>();
                        for (Object item : coll) {
                            newList.add(convertAssetAttributesToList(item));
                        }
                        return newList;
                    } else {
                        return convertAssetAttributesToList(result);
                    }
                }
                // Always convert to JSON if not primitive, String, or enum
                Class<?> resultClass = result.getClass();
                boolean isPrimitiveOrStringOrEnum = resultClass.isPrimitive() || resultClass.equals(String.class) || resultClass.isEnum() || Number.class.isAssignableFrom(resultClass) || resultClass.equals(Boolean.class);
                if (!isPrimitiveOrStringOrEnum) {
                    try {
                        return ValueUtil.JSON.writeValueAsString(result);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize result to JSON", e);
                    }
                }
                return result;
            } catch (InvocationTargetException e) {
                // Unwrap and throw the real cause for better error reporting
                Throwable cause = e.getCause();
                if (cause != null) {
                    throw new RuntimeException(cause);
                } else {
                    throw new RuntimeException(e);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        return builder.build();
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.equals(String.class) || Number.class.isAssignableFrom(clazz) || clazz.equals(Boolean.class) || clazz.isEnum();
    }

    private GraphQLOutputType getGraphQLType(Class<?> clazz) {
        // Map primitives and String
        // Map AttributeMap to a list of Attribute (ignore keys)

        if(typeCache.containsKey(clazz)) {return typeCache.get(clazz);}

        if (clazz.getName().equals("org.openremote.model.attribute.AttributeMap")) {
            Class<?> attributeClass = Attribute.class;
            return GraphQLList.list(getGraphQLType(attributeClass));
        }
        // Map MetaMap to a list of MetaItem (if available), otherwise GraphQLString
        if (clazz.getName().equals("org.openremote.model.attribute.MetaMap")) {
            try {
                Class<?> metaItemClass = Class.forName("org.openremote.model.attribute.MetaItem");
                return GraphQLList.list(getGraphQLType(metaItemClass));
            } catch (ClassNotFoundException e) {
                return ExtendedScalars.Json;
            }
        }
        if(clazz.getSimpleName().equals("Object")) {
            return ExtendedScalars.Object;
        }
        // Map Map and Object to GraphQLString to avoid empty object types
        if (clazz.getName().equals("java.util.Map") || clazz.getSimpleName().equals("Map")) {
            return ExtendedScalars.Object;
        }
        // Only map to GraphQLString for true primitives, enums, and known value types
        if (clazz.equals(Class.class)) return Scalars.GraphQLString;
        if (clazz.equals(String.class)) return Scalars.GraphQLString;
        if (clazz.equals(Integer.class) || clazz.equals(int.class)) return Scalars.GraphQLInt;
        if (clazz.equals(Long.class) || clazz.equals(long.class)) return Scalars.GraphQLFloat;
        if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) return Scalars.GraphQLBoolean;
        if (clazz.equals(Float.class) || clazz.equals(float.class) || clazz.equals(Double.class) || clazz.equals(double.class)) return ExtendedScalars.GraphQLLong;
        if (clazz.equals(Date.class) || clazz.getName().equals("java.time.LocalDate") || clazz.getName().equals("java.time.Instant")) return ExtendedScalars.Date;
        if (clazz.getSimpleName().equals("Id")) return ExtendedScalars.UUID;
        if (clazz.isEnum()) {
            GraphQLOutputType cachedEnum = typeCache.get(clazz);
            if (cachedEnum != null) {
                return cachedEnum;
            }
            // Use fully qualified class name for enum type name to ensure uniqueness and GraphQL compliance
            String enumTypeName = clazz.getName().replace('.', '_').replace('$', '_');
            if (!enumTypeName.matches("^[_A-Za-z].*")) {
                enumTypeName = "_" + enumTypeName;
            }
            GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum().name(enumTypeName);
            Object[] enumConstants = clazz.getEnumConstants();
            for (Object constant : enumConstants) {
                enumBuilder.value(constant.toString(), constant);
            }
            GraphQLEnumType enumType = enumBuilder.build();
            typeCache.put(clazz, enumType);
            return enumType;
        }
        // Check cache first
        GraphQLOutputType cachedType = typeCache.get(clazz);
        if (cachedType != null) {
            // If the cached type is a type reference, return it to avoid duplicate type creation
            return cachedType;
        }
        // If the type name is a built-in or common Java type, map to GraphQLString to avoid conflicts
        String typeName = clazz.getSimpleName();
        if (typeName.equals("Node") || typeName.equals("PageInfo") || typeName.equals("Entry")) {
            return Scalars.GraphQLString;
        }
        // Phase 1: Put a type reference in the cache to break recursion
        GraphQLTypeReference typeRef = GraphQLTypeReference.typeRef(typeName);
        typeCache.put(clazz, typeRef);
        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(typeName);
        boolean hasFields = false;
        for (Field field : getAllFields(clazz)) {
            // Skip static and synthetic fields
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            Class<?> fieldType = field.getType();
            GraphQLOutputType gqlType;
            if (fieldType.isArray()) {
                gqlType = GraphQLList.list(getGraphQLType(fieldType.getComponentType()));
            } else if (Collection.class.isAssignableFrom(fieldType)) {
                // Try to get generic type
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    Type itemType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                    if (itemType instanceof Class<?>) {
                        gqlType = GraphQLList.list(getGraphQLType((Class<?>) itemType));
                    } else {
                        gqlType = GraphQLList.list(Scalars.GraphQLString);
                    }
                } else {
                    gqlType = GraphQLList.list(Scalars.GraphQLString);
                }
            } else {
                gqlType = getGraphQLType(fieldType);
            }
            builder.field(f -> f.name(field.getName()).type(gqlType));
            hasFields = true;
        }
        // If no fields, add a dummy field to avoid empty type error
        if (!hasFields) {
            builder.field(f -> f.name("_dummy").type(Scalars.GraphQLString));
        }
        // Phase 2: Build the type and update the cache
        GraphQLObjectType type = builder.build();
        typeCache.put(clazz, type);
        return type;
    }

    private GraphQLInputType getGraphQLInputType(Class<?> clazz) {
        return getGraphQLInputType(clazz, new HashSet<>());
    }

    private GraphQLInputType getGraphQLInputType(Class<?> clazz, Set<Class<?>> visited) {
        // Handle arrays
        if (clazz.isArray()) {
            return GraphQLList.list(getGraphQLInputType(clazz.getComponentType(), visited));
        }
        // Handle collections (e.g., List, Set)
        if (Collection.class.isAssignableFrom(clazz)) {
            return GraphQLList.list(Scalars.GraphQLString);
        }
        if (clazz.equals(String.class) || clazz.isPrimitive() || clazz.isEnum() || Number.class.isAssignableFrom(clazz) || clazz.equals(Boolean.class)) {
            return Scalars.GraphQLString;
        }
        // Exclude Java, Jakarta, Sun, and common library types from input object mapping
        String pkg = clazz.getPackage() != null ? clazz.getPackage().getName() : "";
        if (pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("jakarta.") || pkg.startsWith("sun.") || pkg.startsWith("com.fasterxml.jackson.")) {
            return Scalars.GraphQLString;
        }
        if (visited.contains(clazz)) {
            // Prevent infinite recursion for cyclic/self-referential types
            String typeName = clazz.getName().replace('.', '_').replace('$', '_') + "Input";
            // Ensure valid GraphQL name
            typeName = typeName.replaceAll("[^_0-9A-Za-z]", "_");
            if (!typeName.matches("^[_A-Za-z].*")) typeName = "_" + typeName;
            return GraphQLTypeReference.typeRef(typeName);
        }
        GraphQLInputType cached = inputTypeCache.get(clazz);
        if (cached != null) return cached;
        visited.add(clazz);
        String typeName = clazz.getName().replace('.', '_').replace('$', '_') + "Input";
        // Ensure valid GraphQL name
        typeName = typeName.replaceAll("[^_0-9A-Za-z]", "_");
        if (!typeName.matches("^[_A-Za-z].*")) typeName = "_" + typeName;
        GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject().name(typeName);
        for (Field field : getAllFields(clazz)) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            Class<?> fieldType = field.getType();
            GraphQLInputType gqlType = getGraphQLInputType(fieldType, visited);
            builder.field(GraphQLInputObjectField.newInputObjectField().name(field.getName()).type(gqlType));
        }
        GraphQLInputObjectType inputType = builder.build();
        inputTypeCache.put(clazz, inputType);
        visited.remove(clazz);
        return inputType;
    }

    private Set<Class<?>> getJaxRsResources() {

        Set<Class<?>> resources = new HashSet<>();

        if (this.resources == null) {throw new RuntimeException("Resources not found");}

        // For each resource instance, add all interfaces it implements
        for (Object res : this.resources) {
            for (Class<?> iface : res.getClass().getInterfaces()) {
                resources.add(iface);
            }
        }
        return resources;
    }

    @Override
    public void init(Container container) throws Exception {

    }

    public static Object exportSchemaAsJson(GraphQLSchema schema, String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // graphql-java does not provide a direct way to serialize schema as JSON, so we export the introspection result
        var introspectionResult = IntrospectionQuery.INTROSPECTION_QUERY;
        ExecutionResult executionResult = GraphQL.newGraphQL(schema).build().execute(introspectionResult);
        return executionResult.getData();
    }

    public void exportSchemaAsSDL(GraphQLSchema schema, String filePath) throws IOException {
        SchemaPrinter schemaPrinter = new SchemaPrinter();
        String sdl = schemaPrinter.print(schema);
        Files.write(Paths.get(filePath), sdl.getBytes());
    }

    public String exportSchemaAsSDLString(GraphQLSchema schema) {
        SchemaPrinter schemaPrinter = new SchemaPrinter();
        return schemaPrinter.print(schema);
    }

    @Override
    public void start(Container container) throws Exception {
//        this.container = container;
        LOG.log(System.Logger.Level.INFO, "Starting GraphQlService");
        GraphQLSchema schema = generateSchemaFromApis();
        LOG.log(System.Logger.Level.INFO, schema.toString());
        // Export schema as JSON
        Object schemaJson = exportSchemaAsJson(schema, "schema.json");
        LOG.log(System.Logger.Level.INFO, ValueUtil.JSON.writeValueAsString(exportSchemaAsSDLString(schema)));
        // Export schema as SDL

        LOG.log(System.Logger.Level.INFO, "GraphQL SDL exported to schema.graphqls");

        this.schema = schema;


        this.graphQlResource.setGraphQL(GraphQL.newGraphQL(schema).build());

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    private Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && !clazz.equals(Object.class)) {
            for (Field field : clazz.getDeclaredFields()) {
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    public void setGraphQlResource(GraphQlWebResourceImpl resourceImpl) {
        this.graphQlResource = resourceImpl;
    }

    // Helper to convert a Map to a List of values, each value including the key as a field
    private List<Object> convertMapToListWithKey(Map<?, ?> map) {
        if (map == null) return null;
        List<Object> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                // Recursively convert nested maps
                value = convertMapToListWithKey((Map<?, ?>) value);
            }
            if (value instanceof List) {
                // Optionally handle lists of maps
                List<?> list = (List<?>) value;
                List<Object> newList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map) {
                        newList.add(convertMapToListWithKey((Map<?, ?>) item));
                    } else {
                        newList.add(item);
                    }
                }
                value = newList;
            }
            // If value is a POJO, add the key as a field if possible
            if (value != null && !(value instanceof Map) && !(value instanceof List) && !(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean)) {
                try {
                    Field keyField = null;
                    for (Field f : value.getClass().getDeclaredFields()) {
                        if (f.getName().equals("key")) {
                            keyField = f;
                            break;
                        }
                    }
                    if (keyField != null) {
                        keyField.setAccessible(true);
                        keyField.set(value, entry.getKey());
                        result.add(value);
                        continue;
                    }
                } catch (Exception ignore) {}
            }
            // Otherwise, use a Map to hold the key and value
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("key", entry.getKey());
            entryMap.put("value", value);
            result.add(entryMap);
        }
        return result;
    }

    // Helper to convert Asset 'attributes' field to a list if needed
    private Object convertAssetAttributesToList(Object assetObj) {
        if (assetObj == null) return null;
        try {
            Class<?> resultClass = assetObj.getClass();
            Field attributesField = null;
            Class<?> searchClass = resultClass;
            while (searchClass != null && attributesField == null) {
                try {
                    attributesField = searchClass.getDeclaredField("attributes");
                } catch (NoSuchFieldException e) {
                    searchClass = searchClass.getSuperclass();
                }
            }
            if (attributesField != null) {
                attributesField.setAccessible(true);
                Object attributesValue = attributesField.get(assetObj);
                if (attributesValue instanceof Map) {
                    // Convert the map to a list with key included
                    List<Attribute<?>> entryList = ((AttributeMap)attributesValue).values().stream().toList();
                    // Return a Map with all fields, replacing 'attributes' with the entry list
                    Map<String, Object> resultMap = new HashMap<>();
                    for (Field f : getAllFields(resultClass)) {
                        f.setAccessible(true);
                        Object value = f.get(assetObj);
                        if ("attributes".equals(f.getName())) {
                            resultMap.put("attributes", entryList);
                        } else {
                            resultMap.put(f.getName(), value);
                        }
                    }
                    return resultMap;
                }
            }
        } catch (Exception e) { /* ignore, fallback to original */ }
        return assetObj;
    }
}

