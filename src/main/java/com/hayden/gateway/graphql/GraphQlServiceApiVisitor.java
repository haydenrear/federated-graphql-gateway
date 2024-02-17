package com.hayden.gateway.graphql;

import com.hayden.gateway.compile.DgsCompiler;
import com.hayden.gateway.compile.JavaCompile;
import com.hayden.gateway.discovery.MimeTypeRegistry;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * The API actions are pull, as the gateway only registers schema from servers it already knows from service
 * discovery, and so there are different types of actions for the Type and Fetcher.
 */
public interface GraphQlServiceApiVisitor {

    interface Context {}

    record CodegenContext(DgsCompiler javaCompiler) implements Context {}
    record TypeDefinitionContext() implements Context {}
    record MimeTypeDefinitionContext(List<MimeTypeRegistry.DataFetcherRegistration> dataFetchers) implements Context {}
    record RegistryContext(TypeDefinitionRegistry typeDefinitionRegistry, GraphQLCodeRegistry.Builder codeRegistry, MimeTypeRegistry mimeTypeRegistry) {
        public RegistryContext(TypeDefinitionRegistry typeDefinitionRegistry, MimeTypeRegistry mimeTypeRegistry) {
            this(typeDefinitionRegistry, null, mimeTypeRegistry);
        }
    }
    record RegistriesContext(TypeDefinitionContext typeDefinitionContext, CodegenContext codegenContext, MimeTypeDefinitionContext mimeTypeDefinitionContext) {
        public RegistriesContext(TypeDefinitionContext typeDefinitionContext, CodegenContext codegenContext) {
            this(typeDefinitionContext, codegenContext,
                    new GraphQlServiceApiVisitor.MimeTypeDefinitionContext(new ArrayList<>()));
        }
    }

    default void visit(RegistryContext registries, RegistriesContext context) {
        Optional.ofNullable(registries.typeDefinitionRegistry).ifPresent(t -> this.visit(t, context));
        Optional.ofNullable(registries.codeRegistry).flatMap(b -> Optional.ofNullable(registries.typeDefinitionRegistry).map(t -> Pair.of(b, t)))
                .ifPresent(t -> this.visit(registries.codeRegistry, registries.typeDefinitionRegistry, context));
        Optional.ofNullable(registries.mimeTypeRegistry).ifPresent(t -> this.visit(t, context));
    }

    default void visit(TypeDefinitionRegistry typeDefinitionRegistry, RegistriesContext ctx) {}

    default void visit(GraphQLCodeRegistry.Builder codeRegistryBuilder, TypeDefinitionRegistry registry, RegistriesContext ctx) {}

    default void visit(MimeTypeRegistry mimeTypeRegistry, RegistriesContext ctx) {}

}
