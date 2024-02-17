package com.hayden.gateway.compile;

import com.hayden.gateway.graphql.GraphQlServiceRegistration;
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

    public record GraphQlFetcherSourceResult(String typeName, Map<GraphQlServiceRegistration.DataFetcherSourceId, GraphQlServiceRegistration.DataFetcherMetaData> fetchers) {}

    public Collection<GraphQlFetcherSourceResult> dataFetcherClasses(
            Collection<GraphQlServiceRegistration.GraphQlFetcherSource> sources,
            GraphQlServiceRegistration.GraphQlFederatedSchemaSource schemaSource
    ) {
        // write the source to files or copy the files, do codegen where necessary, and then return the params fetchers
        // for the fields.
        return null;
    }

}
