package com.hayden.gateway.compile;

import com.hayden.utilitymodule.io.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public interface CompileArgs {

    String compilerIn();

    String compilerOut();


    default Path compilerOutPath(String packageName) {
        return Optional.ofNullable(compilerOut())
                .map(File::new)
                .orElse(new File("build/classes/java/main/%s".formatted(packageName)))
                .toPath();
    }

    default boolean cleanPrevious() {
        return FileUtils.deleteFilesRecursive(Paths.get(compilerIn(), "com"))
                && FileUtils.deleteFilesRecursive(compilerOutPath("com/netflix"));
    }
}
