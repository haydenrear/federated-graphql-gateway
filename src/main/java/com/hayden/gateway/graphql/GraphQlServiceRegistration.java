package com.hayden.gateway.graphql;

import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.utilitymodule.MapFunctions;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.MimeType;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * TODO: create a request for delete of schema from TypeRegistry.
 * @param serviceId the serviceId of this graph-ql service. This will be the same for deploying multiple services of
 *                  the same type.
 * @param source the GraphQlSource schema provided.
 * @param fetcherSource the source code for the params fetchers so this service id can be federated.
 */
@Slf4j
public record GraphQlServiceRegistration(String serviceId,
                                         GraphQlFederatedSchemaSource source,
                                         Collection<GraphQlFetcherSource> fetcherSource)
        implements GraphQlServiceApiVisitor {

    public record DataFetcherTemplate(String query, Set<String> required, Set<String> optional) {
        public String toQuery(Map<String, String> params) {
            String queryCreated = query;
            for (var p : params.entrySet()) {
                queryCreated = queryCreated.replace("{%s}".formatted(p.getKey()), p.getValue());
            }
            return queryCreated;
        }
    }
    public record DataFetcherMetaData(Class<? extends DataFetcher<?>> fetcher, DataFetcherTemplate template)  {}

    public record DataFetcherData(DataFetcher<?> fetcher, DataFetcherTemplate template)  {}


    @Override
    public void visit(TypeDefinitionRegistry typeDefinitionRegistry,
                      RegistriesContext ctx) {
        SchemaParser schemaParser = new SchemaParser();
        this.source().sources().targets().forEach((schema, target) -> {
            if (schema.equals(GraphQlServiceRegistration.GraphQlTarget.String)) {
                target.stream()
                        .map(schemaParser::parse)
                        .peek(newTypes -> this.deleteCurrent(newTypes, typeDefinitionRegistry))
                        .forEach(typeDefinitionRegistry::merge);
            }
        });
    }

    /**
     * Makes the addition to the TypeDefinitionRegistry idempotent, so that if a service updates their schema then
     * when the timer runs out and it's updated it will clear out the old implementation and add the new implementation.
     * @param toMergeData
     */
    private void deleteCurrent(TypeDefinitionRegistry toMergeData, TypeDefinitionRegistry toMergeInto) {
        toMergeData.types()
                .forEach((typeKey, typeValue) -> toMergeInto.getType(typeKey)
                        .ifPresent(td -> toMergeInto.remove(typeKey, td))
                );
    }

    @Override
    public void visit(GraphQLCodeRegistry.Builder codeRegistryBuilder,
                      TypeDefinitionRegistry registry,
                      RegistriesContext context) {
        context.codegenContext().javaCompiler()
                .dataFetcherClasses(this.fetcherSource, this.source)
                .stream()
                .map(result -> Map.entry(
                        result.typeName(),
                        MapFunctions.CollectMap(
                                result.fetchers().entrySet().stream()
                                        .flatMap(GraphQlServiceRegistration::nextDataFetcherItem)
                        )
                ))
                .forEach(nextFetcher -> registerDataFetcher(codeRegistryBuilder, nextFetcher, context));
    }

    @Override
    public void visit(MimeTypeRegistry mimeTypeRegistry, RegistriesContext ctx) {
        ctx.mimeTypeDefinitionContext().dataFetchers()
                .forEach(mimeTypeRegistry::register);
    }

    @NotNull
    private static Stream<Map.Entry<DataFetcherSourceId, DataFetcherData>> nextDataFetcherItem(
            Map.Entry<DataFetcherSourceId, DataFetcherMetaData> fetcherItem
    ) {
        try {
            return Stream.of(Map.entry(
                    fetcherItem.getKey(),
                    new DataFetcherData(fetcherItem.getValue().fetcher().getConstructor().newInstance(), fetcherItem.getValue().template())
            ));
        } catch (InstantiationException |
                 IllegalAccessException |
                 NoSuchMethodException |
                 InvocationTargetException e) {
            log.error("Error when building {}: {}.", fetcherItem.getValue().fetcher().getSimpleName(), e.getMessage());
        }
        return Stream.empty();
    }

    private GraphQLCodeRegistry.Builder registerDataFetcher(
            GraphQLCodeRegistry.Builder codeRegistryBuilder,
            Map.Entry<String, Map<DataFetcherSourceId, DataFetcherData>> nextFetcher,
            RegistriesContext registriesContext
    ) {
        Map<DataFetcherSourceId, DataFetcherData> dataFetchers
                = MapFunctions.CollectMap(nextFetcher.getValue().entrySet().stream());
        registriesContext.mimeTypeDefinitionContext().dataFetchers().addAll(
                dataFetchers.entrySet().stream()
                        .map(e -> new MimeTypeRegistry.DataFetcherRegistration(e.getKey(), e.getValue()))
                        .toList()
        );
        return codeRegistryBuilder.dataFetchers(
                nextFetcher.getKey(),
                MapFunctions.CollectMap(dataFetchers.entrySet().stream()
                        .map(d -> Map.entry(d.getKey().fieldName, d.getValue().fetcher())))
        );
    }


    public record Sources(Map<GraphQlTarget, List<String>> targets) {}

    /**
     * A services GraphQl schema to be proxied using GraphQl federation.
     * @param sources
     */
    public record GraphQlFederatedSchemaSource(Sources sources) {
        public GraphQlFederatedSchemaSource(Map<GraphQlTarget, List<String>> targets) {
            this(new Sources(targets));
        }
    }


    /**
     * A services GraphQl source classes used in the fetchers or schema.
     * @param sources
     */
    public record GraphQlFederatedModelsSource(Sources sources) {
        public GraphQlFederatedModelsSource(Map<GraphQlTarget, List<String>> targets) {
            this(new Sources(targets));
        }
    }

    /**
     * The params is pulled from the service, compiled and then can use the queries.
     * @param sources The source code for the params fetcher that will be compiled
     * @param typeName The base type name for the sources.
     */
    public record GraphQlFetcherSource(String typeName, Map<DataFetcherSourceId, DataFetcherSource> sources, DataFetcherTemplate template) {}


    public enum GraphQlTarget {
        FileLocation, String
    }

    /**
     * @param packageName
     */
    public record SourceMetadata(String packageName, String target, GraphQlTarget targetType) {
        public SourceMetadata(String target, GraphQlTarget targetType) {
            this(null, target, targetType);
        }
    }

    /**
     * Data fetcher can be provided as a file location or the source code as a string.
     * @param dataFetcherSourceType
     * @param target
     */
    public record DataFetcherSource(SourceMetadata sourceMetadata) {
        public DataFetcherSource(GraphQlTarget targetType, String target) {
            this(new SourceMetadata(target, targetType));
        }
    }

    public enum DataFetcherSourceType {
        GraphQl, DgsComponentJava, DataFetcherJava
    }

    /**
     * Each params fetcher has a field name and the source. The source can be provided in Java or query dsl, which will
     * then be compiled to Java classes and loaded into the program.
     * @param dataFetcherSourceType
     * @param fieldName
     */
    public record DataFetcherSourceId(DataFetcherSourceType dataFetcherSourceType, String fieldName, MimeType mimeType) {}


}
