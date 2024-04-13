package com.hayden.gateway.compile;

import com.hayden.gateway.compile.compile_in.DgsCompileFileProvider;
import com.hayden.gateway.config.CompilerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {JavaCompile.class, CompilerSourceWriter.class, CompilerConfig.class, DgsCompileFileProvider.class})
@ExtendWith(SpringExtension.class)
class JavaCompileDgsTest {

    @Autowired
    private JavaCompile javaCompile;


    @Test
    void compileAndLoad() {
        var loaded = javaCompile.compileAndLoad(new JavaCompile.CompileArgs("src/test/resources/graphql", "dgs_in"));
        assertThat(loaded.size()).isEqualTo(51);
    }
}