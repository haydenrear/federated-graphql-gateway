package com.hayden.gateway.compile.compile_in;

import com.hayden.graphql.models.visitor.DataSource;
import com.hayden.utilitymodule.io.FileUtils;
import com.squareup.javapoet.JavaFile;
import lombok.experimental.Delegate;

import java.io.File;
import java.io.IOException;

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
        public void writeTo(File file) throws IOException {
            FileUtils.writeToFile(o.sourceMetadata().target(), file.toPath());
        }

        @Override
        public String name() {
            return o.id();
        }

        @Override
        public String packageName() {
            return o.sourceMetadata().packageName();
        }
    }
    
    

    
}
