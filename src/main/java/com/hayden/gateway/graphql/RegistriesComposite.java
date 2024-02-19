package com.hayden.gateway.graphql;

import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;

public record RegistriesComposite(
        TypeDefinitionRegistry typeDefinitionRegistry,
        GraphQLCodeRegistry.Builder codeRegistry,
        MimeTypeRegistry mimeTypeRegistry,
        FederatedGraphQlTransportRegistrar federatedGraphQlTransportRegistrar) {
    public RegistriesComposite(TypeDefinitionRegistry typeDefinitionRegistry, MimeTypeRegistry mimeTypeRegistry,
                               FederatedGraphQlTransportRegistrar federatedGraphQlTransportRegistrar) {
        this(typeDefinitionRegistry, null, mimeTypeRegistry, federatedGraphQlTransportRegistrar);
    }
}
