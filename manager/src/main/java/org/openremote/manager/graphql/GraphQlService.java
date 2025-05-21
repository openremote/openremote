package org.openremote.manager.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.query.BeanResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.PublicResolverBuilder;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.DefaultTypeTransformer;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.MemberOperationInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeInfoGenerator;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.annotations.types.GraphQLType;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetResourceImpl;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.map.MapResourceImpl;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointAllQuery;
import org.openremote.model.datapoint.query.AssetDatapointQuery;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.query.AssetQuery;

import java.io.InputStreamReader;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;

public class GraphQlService implements ContainerService {
    protected GraphQL graphQL;

    protected AssetDatapointService assetDatapointService;
    protected AssetStorageService assetStorageService;
    protected ManagerIdentityService managerIdentityService;

    @Override
    public int getPriority() {
        return LOW_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        container.getService(ManagerWebService.class).addApiSingleton(
                new GraphQlResourceImpl(
                        container.getService(TimerService.class),
                        container.getService(ManagerIdentityService.class),
                        this)
        );

        // Create API instances
        AssetDatapointApi datapointApi = new AssetDatapointApi(
            container.getService(AssetDatapointService.class),
            container.getService(AssetStorageService.class), 
            container.getService(ManagerIdentityService.class)
        );

        AssetApi assetApi = new AssetApi(
            container.getService(AssetStorageService.class),
            container.getService(ManagerIdentityService.class)
        );

        // Create a custom type info generator that handles Asset<?> properly
        TypeInfoGenerator typeInfoGenerator = new DefaultTypeInfoGenerator() {
            @Override
            public String generateTypeName(AnnotatedType type, MessageBundle messageBundle) {
                // For any type that is or contains Asset, use "Asset" as the type name
                if (type.getType() instanceof Class<?> clazz && Asset.class.isAssignableFrom(clazz)) {
                    return "Asset";
                }
                return super.generateTypeName(type, messageBundle);
            }
        };

        // Create a custom type transformer to handle GraphQL types
        TypeTransformer typeTransformer = new DefaultTypeTransformer(true, true) {
            @Override
            public AnnotatedType transform(AnnotatedType type) {
                // For any GraphQL annotated type that involves Asset, use ThingAsset
                if (type.getAnnotations().length > 0 && 
                    type.getAnnotations()[0].annotationType().getName().contains("GraphQL")) {
                    
                    // For any type that is or contains Asset, use ThingAsset
                    if (type.getType() instanceof Class<?> clazz) {
                        // Handle inner classes of AssetQuery
                        if (clazz.getDeclaringClass() != null && clazz.getDeclaringClass().equals(AssetQuery.class)) {
                            // For inner classes of AssetQuery, ensure we use Asset<?> as type parameter if required
                            TypeVariable<?>[] typeParams = clazz.getTypeParameters();
                            if (typeParams.length > 0) {
                                // Check if the type parameter has a bound of Asset<?>
                                for (TypeVariable<?> typeParam : typeParams) {
                                    for (Type bound : typeParam.getBounds()) {
                                        if (bound instanceof ParameterizedType boundType && 
                                            boundType.getRawType().equals(Asset.class)) {
                                            // This inner class has a type parameter bound to Asset<?>
                                            // Use Asset<?> as the type parameter
                                            return GenericTypeReflector.annotate(
                                                TypeFactory.parameterizedClass(
                                                    clazz,
                                                    new Type[] { TypeFactory.parameterizedClass(Asset.class, new Type[] { WildcardType.class }) }
                                                )
                                            );
                                        }
                                    }
                                }
                            }
                            // If no Asset<?> bound found, return as is
                            return GenericTypeReflector.annotate(clazz);
                        }
                        
                        // Handle Asset types
                        if (Asset.class.isAssignableFrom(clazz)) {
                            return GenericTypeReflector.annotate(ThingAsset.class);
                        }
                    }
                    
                    if (type.getType() instanceof ParameterizedType parameterizedType) {
                        Type rawType = parameterizedType.getRawType();
                        if (rawType instanceof Class<?> rawClass) {
                            // Handle inner classes of AssetQuery
                            if (rawClass.getDeclaringClass() != null && rawClass.getDeclaringClass().equals(AssetQuery.class)) {
                                // For inner classes of AssetQuery, ensure we use Asset<?> as type parameter if required
                                TypeVariable<?>[] typeParams = rawClass.getTypeParameters();
                                if (typeParams.length > 0) {
                                    // Check if the type parameter has a bound of Asset<?>
                                    for (TypeVariable<?> typeParam : typeParams) {
                                        for (Type bound : typeParam.getBounds()) {
                                            if (bound instanceof ParameterizedType boundType && 
                                                boundType.getRawType().equals(Asset.class)) {
                                                // This inner class has a type parameter bound to Asset<?>
                                                // Use Asset<?> as the type parameter
                                                return GenericTypeReflector.annotate(
                                                    TypeFactory.parameterizedClass(
                                                        rawClass,
                                                        new Type[] { TypeFactory.parameterizedClass(Asset.class, new Type[] { WildcardType.class }) }
                                                    )
                                                );
                                            }
                                        }
                                    }
                                }
                                // If no Asset<?> bound found, preserve original type parameters
                                return GenericTypeReflector.annotate(parameterizedType);
                            }
                            
                            // Handle Asset types
                            if (Asset.class.isAssignableFrom(rawClass)) {
                                return GenericTypeReflector.annotate(ThingAsset.class);
                            }
                        }
                    }

                    if (type.getType() instanceof WildcardType wildcardType) {
                        Type[] upperBounds = wildcardType.getUpperBounds();
                        if (upperBounds.length > 0 && upperBounds[0] instanceof ParameterizedType upperBoundType) {
                            Type rawType = upperBoundType.getRawType();
                            if (rawType instanceof Class<?> rawClass) {
                                // Handle inner classes of AssetQuery
                                if (rawClass.getDeclaringClass() != null && rawClass.getDeclaringClass().equals(AssetQuery.class)) {
                                    // For inner classes of AssetQuery, ensure we use Asset<?> as type parameter if required
                                    TypeVariable<?>[] typeParams = rawClass.getTypeParameters();
                                    if (typeParams.length > 0) {
                                        // Check if the type parameter has a bound of Asset<?>
                                        for (TypeVariable<?> typeParam : typeParams) {
                                            for (Type bound : typeParam.getBounds()) {
                                                if (bound instanceof ParameterizedType boundType && 
                                                    boundType.getRawType().equals(Asset.class)) {
                                                    // This inner class has a type parameter bound to Asset<?>
                                                    // Use Asset<?> as the type parameter
                                                    return GenericTypeReflector.annotate(
                                                        TypeFactory.parameterizedClass(
                                                            rawClass,
                                                            new Type[] { TypeFactory.parameterizedClass(Asset.class, new Type[] { WildcardType.class }) }
                                                        )
                                                    );
                                                }
                                            }
                                        }
                                    }
                                    // If no Asset<?> bound found, preserve original type parameters
                                    return GenericTypeReflector.annotate(upperBoundType);
                                }
                                
                                // Handle Asset types
                                if (Asset.class.isAssignableFrom(rawClass)) {
                                    return GenericTypeReflector.annotate(ThingAsset.class);
                                }
                            }
                        }
                    }
                }

                return super.transform(type);
            }
        };

        // Create schema with minimal customization
        GraphQLSchemaGenerator schema = new GraphQLSchemaGenerator()
                .withOperationsFromSingletons(datapointApi, assetApi)
                .withBasePackages("org.openremote")
                .withTypeInfoGenerator(typeInfoGenerator)
                .withTypeTransformer(typeTransformer)
                .withResolverBuilders(new AnnotatedResolverBuilder()
                    .withOperationInfoGenerator(new SnakeCaseOperationNameGenerator()));

        graphQL = GraphQL.newGraphQL(schema.generate()).build();
    }

    private RuntimeWiring buildWiring(Container container) {
        AssetDatapointService dpService = container.getService(AssetDatapointService.class);
        AssetStorageService storage = container.getService(AssetStorageService.class);
        ManagerIdentityService identity = container.getService(ManagerIdentityService.class);

        return RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("datapoints", env -> {
                            String assetId = env.getArgument("assetId");
                            String attributeName = env.getArgument("attributeName");
                            Map<String, Object> qMap = env.getArgument("query");
                            AssetDatapointQuery q = new AssetDatapointAllQuery(
                                    ((Number)qMap.get("from")).longValue(),
                                    ((Number)qMap.get("to")).longValue()
                            );
                            // auth checks omitted for brevity
                            return dpService.queryDatapoints(assetId, attributeName, q);
                        })
                        .dataFetcher("datapointPeriod", env -> {
                            String assetId = env.getArgument("assetId");
                            String attributeName = env.getArgument("attributeName");
                            return dpService.getDatapointPeriod(assetId, attributeName);
                        })
                )
                .build();
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {
        // No-op
    }
}

