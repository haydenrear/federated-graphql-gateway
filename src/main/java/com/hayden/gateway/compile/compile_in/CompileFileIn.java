package com.hayden.gateway.compile.compile_in;

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
    
    

    
}
