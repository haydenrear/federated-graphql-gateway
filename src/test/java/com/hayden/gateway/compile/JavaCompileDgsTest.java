package com.hayden.gateway.compile;

import com.hayden.gateway.compile.compile_in.DgsCompileFileProvider;
import com.hayden.gateway.config.CompilerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {FlyJavaCompile.class, CompilerSourceWriter.class, CompilerConfig.class, DgsCompileFileProvider.class})
@TestPropertySource(properties = {"spring.docker.compose.enabled=false"})
@ExtendWith(SpringExtension.class)
class JavaCompileDgsTest {

    @Autowired
    private FlyJavaCompile dgsFlyCompileJava;
    @MockBean(name = "clientFlyClientCompileJava")
    private FlyJavaCompile clientCodeCompileProvider;


    @Test
    void compileAndLoad() {
        var loaded = dgsFlyCompileJava.compileAndLoad(new FlyJavaCompile.PathCompileArgs("src/test/resources/test_schemas", "dgs_in"));
        assertThat(loaded.get().classesCreated().size()).isEqualTo(49);
    }
}