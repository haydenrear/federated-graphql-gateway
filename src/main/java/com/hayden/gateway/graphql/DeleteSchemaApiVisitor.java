package com.hayden.gateway.graphql;

import com.hayden.graphql.models.visitor.datafetcher.DeleteSchemaApiVisitorModel;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record DeleteSchemaApiVisitor(@Delegate DeleteSchemaApiVisitorModel deleteModel) implements GraphQlServiceApiVisitor {
    @Override
    public void visit(TypeDefinitionRegistry typeDefinitionRegistry,
                      Context.RegistriesContext ctx) {
        typeDefinitionRegistry.getType(deleteModel.toDelete())
                .map(t -> {
                    typeDefinitionRegistry.remove(deleteModel.toDelete(), t);
                    return typeDefinitionRegistry;
                })
                .orElseGet(() -> {
                    log.error("Received request to delete {} but did not exist.", deleteModel);
                    return typeDefinitionRegistry;
                });
    }

    @Override
    public void visit(GraphQLCodeRegistry.Builder codeRegistryBuilder,
                      TypeDefinitionRegistry registry, Context.RegistriesContext ctx) {
        // no op
    }
}
