package com.hayden.gateway.compile.compile_in;

import com.hayden.gateway.compile.CompilerSourceWriter;
import com.hayden.gateway.compile.JavaCompile;
import com.hayden.utilitymodule.result.Result;
import com.netflix.graphql.dgs.codegen.CodeGen;
import com.netflix.graphql.dgs.codegen.CodeGenConfig;
import com.netflix.graphql.dgs.codegen.CodeGenResult;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class DgsCompileFileProvider implements CompileFileProvider {


    private final CompilerSourceWriter sourceWriter;

    @Override
    public Collection<CompilerSourceWriter.ToCompileFile> toCompileFiles(JavaCompile.CompileArgs sourceIn) {
        return generateDgsToFiles(sourceIn);
    }


    @NotNull
    private Collection<CompilerSourceWriter.ToCompileFile> generateDgsToFiles(JavaCompile.CompileArgs sourceIn) {
        CodeGenResult generate = generateDgs(sourceIn);
        Collection<CompilerSourceWriter.ToCompileFile> out = new ArrayList<>();
        addTys(out, addJavaDataTypes(generate, sourceIn));
        addTys(out, addJavaDataFetchers(generate, sourceIn));
        addTys(out, addQueries(generate, sourceIn));
        addTys(out, addProjections(generate, sourceIn));
        addTys(out, addConstants(generate, sourceIn));
        return out;
    }

    private void addTys(Collection<CompilerSourceWriter.ToCompileFile> out,
                        Result<Collection<CompilerSourceWriter.ToCompileFile>, Result.Error> toAdd) {
        toAdd.map(out::addAll);
    }

    private Result<Collection<CompilerSourceWriter.ToCompileFile>, Result.Error> addProjections(CodeGenResult generate, JavaCompile.CompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                dgs -> dgs.getClientProjections().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut
        );
    }

    private Result<Collection<CompilerSourceWriter.ToCompileFile>, Result.Error> addConstants(CodeGenResult generate, JavaCompile.CompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                dgs -> dgs.getJavaConstants().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut
        );
    }

    @NotNull
    private CodeGenResult generateDgs(JavaCompile.CompileArgs compileWriterIn) {
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

    private Result<Collection<CompilerSourceWriter.ToCompileFile>, Result.Error> addJavaDataFetchers(CodeGenResult generate, JavaCompile.CompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                g -> g.getJavaDataFetchers().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut
        );
    }

    private Result<Collection<CompilerSourceWriter.ToCompileFile>, Result.Error> addQueries(CodeGenResult generate, JavaCompile.CompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                g -> g.getJavaQueryTypes().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut
        );
    }

    private Result<Collection<CompilerSourceWriter.ToCompileFile>, Result.Error> addJavaDataTypes(CodeGenResult generate, JavaCompile.CompileArgs compileWriterOut) {
        return sourceWriter.writeFiles(
                generate,
                g -> g.getJavaDataTypes().stream().map(CompileFileIn.JavaPoetCompileFileIn::new),
                compileWriterOut
        );
    }

}
