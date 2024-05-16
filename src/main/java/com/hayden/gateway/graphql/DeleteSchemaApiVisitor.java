package com.hayden.gateway.graphql;

import com.hayden.graphql.models.visitor.schema.DeleteSchemaApiVisitorModel;
import com.hayden.utilitymodule.result.Result;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record DeleteSchemaApiVisitor(@Delegate DeleteSchemaApiVisitorModel deleteModel) implements GraphQlServiceApiVisitor {
    @Override
    public void remove() {

    }

    @Override
    public String id() {
        return deleteModel.id().host();
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(TypeDefinitionRegistry typeDefinitionRegistry,
                                                            Context.RegistriesContext ctx) {
        return typeDefinitionRegistry.getType(deleteModel.toDelete())
                .map(t -> {
                    typeDefinitionRegistry.remove(deleteModel.toDelete(), t);
                    return Result.<TypeDefinitionRegistry, GraphQlServiceVisitorError>ok(typeDefinitionRegistry);
                })
                .orElseGet(() -> {
                    log.error("Received request to delete {} but did not exist.", deleteModel);
                    return Result.err(new GraphQlServiceVisitorError("Did not exist."));
                })
                .flatMapResult(t -> Result.ok(new GraphQlServiceVisitorResponse("Successfully removed.")));
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(GraphQLCodeRegistry.Builder codeRegistryBuilder,
                      TypeDefinitionRegistry registry, Context.RegistriesContext ctx) {
        return Result.ok(new GraphQlServiceVisitorResponse("Nothing to remove."));
    }
}
