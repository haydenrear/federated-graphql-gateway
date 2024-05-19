package com.hayden.gateway.config;

import com.hayden.gateway.compile.FlyJavaCompile;
import com.hayden.gateway.compile.compile_in.ClientCodeCompileFileProvider;
import com.hayden.gateway.compile.compile_in.DgsCompileFileProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompilerConfig {

    @Bean
    public FlyJavaCompile dgsFlyCompileJava(DgsCompileFileProvider dgsCompileFileProvider) {
        return new FlyJavaCompile(dgsCompileFileProvider);
    }

    @Bean
    public FlyJavaCompile clientFlyClientCompileJava(ClientCodeCompileFileProvider fileProvider) {
        return new FlyJavaCompile(fileProvider);
    }

}
