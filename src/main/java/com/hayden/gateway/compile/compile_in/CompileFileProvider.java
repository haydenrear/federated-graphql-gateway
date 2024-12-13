package com.hayden.gateway.compile.compile_in;

import com.hayden.gateway.compile.CompileArgs;
import com.hayden.gateway.compile.CompilerSourceWriter;
import com.hayden.gateway.compile.FlyJavaCompile;
import com.hayden.utilitymodule.result.Result;

public interface CompileFileProvider<T extends CompilerSourceWriter.CompileSourceWriterResult> {

    Result<T, FlyJavaCompile.CompileAndLoadError> toCompileFiles(CompileArgs sourceIn);

}
