package com.hayden.gateway.compile;

import lombok.Builder;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.util.List;

@Builder
public record JavaCCompileArgs(
        CompilerSourceWriter.CompileSourceWriterResult compileSourceWriterResult,
        StandardJavaFileManager fileManager,
        JavaCompiler compiler,
        DiagnosticCollector<JavaFileObject> diagnostics,
        List<String> optionList) {
}