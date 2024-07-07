package com.hayden.gateway.compile;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hayden.gateway.compile.compile_in.ClientCodeCompileFileProvider;
import com.hayden.gateway.compile.compile_in.CompileFileIn;
import com.hayden.gateway.compile.compile_in.DgsCompileFileProvider;
import com.hayden.graphql.models.GraphQlTarget;
import com.hayden.graphql.models.visitor.model.DataSource;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.schema.GraphQlFederatedSchemaSource;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherGraphQlSource;
import com.hayden.utilitymodule.MapFunctions;
import com.hayden.utilitymodule.io.FileUtils;
import com.hayden.utilitymodule.reflection.PathUtil;
import com.hayden.utilitymodule.result.*;
import com.hayden.utilitymodule.result.error.AggregateError;
import com.hayden.utilitymodule.result.error.Error;
import com.hayden.utilitymodule.result.map.ResultCollectors;
import com.hayden.utilitymodule.result.res.Responses;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class DgsCompiler {


    @Value("${dgs.compile-writer.out:dgs-in}")
    private String dgsCompilerIn;

    private final GraphQlCompilerProperties graphQlCompilerProperties;
    private final FlyJavaCompile dgsFlyCompileJava;
    private final FlyJavaCompile clientFlyClientCompileJava;
    private final CompilerSourceWriter compileSourceWriterResult;

    public record GraphQlFetcherSourceResult(String typeName, Map<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherMetaData> fetchers) {}

    public record GraphQlDataFetcherWriteResult(Collection<CompilerSourceWriter.ToCompileFile> results, DataFetcherSourceId sourceId, DataSource dataSource) {}

    public record GraphQlDataFetcherAggregateWriteResult(List<GraphQlDataFetcherWriteResult> source, ClientCodeCompileFileProvider.ClientCodeCompileProvider compileSourceWriterResult)
            implements Responses.AggregateResponse {

        public GraphQlDataFetcherAggregateWriteResult() {
            this(Lists.newArrayList(), new ClientCodeCompileFileProvider.ClientCodeCompileProvider(new ArrayList<>()));
        }

        @Override
        public void add(Agg aggregateResponse) {
            if (aggregateResponse instanceof GraphQlDataFetcherAggregateWriteResult res) {
                this.source().addAll(res.source);
                this.compileSourceWriterResult.compileFiles().addAll(res.compileSourceWriterResult().compileFiles());
                this.compileSourceWriterResult.codeGenResult().putAll(res.compileSourceWriterResult.codeGenResult());
            }
        }
    }

    public record GraphQlModelWriteResult(List<Path> results) implements Responses.AggregateResponse {

        public GraphQlModelWriteResult() {
            this(Lists.newArrayList());
        }

        public GraphQlModelWriteResult(Path result) {
            this(Lists.newArrayList(result));
        }

        @Override
        public void add(Agg aggregateResponse) {
            if (aggregateResponse instanceof GraphQlModelWriteResult res) {
                results.addAll(res.results);
            }
        }
    }

    public record GraphQlFetcherFetcherClassesResult(List<GraphQlFetcherSourceResult> results,
                                                     FlyJavaCompile.CompileAndLoadResult<CompilerSourceWriter.CompileSourceWriterResult> compileLoadResult,
                                                     FlyJavaCompile.CompileAndLoadResult<CompilerSourceWriter.CompileSourceWriterResult> dataFetcherCompileLoadResult)
            implements Responses.AggregateResponse {

        public GraphQlFetcherFetcherClassesResult(FlyJavaCompile.CompileAndLoadResult<CompilerSourceWriter.CompileSourceWriterResult> dataFetcherCompileLoadResult) {
            this(new ArrayList<>(), null, dataFetcherCompileLoadResult);
        }

        public GraphQlFetcherFetcherClassesResult() {
            this(Lists.newArrayList(), null, null);
        }

        public GraphQlFetcherFetcherClassesResult(GraphQlFetcherSourceResult result) {
            this(Lists.newArrayList(result), null, null);
        }

        @Override
        public void add(Agg aggregateResponse) {
            if (aggregateResponse instanceof GraphQlFetcherFetcherClassesResult res) {
                results.addAll(res.results);
            }
        }
    }

    public record GraphQlFetcherFetcherClassesError(Set<Error> errors) implements AggregateError {
        public GraphQlFetcherFetcherClassesError() {
            this(new HashSet<>());
        }
        public GraphQlFetcherFetcherClassesError(Error error) {
            this(Sets.newHashSet(error));
        }
        public GraphQlFetcherFetcherClassesError(Throwable throwable) {
            this(Sets.newHashSet(Error.fromE(throwable)));
        }
        @Override
        public Set<Error> errors() {
            return errors;
        }
    }

    /**
     * @param sources to compile DataFetcher for federation.
     * @param schemaSource to generate the data model using DGS as class files and compile.
     * @return a result type that contains the compiled data fetchers.
     */
    public Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> dataFetcherClasses(
            Collection<DataFetcherGraphQlSource> sources,
            Collection<GraphQlFederatedSchemaSource> schemaSource
    ) {
        // write the source to files or copy the files, do codegen where necessary, and then return the params fetchers
        // for the fields.
        Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> dgsCompile = dgsModelCompile(schemaSource);

        Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> dataFetcherCompile = clientFetchersCompile(sources);

        var result = collectToFetcherClassesResult(dgsCompile, dataFetcherCompile);

        return Result.from(
                    result.orElse(null),
                    result.error()
                );
    }


    private Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> dgsModelCompile(Collection<GraphQlFederatedSchemaSource> schemaSource) {
        return schemaSource.stream()
                .map(nextGraphQlSchema -> {
                    Path outputPath = graphQlCompilerProperties.getSchemaOutput().resolve(nextGraphQlSchema.schemaName());
                    return writeToLocal(outputPath, nextGraphQlSchema.targetType(), nextGraphQlSchema.target());
                })
                .collect(ResultCollectors.from(new GraphQlModelWriteResult(), new GraphQlFetcherFetcherClassesError()))
                .map(ignored -> {
                    var loaded = dgsFlyCompileJava.compileAndLoad(
                            new FlyJavaCompile.PathCompileArgs(
                                    graphQlCompilerProperties.getSchemaOutput().toString(),
                                    graphQlCompilerProperties.getCompilerIn().toString()
                            ));
                    return loaded.flatMapResult(r -> r.writerResult() instanceof DgsCompileFileProvider.DgsCompileResult
                                    ? Result.ok(r)
                                    : Result.err(new GraphQlFetcherFetcherClassesError(Error.fromMessage("Did not recognize compile result."))
                            ))
                            .flatMapResult(ig -> Result.ok(new GraphQlFetcherFetcherClassesResult(loaded.get())))
                            .orElseThrow(() -> new RuntimeException("No other introspection implemented at this time, besides DGS, for schema!"));
                });
    }


    private Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> clientFetchersCompile(Collection<DataFetcherGraphQlSource> sources) {
        return sources.stream()
                .map(dataFetcherGraphQlSource -> {
                    return dataFetcherGraphQlSource.sources().entrySet()
                            .stream()
                            .map(this::writeFetcherClasses)
                            .<Result<GraphQlDataFetcherAggregateWriteResult, GraphQlFetcherFetcherClassesError>>map(Result::cast)
                            .collect(ResultCollectors.from(new GraphQlDataFetcherAggregateWriteResult(), new GraphQlFetcherFetcherClassesError()))
                            .flatMapResult(this::compileAndLoad)
                            .flatMapResult(r -> collectToFetcher(dataFetcherGraphQlSource, r));
                })
                .collect(ResultCollectors.from(new GraphQlFetcherFetcherClassesResult(), new GraphQlFetcherFetcherClassesError()));
    }

    private Result<FlyJavaCompile.CompileAndLoadResult<CompilerSourceWriter.CompileSourceWriterResult>, FlyJavaCompile.CompileAndLoadError> compileAndLoad(
            GraphQlDataFetcherAggregateWriteResult writeResult
    ) {
        return clientFlyClientCompileJava.compileAndLoad(
                new FlyJavaCompile.JavaFilesCompilerArgs(
                        writeResult,
                        graphQlCompilerProperties.getCompilerIn().toString()
                ),
                (provider, clzzes) -> {
                    Map<String, List<Class<?>>> clzzesByType = MapFunctions.CollectMapGroupBy(
                            clzzes.stream()
                                    // TODO: is assignable from datafetcher or has DgsData
//                                    .filter(DataFetcher.class::isAssignableFrom)
                                    .map(c -> Map.entry(c.getSimpleName(), c))
                    );
                    var agg = new FlyJavaCompile.CompileAndLoadError(Sets.newHashSet());
                    if (provider instanceof ClientCodeCompileFileProvider.ClientCodeCompileProvider fileProvider) {
                        return writeResult.source()
                                .stream()
                                .flatMap(w -> retrieveDataFetchers(w, clzzesByType))
                                .collect(ResultCollectors.from(
                                        provider, agg,
                                        r -> {
                                            fileProvider.codeGenResult().put(r.get().getKey(), r.get().getValue());
                                            return Optional.of(fileProvider);
                                        },
                                        e -> {
                                            if (e.isError()) {
                                                agg.addError(e.error());
                                                return Optional.of(agg);
                                            }
                                            return Optional.empty();
                                        }));
                    }

                    agg.addError(Error.fromMessage("Did not recognize file provider: %s.".formatted(provider.getClass().getName())));
                    return Result.err(agg);
                });
    }

    private static @NotNull Stream<Result<Map.Entry<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherMetaData>, Error>> retrieveDataFetchers(
            GraphQlDataFetcherWriteResult w,
            Map<String, List<Class<?>>> clzzesByType
    ) {
        return clzzesByType.get(w.sourceId.dataFetcherTypeName())
                .stream()
                .flatMap(DgsCompiler::toDataFetcherClzz)
                .map(s -> s.map(resultObj -> Map.entry(
                        w.sourceId,
                        new GraphQlDataFetcherDiscoveryModel.DataFetcherMetaData(
                                resultObj,
                                w.dataSource.dataTemplate()
                        )
                )))
                .findFirst()
                .stream();
    }

    private static @NotNull Stream<Result<Class<? extends DataFetcher<?>>, Error>> toDataFetcherClzz(Class<?> c) {
        try {
            return Stream.of(Result.ok((Class<? extends DataFetcher<?>>) c));
        } catch (ClassCastException dataFetcherClass) {
            return Stream.of(Result.err(Error.fromE(dataFetcherClass)));
        }
    }

    private static Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> collectToFetcherClassesResult(Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> dgsCompile, Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> dataFetcherCompile) {
        GraphQlFetcherFetcherClassesResult fetcherClassesResult = new GraphQlFetcherFetcherClassesResult(
                new ArrayList<>(),
                dgsCompile.map(r -> r.compileLoadResult)
                        .orElse(null),
                dataFetcherCompile.map(d -> d.dataFetcherCompileLoadResult)
                        .orElse(null)
        );
        return Stream.of(dgsCompile, dataFetcherCompile)
                .collect(ResultCollectors.from(fetcherClassesResult, new GraphQlFetcherFetcherClassesError()));
    }
    private Result<ClientCodeCompileFileProvider.ClientCodeCompileProvider, GraphQlFetcherFetcherClassesError> writeCompileFiles(DataSource dataFetcher) {
        if (dataFetcher.sourceMetadata().targetType() == GraphQlTarget.String) {
            return compileSourceWriterResult.writeFiles(
                            dataFetcher,
                            o -> Stream.of(new CompileFileIn.ClientFileCompileFileIn(o)),
                            ClientCodeCompileFileProvider.ClientCodeCompileProvider::new,
                            Paths.get(graphQlCompilerProperties.getCompilerIn().toString(), PathUtil.fromPackage(dataFetcher.sourceMetadata().packageName())).toString()
                    )
                    .cast();
        }

        return Result.err(new GraphQlFetcherFetcherClassesError(new RuntimeException("Found unsupported target type: %s for %s."
                .formatted(dataFetcher.sourceMetadata().targetType(), dataFetcher.id()))));
    }

    private static Result<GraphQlModelWriteResult, GraphQlFetcherFetcherClassesError> writeToLocal(Path outputPath, GraphQlTarget graphQlTarget, String target) {
        if (graphQlTarget == GraphQlTarget.String) {
            FileUtils.writeToFile(target, outputPath);
            return Result.ok(
                    new GraphQlModelWriteResult(outputPath)
            );
        } else {
            try {
                Files.copy(new File(target).toPath(), outputPath);
                return Result.ok(
                        new GraphQlModelWriteResult(outputPath)
                );
            } catch (IOException e) {
                return Result.err(
                        new GraphQlFetcherFetcherClassesError(e));
            }
        }
    }

    private static Result<GraphQlFetcherFetcherClassesResult, GraphQlFetcherFetcherClassesError> collectToFetcher(DataFetcherGraphQlSource dataFetcherGraphQlSource, FlyJavaCompile.CompileAndLoadResult<CompilerSourceWriter.CompileSourceWriterResult> r) {
        if (r.writerResult() instanceof ClientCodeCompileFileProvider.ClientCodeCompileProvider clientCodeCompileProvider)
            return Result.ok(new GraphQlFetcherFetcherClassesResult(
                    new GraphQlFetcherSourceResult(
                            dataFetcherGraphQlSource.typeName(),
                            clientCodeCompileProvider.codeGenResult())
            ));
        return Result.err(new GraphQlFetcherFetcherClassesError(Error.fromMessage("Writer result was of unknown type when compiling client data fetchers..")));
    }

    private Result<GraphQlDataFetcherAggregateWriteResult, Error> writeFetcherClasses(Map.Entry<DataFetcherSourceId, DataSource> sourceIdDataFetcher) {
        var dataFetcher = sourceIdDataFetcher.getValue();
        return writeCompileFiles(dataFetcher)
                .map(writeResult -> Map.entry(writeResult, new GraphQlDataFetcherWriteResult(writeResult.compileFiles(), sourceIdDataFetcher.getKey(), sourceIdDataFetcher.getValue())))
                .map(l -> new GraphQlDataFetcherAggregateWriteResult(Lists.newArrayList(l.getValue()), l.getKey()));
    }

}
