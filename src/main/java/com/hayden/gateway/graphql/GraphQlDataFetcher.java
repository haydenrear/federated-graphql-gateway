package com.hayden.gateway.graphql;

import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.graphql.models.GraphQlTarget;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.utilitymodule.MapFunctions;
import com.hayden.utilitymodule.result.error.SingleError;
import com.hayden.utilitymodule.result.Result;
import graphql.language.*;
import graphql.schema.*;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Add a DataFetcher to the DGS context for accessing that DataFetcher when a GraphQl query is sent to the Gateway.
 */
@Slf4j
public record GraphQlDataFetcher(@Delegate GraphQlDataFetcherDiscoveryModel model, MessageDigestBytes digest, ContextCallback removeCallback)
        implements GraphQlServiceApiVisitor {

    public GraphQlDataFetcher(GraphQlDataFetcherDiscoveryModel model, MessageDigestBytes digest) {
        this(model, digest, new ContextCallback());
    }

    @Override
    public boolean onRemoved() {
        removeCallback.callback.accept(id().host().host(), model.serviceId());
        return true;
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(RegistriesComposite registries, Context.RegistriesContext ctx) {
        SchemaParser schemaParser = new SchemaParser();
        var typeDefinitionRegistry = registries.typeDefinitionRegistry();
        var codeRegistryBuilder = registries.codeRegistry();
        this.model.source()
                .forEach(sourceItem -> {
                    var schema = sourceItem.targetType();
                    if (schema == GraphQlTarget.String) {
                        Optional.ofNullable(sourceItem.target())
                                .stream()
                                .map(schemaParser::parse)
                                .forEach(newQ -> {
//                                    TODO: extract out - add @Mutation
                                    typeDefinitionRegistry.getType("Query")
                                            .ifPresentOrElse(td -> {
                                                newQ.getType("Query")
                                                        .ifPresentOrElse(toAddTd -> {
                                                            if (td instanceof ObjectTypeDefinition f
                                                                && toAddTd instanceof ObjectTypeDefinition o) {
                                                                List<FieldDefinition> defs = new java.util.ArrayList<>(f.getFieldDefinitions().stream().toList());
                                                                defs.addAll(o.getFieldDefinitions().stream().toList());
                                                                var newTd = ObjectTypeDefinition.newObjectTypeDefinition()
                                                                        .name(f.getName())
                                                                        .implementz(f.getImplements().stream().toList())
                                                                        .directives(f.getDirectives().stream().toList())
                                                                        .fieldDefinitions(defs)
                                                                        .description(f.getDescription())
                                                                        .sourceLocation(f.getSourceLocation())
                                                                        .comments(f.getComments())
                                                                        .ignoredChars(f.getIgnoredChars())
                                                                        .additionalData(f.getAdditionalData())
                                                                        .build();
                                                                typeDefinitionRegistry.remove("Query", td);
                                                                typeDefinitionRegistry.add(newTd);
                                                            }
                                                            newQ.remove("Query", toAddTd);
                                                        }, () -> {});
                                            }, () -> {});

                                    typeDefinitionRegistry.merge(newQ);
                                });
                    }
                });

        ctx.codegenContext().javaCompiler()
                .dataFetcherClasses(this.model.fetcherSource(), this.model.source())
                .stream()
                .flatMap(g -> g.results().stream())
                .map(result -> Map.entry(
                        result.typeName(),
                        MapFunctions.CollectMap(result.fetchers().entrySet().stream()
                                .flatMap(f -> nextDataFetcherItem(f, ctx).stream()))
                ))
                .forEach(nextFetcher -> registerDataFetcher(codeRegistryBuilder, nextFetcher, ctx));

        ctx.mimeTypeDefinitionContext().dataFetchers()
                .forEach(dfr -> typeDefinitionRegistry.types()
                        .values().stream()
                        .flatMap(td -> td instanceof ObjectTypeDefinition otd
                                       ? Stream.of(otd)
                                       : Stream.empty())
                        .flatMap(otd -> this.fieldDefinitionsFor(dfr.id.dataFetchedClassName(), typeDefinitionRegistry, otd))
                        .forEach(f -> addDataFetcherField(dfr, f)));

        var fetchersByTy = MapFunctions.CollectMapRidDuplicates(
                ctx.mimeTypeDefinitionContext().dataFetchers()
                        .stream()
                        .flatMap(dfr -> dfr.dataFetcherData.field().stream().map(dfa -> Map.entry(dfr.id.dataFetchedClassName(), dfr.dataFetcherData.fetcher()))));

        Map<GraphQlDataFetcherDiscoveryModel.DataFetcherData.DataFetcherAssignment, DataFetcher> fetchers = MapFunctions.CollectMapRidDuplicates(
                ctx.mimeTypeDefinitionContext().dataFetchers()
                        .stream().flatMap(dfr -> dfr.dataFetcherData.field().stream().map(dfa -> Map.entry(dfa, dfr.dataFetcherData.fetcher()))));

        // federated data fetcher - graphql recursively calls for each field, then check for each field for each ty
        codeRegistryBuilder.defaultDataFetcher(environment -> new DataFetcher<Object>() {

            record SourceResult(@Nullable Object obj) {}

            Optional<SourceResult> getBySource(DataFetchingEnvironment dfe, String tn) {
                return Optional.ofNullable(dfe.getSource())
                        .flatMap(s -> {
                            try {
//                                TODO: why does !containsKey fail? Is this a problem?
                                if (s instanceof Map m && m.get(dfe.getField().getName()) != null) {
                                    return Optional.of(new SourceResult(m.get(dfe.getField().getName())));
                                } else if (isNotSourceClass(dfe, tn, s)) {
                                    return Optional.empty();
                                } else {
                                    var f = s.getClass().getDeclaredField(dfe.getField().getName());
                                    f.trySetAccessible();
                                    return Optional.of(new SourceResult(f.get(s)));
                                }
                            } catch (Exception ex) {
                                log.debug("{}", ex.getMessage(), ex);
                            }

                            return Optional.empty();
                        });
            }

            private boolean isNotSourceClass(DataFetchingEnvironment dfe, String tn, Object s) {
                return !Objects.equals(tn, s.getClass().getSimpleName())
                       && !Objects.equals(getParentTypeName(dfe), s.getClass().getSimpleName());
            }

            @Override
            public Object get(DataFetchingEnvironment environment) {

                var tn = getTypeName(environment);

                return getBySource(environment, tn)
                        .orElseGet(() -> {
                            return Optional.ofNullable(fetchers.get(new GraphQlDataFetcherDiscoveryModel.DataFetcherData.DataFetcherAssignment(tn, environment.getField().getName())))
                                    .or(() -> Optional.ofNullable(fetchersByTy.get(tn)))
                                    .flatMap(df -> {
                                        try {
                                            return Optional.of(new SourceResult(df.get(environment)));
                                        } catch (Exception e) {
                                            log.error("{}", e.getMessage(), e);
                                            return Optional.empty();
                                        }
                                    })
                                    .orElseGet(() -> {
                                        var fields = environment.getMergedField().getFields()
                                                .stream()
                                                .flatMap(f -> f.getSelectionSet().getSelections().stream()
                                                        .flatMap(s -> s instanceof Field field ? Stream.of(field) : Stream.empty()));

                                        var outMap = new HashMap<String, Object>();
                                        fields.map(e -> Pair.of(e.getName(), null))
                                                .forEach(p -> outMap.put(p.getKey(), p.getValue()));

                                        return new SourceResult(outMap);
                                    });

                        }).obj;


            }

            private static String getTypeName(DataFetchingEnvironment environment) {
                return environment.getFieldType() instanceof GraphQLNamedType  f
                       ? f.getName() : null;
            }

            private static String getParentTypeName(DataFetchingEnvironment environment) {
                return environment.getParentType() instanceof GraphQLNamedType  f
                       ? f.getName() : null;
            }
        });

        return Result.ok(new GraphQlServiceVisitorResponse("Unimplemented error scenario."));
    }

    private static void addDataFetcherField(DataFetcherRegistration dfr, Map.Entry<String, FieldDefinition> f) {
        if (f.getValue().getType() instanceof TypeName tn) {
            dfr.dataFetcherData.field().add(new GraphQlDataFetcherDiscoveryModel.DataFetcherData.DataFetcherAssignment(f.getKey(), f.getValue().getName()));
        } else if (f.getValue().getType() instanceof NonNullType nt && nt.getType() instanceof TypeName tn) {
            dfr.dataFetcherData.field().add(new GraphQlDataFetcherDiscoveryModel.DataFetcherData.DataFetcherAssignment(f.getKey(), f.getValue().getName()));
        }
    }

    private Stream<Map.Entry<String, FieldDefinition>> fieldDefinitionsFor(String typeName, TypeDefinitionRegistry typeDefinitionRegistry,
                                                        ObjectTypeDefinition objectTypeDefinition)  {
        return objectTypeDefinition.getFieldDefinitions()
                .stream()
                .flatMap(f -> fieldDefinitionsForRecursive(typeName, typeDefinitionRegistry, f, objectTypeDefinition));
    }

    private Stream<Map.Entry<String, FieldDefinition>> fieldDefinitionsForRecursive(String typeName, TypeDefinitionRegistry typeDefinitionRegistry,
                                                                                    FieldDefinition parent, ObjectTypeDefinition objectTypeDefinition)  {
        if (parent.getType() instanceof NonNullType n && n.getType() instanceof TypeName name) {
             if(name.getName().equals(typeName))  {
                 return Stream.of(Map.entry(objectTypeDefinition.getName(), parent));
             } else if (typeDefinitionRegistry.getType(name).orElse(null) instanceof ObjectTypeDefinition otd) {
                 // retrieve from type definition registry by name...
                 return fieldDefinitionsFor(typeName, typeDefinitionRegistry, otd)
                         .map(e -> Map.entry(objectTypeDefinition.getName(), e.getValue()));
             }
        } else if (parent.getType() instanceof TypeName tn) {
            if (tn.getName().equals(typeName)) {
                return Stream.of(Map.entry(objectTypeDefinition.getName(), parent));
            } else if (typeDefinitionRegistry.getType(tn.getName()).orElse(null) instanceof ObjectTypeDefinition otd) {
                return fieldDefinitionsFor(typeName, typeDefinitionRegistry, otd)
                        .map(e -> Map.entry(objectTypeDefinition.getName(), e.getValue()));
            }
        }


        return Stream.empty();
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(MimeTypeRegistry mimeTypeRegistry, Context.RegistriesContext ctx) {
        ctx.mimeTypeDefinitionContext().dataFetchers()
                .forEach(mimeTypeRegistry::register);

        return Result.ok(new GraphQlServiceVisitorResponse("Unimplemented error scenario."));
    }


    @NotNull
    private static Result<Map.Entry<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherData>, SingleError> nextDataFetcherItem(
            Map.Entry<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherMetaData> fetcherItem,
            Context.RegistriesContext ctx
    ) {
        try {
            DataFetcher<?> fetcher = fetcherItem.getValue().fetcher().getConstructor().newInstance();
            ctx.ctx().getAutowireCapableBeanFactory().autowireBean(fetcher);
            return Result.ok(Map.entry(
                    fetcherItem.getKey(),
                    new GraphQlDataFetcherDiscoveryModel.DataFetcherData(fetcher, fetcherItem.getValue().template())
            ));
        } catch (InstantiationException |
                 IllegalAccessException |
                 NoSuchMethodException |
                 InvocationTargetException e) {
            log.error("Error when building {}: {}.", fetcherItem.getValue().fetcher().getSimpleName(), e.getMessage());
            return Result.err(SingleError.fromE(e));
        }
    }

    private void registerDataFetcher(
            GraphQLCodeRegistry.Builder codeRegistryBuilder,
            Map.Entry<String, Map<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherData>> nextFetcher,
            Context.RegistriesContext registriesContext
    ) {

        Map<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherData> dataFetchers
                = MapFunctions.CollectMap(nextFetcher.getValue().entrySet().stream());
        registriesContext.mimeTypeDefinitionContext().dataFetchers().addAll(
                dataFetchers.entrySet().stream()
                        .map(e -> new DataFetcherRegistration(e.getKey(), e.getValue()))
                        .toList());

        nextFetcher.getValue()
                .forEach((sourceId, fetcherData) -> {
                    codeRegistryBuilder.dataFetcher(
                            FieldCoordinates.coordinates("Query", sourceId.fieldName()),
                            fetcherData.fetcher()
                    );
                });

    }

    @Override
    public boolean isReloadable() {
        return true;
    }

    public record DataFetcherRegistration(
            DataFetcherSourceId id,
            GraphQlDataFetcherDiscoveryModel.DataFetcherData dataFetcherData
    ) {}
}
