package com.hayden.gateway.compile.compile_in;

import com.hayden.gateway.compile.CompileArgs;
import com.hayden.gateway.compile.CompilerSourceWriter;
import com.hayden.gateway.compile.JavaCompile;
import com.hayden.utilitymodule.result.Result;
import com.netflix.graphql.dgs.codegen.CodeGen;
import com.netflix.graphql.dgs.codegen.CodeGenConfig;
import com.netflix.graphql.dgs.codegen.CodeGenResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class DgsCompileFileProvider implements CompileFileProvider<DgsCompileFileProvider.DgsCompileResult> {


    private final CompilerSourceWriter sourceWriter;

    public record DgsCompileResult(Collection<CompilerSourceWriter.ToCompileFile> compileFiles,
                                   CodeGenResult codeGenResult) implements CompilerSourceWriter.CompileSourceWriterResult {

        @Override
        public void add(Result.AggregateResponse aggregateResponse) {
            if (aggregateResponse instanceof CompilerSourceWriter.CompileSourceWriterResult sourceWriterResult) {
                this.compileFiles.addAll(sourceWriterResult.compileFiles());
            }
        }
    }

    @SneakyThrows
    @Override
    public Result<DgsCompileResult, Result.AggregateError> toCompileFiles(CompileArgs sourceIn) {
        if (sourceIn instanceof JavaCompile.PathCompileArgs pathCompileArgs) {
            if (!sourceIn.cleanPrevious())
                log.error("Could not clean previous before compiling DGS.");
            DgsCompileResult gen = generateDgsToFiles(pathCompileArgs);
            log.info("Found {} DGS compile files.", gen.compileFiles.size());
            return Result.ok(gen);
        } else {
            return Result.err(new Result.StandardAggregateError("Compiler args of invalid type for Dgs compile file provider: %s."
                    .formatted(sourceIn.getClass().getSimpleName())));
        }
    }

    @NotNull
    private DgsCompileResult generateDgsToFiles(JavaCompile.PathCompileArgs sourceIn) {
        CodeGenResult generate = generateDgs(sourceIn);
        DgsCompileResult out = new DgsCompileResult(new ArrayList<>(), generate);
        addTys(out, addJavaDataTypes(generate, sourceIn));
        addTys(out, addJavaDataFetchers(generate, sourceIn));
        addTys(out, addQueries(generate, sourceIn));
        addTys(out, addProjections(generate, sourceIn));
        addTys(out, addConstants(generate, sourceIn));
        return out;
    }

    private void addTys(DgsCompileResult out,
                        Result<DgsCompileResult, Result.Error> toAdd) {
        toAdd.ifPresent(out::add);
    }

    private Result<DgsCompileResult, Result.Error> addProjections(CodeGenResult generate, JavaCompile.PathCompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                dgs -> dgs.getClientProjections().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut,
                c -> new DgsCompileResult(c, generate)
        );
    }

    private Result<DgsCompileResult, Result.Error> addConstants(CodeGenResult generate, JavaCompile.PathCompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                dgs -> dgs.getJavaConstants().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut,
                c -> new DgsCompileResult(c, generate)
        );
    }

    @NotNull
    private CodeGenResult generateDgs(JavaCompile.PathCompileArgs compileWriterIn) {
        CodeGenConfig config = new CodeGenConfig();

        Set<File> collect = Optional.ofNullable(new File(compileWriterIn.compileWriterIn()).listFiles())
                .stream().flatMap(Arrays::stream)
                .collect(Collectors.toSet());

        config.setSchemaFiles(collect);

        config.setOutputDir(Path.of(compileWriterIn.compileWriterIn()));
        config.setPackageName("com.hayden.gateway");
        config.setGenerateClientApiv2(true);

        var codeGen = new CodeGen(config);

        return codeGen.generate();
    }

    private Result<DgsCompileResult, Result.Error> addJavaDataFetchers(CodeGenResult generate, JavaCompile.PathCompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                g -> g.getJavaDataFetchers().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut,
                c -> new DgsCompileResult(c, generate)
        );
    }

    private Result<DgsCompileResult, Result.Error> addQueries(CodeGenResult generate, JavaCompile.PathCompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                g -> g.getJavaQueryTypes().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut,
                c -> new DgsCompileResult(c, generate)
        );
    }

    private Result<DgsCompileResult, Result.Error> addJavaDataTypes(CodeGenResult generate, JavaCompile.PathCompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                g -> g.getJavaDataTypes().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut,
                c -> new DgsCompileResult(c, generate)
        );
    }

}
