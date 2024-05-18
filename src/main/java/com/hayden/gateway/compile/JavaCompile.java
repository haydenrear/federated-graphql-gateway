package com.hayden.gateway.compile;

import com.hayden.gateway.compile.compile_in.CompileFileProvider;
import com.hayden.utilitymodule.result.Agg;
import com.hayden.utilitymodule.result.error.AggregateError;
import com.hayden.utilitymodule.result.res.Responses;
import com.hayden.utilitymodule.result.Result;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class JavaCompile {

    private final CompileFileProvider<? extends CompilerSourceWriter.CompileSourceWriterResult> dgsCompileFileProvider;



    public record JavaFilesCompilerArgs(DgsCompiler.GraphQlDataFetcherAggregateWriteResult compileWriterIn, String compilerIn,
                                        @Nullable String compilerOut) implements CompileArgs {

        public JavaFilesCompilerArgs(DgsCompiler.GraphQlDataFetcherAggregateWriteResult compileWriterIn, String compilerIn) {
            this(compileWriterIn, compilerIn, null);
        }
    }

    public record PathCompileArgs(String compileWriterIn, String compilerIn, @Nullable String compilerOut) implements CompileArgs {
        public PathCompileArgs(String compileWriterIn, String compilerIn) {
            this(compileWriterIn, compilerIn, null);
        }
    }

    public record CompileAndLoadResult<T extends CompilerSourceWriter.CompileSourceWriterResult>(List<Class<?>> classesCreated, T writerResult)
            implements Responses.AggregateResponse {

        @Override
        public void add(Agg aggregateResponse) {
            if (aggregateResponse instanceof CompileAndLoadResult<?> compileAndLoadResult)
                this.classesCreated.addAll(compileAndLoadResult.classesCreated);
        }
    }

    public interface CompileSourceWriterProcessor<T extends CompilerSourceWriter.CompileSourceWriterResult> {
        Result<T, AggregateError> process(T t, Collection<Class<?>> clzzes);
    }

    public Result<CompileAndLoadResult<CompilerSourceWriter.CompileSourceWriterResult>, AggregateError> compileAndLoad(
            CompileArgs args,
            @Nullable CompileSourceWriterProcessor<CompilerSourceWriter.CompileSourceWriterResult> processor
    ) {
        var found = dgsCompileFileProvider.toCompileFiles(args);

        if (doCompilation(found.stream().flatMap(f -> f.compileFiles().stream()).map(CompilerSourceWriter.ToCompileFile::file).toList())) {
            log.info("Starting compilation... for path {}.", args.compilerIn());
            return found.map(c -> {
                        List<Class<?>> clzzes = compileFilesClzzes(args, c);
                        var compileSourceWriterResultAggregateErrorResult = Optional.ofNullable(processor)
                                .map(p -> p.process(c, clzzes))
                                .orElse(Result.err(new AggregateError.StandardAggregateError("Could not process.")));
                        return Result.from(new CompileAndLoadResult<>(
                                        clzzes,
                                        compileSourceWriterResultAggregateErrorResult
                                                .orElse(c)
                                ),
                                compileSourceWriterResultAggregateErrorResult.error()
                        );
                    })
                    .orElse(Result.<CompileAndLoadResult<CompilerSourceWriter.CompileSourceWriterResult>, AggregateError>err(new AggregateError.StandardAggregateError(
                            "Compile result was not found from file provider.")));
        }
        return Result.err(new AggregateError.StandardAggregateError("No compile sources found."));
    }


    public Result<CompileAndLoadResult<CompilerSourceWriter.CompileSourceWriterResult>, AggregateError> compileAndLoad(CompileArgs args) {
        return compileAndLoad(args, null);
    }

    private @NotNull List<Class<?>> compileFilesClzzes(CompileArgs args, CompilerSourceWriter.CompileSourceWriterResult c) {
        List<Class<?>> clzzes = c.compileFiles().stream()
                .map(CompilerSourceWriter.ToCompileFile::packageName)
                .distinct()
                .map(nextPackageName -> {
                    String packageName = nextPackageName.replace(".", "/");
                    var compileOutPath = getCompileOutPath(args, nextPackageName, packageName);
                    return Pair.of(Path.of(args.compilerIn(), packageName).toFile(), compileOutPath.toFile().toPath());
                })
                .flatMap(packageNames -> {
                    try {
                        return loadPackage(packageNames);
                    } catch (
                            MalformedURLException e) {
                        log.error("Error loading with URL class loader with error {}.",
                                e.getMessage());
                    }
                    return Stream.empty();
                }).collect(Collectors.toList());
        return clzzes;
    }

    @NotNull
    private static Path getCompileOutPath(CompileArgs args, String nextPackageName, String packageName) {
        var compileOutPath = Optional.ofNullable(args.compilerOut())
                .map(File::new)
                .orElse(new File("build/classes/java/main/%s".formatted(packageName)))
                .toPath();
        if (!compileOutPath.toFile().exists())
            Assert.isTrue(compileOutPath.toFile().mkdirs(), "Could not make %s directory".formatted(nextPackageName));
        return compileOutPath;
    }

    @NotNull
    private Stream<Class<?>> loadPackage(Pair<File, Path> packageNames) throws MalformedURLException {
        var compileDirectory = packageNames.getLeft();
        var buildDirectory = packageNames.getRight();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{new File("./").toURI().toURL()});
        deleteJavaFiles(compileDirectory);
        return loadClasses(compileDirectory, classLoader, buildDirectory);
    }


    @NotNull
    private Stream<Class<?>> loadClasses(File compileDirectory, URLClassLoader classLoader, Path buildDirectory) {
        return Optional.ofNullable(compileDirectory.listFiles())
                .stream()
                .flatMap(Arrays::stream)
                .filter(f -> f.getName().endsWith(".class"))
                .flatMap(nextClassToLoad -> {
                    try {
                        return loadNextClass(
                                nextClassToLoad,
                                classLoader,
                                Path.of(buildDirectory.toString(), nextClassToLoad.getName())
                        );
                    } catch (IOException |
                            ClassNotFoundException e) {
                        log.error("Error loading {} with error {}.", nextClassToLoad.getName(),
                                e.getMessage());
                    }
                    return Stream.empty();
                });
    }

    private static void deleteJavaFiles(File compileDirectory) {
        Optional.ofNullable(compileDirectory.listFiles())
                .stream()
                .flatMap(Arrays::stream)
                .filter(f -> f.getName().endsWith(".java"))
                .filter(f -> !f.delete())
                .forEach(f -> log.error("Error - could not delete {}.", f.getName()));
    }

    @NotNull
    private Stream<Class<?>> loadNextClass(File compiledFile,
                                           URLClassLoader classLoader,
                                           Path buildDirectory) throws IOException, ClassNotFoundException {
        Files.move(
                compiledFile.toPath(),
                buildDirectory,
                StandardCopyOption.REPLACE_EXISTING
        );
        Class<?> loadedClass = classLoader.loadClass(getQualifiedClassNameFromPath(buildDirectory));
        return Stream.of(loadedClass);
    }

    @NotNull
    private static String getQualifiedClassNameFromPath(Path buildDirectory) {
        return String.format("%s", buildDirectory)
                .replace("build/classes/java/main/", "")
                .replace(".class", "")
                .replace("/", ".");
    }

    private static boolean doCompilation(List<File> found) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        List<String> optionList = new ArrayList<String>();
        optionList.add("-classpath");
        optionList.add(System.getProperty("java.class.path"));
        return doCompile(fileManager, found, compiler, diagnostics, optionList);
    }

    private static boolean doCompile(StandardJavaFileManager fileManager, List<File> found, JavaCompiler compiler,
                                     DiagnosticCollector<JavaFileObject> diagnostics, List<String> optionList) {
        var compilationUnit = fileManager.getJavaFileObjectsFromFiles(found);
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                optionList,
                null,
                compilationUnit);
        Boolean called = task.call();
        return called;
    }


}
