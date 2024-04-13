package com.hayden.gateway.graphql;

import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceItemId;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The API actions are pull, as the gateway only registers schema from servers it already knows from service
 * discovery, and so there are different types of actions for the Type and Fetcher.
 */
public interface GraphQlServiceApiVisitor {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class ContextCallback {
        BiConsumer<String, FederatedGraphQlServiceItemId> callback;
    }

    void remove();

    String id();

    default void visit(RegistriesComposite registries, Context.RegistriesContext context) {
        Optional.ofNullable(registries.typeDefinitionRegistry()).ifPresent(t -> this.visit(t, context));

        Optional.ofNullable(registries.codeRegistry())
                .flatMap(b -> Optional.ofNullable(registries.typeDefinitionRegistry())
                        .map(t -> Pair.of(b, t))
                )
                .ifPresent(t -> this.visit(registries.codeRegistry(), registries.typeDefinitionRegistry(), context));

        Optional.ofNullable(registries.mimeTypeRegistry()).ifPresent(t -> this.visit(t, context));

        Optional.ofNullable(registries.mimeTypeRegistry()).ifPresent(t -> this.visit(t, context));
    }

    default void visit(TypeDefinitionRegistry typeDefinitionRegistry, Context.RegistriesContext ctx) {}

    default void visit(GraphQLCodeRegistry.Builder codeRegistryBuilder, TypeDefinitionRegistry registry, Context.RegistriesContext ctx) {}

    default void visit(MimeTypeRegistry mimeTypeRegistry, Context.RegistriesContext ctx) {}

    default void visit(FederatedGraphQlTransportRegistrar federatedGraphQlTransportRegistrar,
                       Context.RegistriesContext registriesContext) {}

}
