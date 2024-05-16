package com.hayden.gateway.compile;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "graphql-compiler")
@Component
@Data
public class GraphQlCompilerProperties {

    private Path schemaOutput;



}
