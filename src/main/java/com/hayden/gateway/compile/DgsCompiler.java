package com.hayden.gateway.compile;

import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.schema.GraphQlFederatedSchemaSource;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherGraphQlSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DgsCompiler {

    @Value("${dgs.compile-writer.out:dgs-in}")
    private String dgsCompilerIn;

    private final CompilerSourceWriter sourceWriter;

    public record GraphQlFetcherSourceResult(String typeName, Map<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherMetaData> fetchers) {}

    public Collection<GraphQlFetcherSourceResult> dataFetcherClasses(
            Collection<DataFetcherGraphQlSource> sources,
            Collection<GraphQlFederatedSchemaSource> schemaSource
    ) {
        // write the source to files or copy the files, do codegen where necessary, and then return the params fetchers
        // for the fields.
        return null;
    }

}
