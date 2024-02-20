package com.hayden.gateway.graphql;

import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.graphql.federated.transport.FederatedGraphQlTransport;
import com.hayden.graphql.models.visitor.datafed.DataFederationSources;
import com.hayden.graphql.models.visitor.datafed.GraphQlDataFederationModel;

public record GraphQlDataFederation(GraphQlDataFederationModel model)
        implements GraphQlServiceApiVisitor{

    @Override
    public void visit(FederatedGraphQlTransportRegistrar federatedGraphQlTransportRegistrar, Context.RegistriesContext registriesContext) {
        this.model.federationSource().stream().map(this::toTransportRegistration)
                .forEach(federatedGraphQlTransportRegistrar::visit);
    }

    public FederatedGraphQlTransport.GraphQlRegistration toTransportRegistration(
            DataFederationSources graphQlFetcherSource
    ) {
        return null;
    }
}
