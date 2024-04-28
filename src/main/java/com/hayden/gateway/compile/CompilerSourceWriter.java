package com.hayden.gateway.compile;

import com.hayden.gateway.compile.compile_in.CompileFileIn;
import com.hayden.utilitymodule.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class CompilerSourceWriter {

    public record ToCompileFile(File file, String packageName) {}


    public <T> Result<Collection<ToCompileFile>, Result.Error> writeFiles(T generate,
                                                                          Function<T, Stream<CompileFileIn>> fileFn,
                                                                          JavaCompile.CompileArgs writeToFile) {
        return Result.ok(
                fileFn.apply(generate)
                        .flatMap(j -> {
                            try {
                                File nameValue = new File(writeToFile.compilerIn());
                                j.writeTo(nameValue);
                                return Stream.of(new ToCompileFile(new File(nameValue.getPath(), j.name()), j.packageName()));
                            } catch (IOException e) {
                                log.error("Error when writing DGS to {}: {}.", j.name(), e.getMessage());
                            }
                            return Stream.empty();
                        }).collect(Collectors.toCollection(ArrayList::new))
                );
    }
}
