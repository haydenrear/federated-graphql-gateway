package com.hayden.gateway.graphql;

import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;

@Slf4j
public record DeleteSchemaApiVisitor(String toDelete) implements GraphQlServiceApiVisitor {
    @Override
    public void visit(TypeDefinitionRegistry typeDefinitionRegistry,
                      RegistriesContext ctx) {
        typeDefinitionRegistry.getType(toDelete)
                .map(t -> {
                    typeDefinitionRegistry.remove(toDelete, t);
                    return typeDefinitionRegistry;
                })
                .orElseGet(() -> {
                    log.error("Received request to delete {} but did not exist.", toDelete);
                    return typeDefinitionRegistry;
                });
    }

    @Override
    public void visit(GraphQLCodeRegistry.Builder codeRegistryBuilder,
                      TypeDefinitionRegistry registry, RegistriesContext ctx) {
        // no op
    }
}
