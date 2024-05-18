package com.hayden.gateway.compile.compile_in;

import com.hayden.gateway.compile.CompileArgs;
import com.hayden.gateway.compile.CompilerSourceWriter;
import com.hayden.utilitymodule.result.Error;
import com.hayden.utilitymodule.result.Result;

public interface CompileFileProvider<T extends CompilerSourceWriter.CompileSourceWriterResult> {

    Result<T, Error.AggregateError> toCompileFiles(CompileArgs sourceIn);

}
