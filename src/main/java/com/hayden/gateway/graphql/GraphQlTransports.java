package com.hayden.gateway.graphql;

import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.graphql.federated.transport.FederatedGraphQlTransport;
import com.hayden.graphql.models.visitor.datafed.DataFederationSources;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherGraphQlSource;
import com.hayden.graphql.models.visitor.simpletransport.GraphQlTransportModel;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: create a request for delete of schema from TypeRegistry.
 * @param serviceId the serviceId of this graph-ql service. This will be the same for deploying multiple services of
 *                  the same type.
 * @param source the GraphQlSource schema provided.
 * @param fetcherSource the source code for the params fetchers so this service id can be federated.
 */
@Slf4j
public record GraphQlTransports(@Delegate GraphQlTransportModel model)
        implements GraphQlServiceApiVisitor {

    @Override
    public void visit(FederatedGraphQlTransportRegistrar federatedGraphQlTransportRegistrar, Context.RegistriesContext registriesContext) {
        GraphQlServiceApiVisitor.super.visit(federatedGraphQlTransportRegistrar, registriesContext);
    }

    public FederatedGraphQlTransport.GraphQlRegistration toTransportRegistration(
            DataFederationSources graphQlFetcherSource
    ) {
        return null;
    }

}
