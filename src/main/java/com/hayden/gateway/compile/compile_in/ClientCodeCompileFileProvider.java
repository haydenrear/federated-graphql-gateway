package com.hayden.gateway.compile.compile_in;

import com.hayden.gateway.compile.CompileArgs;
import com.hayden.gateway.compile.CompilerSourceWriter;
import com.hayden.gateway.compile.FlyJavaCompile;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.utilitymodule.result.Agg;
import com.hayden.utilitymodule.result.error.AggregateError;
import com.hayden.utilitymodule.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientCodeCompileFileProvider implements CompileFileProvider<ClientCodeCompileFileProvider.ClientCodeCompileProvider> {

    @Override
    public Result<ClientCodeCompileProvider, FlyJavaCompile.CompileAndLoadError> toCompileFiles(CompileArgs sourceIn) {
        if (sourceIn instanceof FlyJavaCompile.JavaFilesCompilerArgs javaFilesCompilerArgs) {
            var paths = javaFilesCompilerArgs.compileWriterIn().source().stream()
                    .flatMap(w -> w.results().stream().map(p -> Map.entry(w.dataSource().sourceMetadata().packageName(), p)))
                    .map(p -> new CompilerSourceWriter.ToCompileFile(p.getValue().file(), p.getKey()))
                    .collect(Collectors.toCollection(ArrayList::new));

            return Result.ok(new ClientCodeCompileProvider(paths));
        }
        return Result.err(new FlyJavaCompile.CompileAndLoadError("Compile args was not of valid type: %s.".formatted(sourceIn.getClass().getName())));
    }

    public record ClientCodeCompileProvider(
            Collection<CompilerSourceWriter.ToCompileFile> compileFiles,
            Map<DataFetcherSourceId, GraphQlDataFetcherDiscoveryModel.DataFetcherMetaData> codeGenResult
    ) implements CompilerSourceWriter.CompileSourceWriterResult {

        public ClientCodeCompileProvider(Collection<CompilerSourceWriter.ToCompileFile> compileFiles) {
            this(compileFiles, new HashMap<>());
        }

        @Override
        public void add(Agg aggregateResponse) {
            if (aggregateResponse instanceof CompilerSourceWriter.CompileSourceWriterResult sourceWriterResult) {
                this.compileFiles.addAll(sourceWriterResult.compileFiles());
            }
        }
    }

}
