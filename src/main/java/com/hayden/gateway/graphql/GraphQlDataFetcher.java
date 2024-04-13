package com.hayden.gateway.graphql;

import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.graphql.models.GraphQlTarget;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.utilitymodule.MapFunctions;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * TODO: create a request for delete of schema from TypeRegistry.
 * @param serviceId the serviceId of this graph-ql service. This will be the same for deploying multiple services of
 *                  the same type.
 * @param source the GraphQlSource schema provided.
 * @param fetcherSource the source code for the params fetchers so this service id can be federated.
 */
@Slf4j
public record GraphQlDataFetcher(@Delegate GraphQlDataFetcherDiscoveryModel model, String id, ContextCallback removeCallback)
        implements GraphQlServiceApiVisitor {

    public GraphQlDataFetcher(GraphQlDataFetcherDiscoveryModel model) {
        this(model, model.serviceId().host(), new ContextCallback());
    }

    @Override
    public void remove() {
        removeCallback.callback.accept(id, model.serviceId());
    }

    @Override
    public void visit(TypeDefinitionRegistry typeDefinitionRegistry,
                      Context.RegistriesContext ctx) {
        SchemaParser schemaParser = new SchemaParser();
        this.model.source()
                .forEach(sourceItem -> {
                    var schema = sourceItem.targetType();
                    if (schema.equals(GraphQlTarget.String)) {
                        Optional.ofNullable(sourceItem.target())
                                .stream()
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
                      Context.RegistriesContext context) {
        context.codegenContext().javaCompiler()
                .dataFetcherClasses(this.model.fetcherSource(), this.model.source())
                .stream()
                .map(result -> Map.entry(
                        result.typeName(),
                        MapFunctions.CollectMap(
                                result.fetchers().entrySet().stream()
                                        .flatMap(GraphQlDataFetcher::nextDataFetcherItem)
                        )
                ))
                .forEach(nextFetcher -> registerDataFetcher(codeRegistryBuilder, nextFetcher, context));
    }

    @Override
    public void visit(MimeTypeRegistry mimeTypeRegistry, Context.RegistriesContext ctx) {
        ctx.mimeTypeDefinitionContext().dataFetchers()
                .forEach(mimeTypeRegistry::register);
    }


    @NotNull
    private static Stream<Map.Entry<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherData>> nextDataFetcherItem(
            Map.Entry<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherMetaData> fetcherItem
    ) {
        try {
            return Stream.of(Map.entry(
                    fetcherItem.getKey(),
                    new GraphQlDataFetcherDiscoveryModel.DataFetcherData(fetcherItem.getValue().fetcher().getConstructor().newInstance(), fetcherItem.getValue().template())
            ));
        } catch (InstantiationException |
                 IllegalAccessException |
                 NoSuchMethodException |
                 InvocationTargetException e) {
            log.error("Error when building {}: {}.", fetcherItem.getValue().fetcher().getSimpleName(), e.getMessage());
        }
        return Stream.empty();
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
                        .toList()
        );
        codeRegistryBuilder.dataFetchers(
                nextFetcher.getKey(),
                MapFunctions.CollectMap(dataFetchers.entrySet().stream()
                        .map(d -> Map.entry(d.getKey().fieldName(), d.getValue().fetcher())))
        );
    }

    public record DataFetcherRegistration(
            DataFetcherSourceId id,
            GraphQlDataFetcherDiscoveryModel.DataFetcherData dataFetcherData
    ) {}
}
