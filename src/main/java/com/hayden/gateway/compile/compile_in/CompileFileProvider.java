package com.hayden.gateway.compile.compile_in;

import com.hayden.gateway.compile.CompilerSourceWriter;
import com.hayden.gateway.compile.JavaCompile;

import java.util.Collection;

public interface CompileFileProvider {

    Collection<CompilerSourceWriter.ToCompileFile> toCompileFiles(JavaCompile.CompileArgs sourceIn);

}
