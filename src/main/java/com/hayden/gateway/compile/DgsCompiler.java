package com.hayden.gateway.compile;

import com.hayden.graphql.models.GraphQlTarget;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.schema.GraphQlFederatedSchemaSource;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherGraphQlSource;
import com.hayden.utilitymodule.io.FileUtils;
import com.hayden.utilitymodule.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DgsCompiler {

    @Value("${dgs.compile-writer.out:dgs-in}")
    private String dgsCompilerIn;

    private final CompilerSourceWriter sourceWriter;
    private final GraphQlCompilerProperties graphQlCompilerProperties;

    public record GraphQlFetcherSourceResult(String typeName, Map<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherMetaData> fetchers) {}

    public record GraphQlFetcherFetcherClassesResult(List<GraphQlFetcherSourceResult> results) implements Result.AggregateResponse {
        @Override
        public void add(Result.AggregateResponse aggregateResponse) {
            if (aggregateResponse instanceof GraphQlFetcherFetcherClassesResult res) {
                results.addAll(res.results);
            }
        }
    }
    public record GraphQlFetcherFetcherClassesError(Set<Result.Error> errors) implements Result.AggregateError {
        public GraphQlFetcherFetcherClassesError() {
            this(new HashSet<>());
        }
        @Override
        public Set<Result.Error> errors() {
            return errors;
        }
    }

    public Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> dataFetcherClasses(
            Collection<DataFetcherGraphQlSource> sources,
            Collection<GraphQlFederatedSchemaSource> schemaSource
    ) {
        // write the source to files or copy the files, do codegen where necessary, and then return the params fetchers
        // for the fields.
        schemaSource.stream()
                .map(g -> {
                    Path outputPath = graphQlCompilerProperties.getSchemaOutput().resolve(g.schemaName());
                    if (g.targetType() == GraphQlTarget.String) {
                        FileUtils.writeToFile(g.target(), outputPath);
                        return Result.ok(null);
                    } else {
                        try {
                            Files.copy(new File(g.target()).toPath(), outputPath);
                            return Result.ok(null);
                        } catch (IOException e) {
                            return Result.err(e);
                        }
                    }
                });
        sources.forEach(d -> {
            d.sources().forEach((sourceId, dataFetcher) -> {
                dataFetcher.sourceMetadata().targetType();
            });
        });
        return null;
    }


}
