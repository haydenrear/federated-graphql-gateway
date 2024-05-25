package com.hayden.gateway.graphql;

import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.graphql.models.GraphQlTarget;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.utilitymodule.MapFunctions;
import com.hayden.utilitymodule.result.error.Error;
import com.hayden.utilitymodule.result.Result;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

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
    public boolean remove() {
        removeCallback.callback.accept(id().host().host(), model.serviceId());
        return true;
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(TypeDefinitionRegistry typeDefinitionRegistry,
                      Context.RegistriesContext ctx) {
        SchemaParser schemaParser = new SchemaParser();
        this.model.source()
                .forEach(sourceItem -> {
                    var schema = sourceItem.targetType();
                    if (schema == GraphQlTarget.String) {
                        Optional.ofNullable(sourceItem.target())
                                .stream()
                                .map(schemaParser::parse)
                                .peek(newTypes -> this.deleteCurrent(newTypes, typeDefinitionRegistry))
                                .forEach(typeDefinitionRegistry::merge);
                    }
                });

        return Result.ok(new GraphQlServiceVisitorResponse("Not implemented error"));
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
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(GraphQLCodeRegistry.Builder codeRegistryBuilder,
                      TypeDefinitionRegistry registry,
                      Context.RegistriesContext context) {
        context.codegenContext().javaCompiler()
                .dataFetcherClasses(this.model.fetcherSource(), this.model.source())
                .stream()
                .flatMap(g -> g.results().stream())
                .map(result -> Map.entry(
                        result.typeName(),
                        MapFunctions.CollectMap(result.fetchers().entrySet().stream()
                                .flatMap(f -> nextDataFetcherItem(f, context).stream()))
                ))
                .forEach(nextFetcher -> registerDataFetcher(codeRegistryBuilder, nextFetcher, context));

        return Result.ok(new GraphQlServiceVisitorResponse("Unimplemented error scenario."));
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(MimeTypeRegistry mimeTypeRegistry, Context.RegistriesContext ctx) {
        ctx.mimeTypeDefinitionContext().dataFetchers()
                .forEach(mimeTypeRegistry::register);

        return Result.ok(new GraphQlServiceVisitorResponse("Unimplemented error scenario."));
    }


    @NotNull
    private static Result<Map.Entry<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherData>, Error> nextDataFetcherItem(
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
            return Result.err(e);
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
                        .toList()
        );

        nextFetcher.getValue()
                .forEach((sourceId, fetcherData) -> codeRegistryBuilder.dataFetcher(
                        FieldCoordinates.coordinates("Query", sourceId.fieldName()),
                        fetcherData.fetcher()
                ));

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
