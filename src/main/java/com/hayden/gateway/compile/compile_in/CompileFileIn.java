package com.hayden.gateway.compile.compile_in;

import com.hayden.graphql.models.visitor.DataSource;
import com.hayden.utilitymodule.io.FileUtils;
import com.squareup.javapoet.JavaFile;
import lombok.experimental.Delegate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public interface CompileFileIn {
   
    void writeTo(File file) throws IOException;
    String name();
    String packageName();

    record JavaPoetCompileFileIn(@Delegate JavaFile codeGenResult) implements CompileFileIn {

        @Override
        public String name() {
            return codeGenResult.toJavaFileObject().getName();
        }

        @Override
        public String packageName() {
            return codeGenResult.packageName;
        }
    }

    record ClientFileCompileFileIn(DataSource o) implements CompileFileIn {

        @Override
        public void writeTo(File file) {
            FileUtils.writeToFile(o.sourceMetadata().target(), Paths.get(file.toPath().toString(), name()));
        }

        @Override
        public String name() {
            return "%s.java".formatted(o.id());
        }

        @Override
        public String packageName() {
            return o.sourceMetadata().packageName();
        }
    }
    
    

    
}
