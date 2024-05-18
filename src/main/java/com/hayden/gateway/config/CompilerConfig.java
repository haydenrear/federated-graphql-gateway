package com.hayden.gateway.config;

import com.hayden.gateway.compile.CompilerSourceWriter;
import com.hayden.gateway.compile.DgsCompiler;
import com.hayden.gateway.compile.JavaCompile;
import com.hayden.gateway.compile.compile_in.ClientCodeCompileFileProvider;
import com.hayden.gateway.compile.compile_in.CompileFileProvider;
import com.hayden.gateway.compile.compile_in.DgsCompileFileProvider;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompilerConfig {

    @Bean
    public JavaCompile dgsJavaCompile(DgsCompileFileProvider fileProvider) {
        return new JavaCompile(fileProvider);
    }

    @Bean
    public JavaCompile clientCodeJavaCompile(ClientCodeCompileFileProvider fileProvider) {
        return new JavaCompile(fileProvider);
    }

}
