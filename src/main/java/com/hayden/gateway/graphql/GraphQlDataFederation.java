package com.hayden.gateway.graphql;

import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.graphql.federated.transport.register.GraphQlRegistration;
import com.hayden.graphql.models.visitor.datafed.DataFederationSources;
import com.hayden.graphql.models.visitor.datafed.GraphQlDataFederationModel;
import com.hayden.utilitymodule.result.Result;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Add the federation for accessibility from the FederatedGraphQlClient inside the DataFetcher.
 * @param model
 * @param id
 * @param removeCallback
 */
public record GraphQlDataFederation(GraphQlDataFederationModel model, String id, ContextCallback removeCallback)
        implements GraphQlServiceApiVisitor {

    public GraphQlDataFederation(GraphQlDataFederationModel model) {
        this(model, UUID.randomUUID().toString(), new ContextCallback());
    }

    @Override
    public void remove() {
        removeCallback.callback.accept(id, model.serviceId());
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(FederatedGraphQlTransportRegistrar federatedGraphQlTransportRegistrar,
                                                            Context.RegistriesContext registriesContext) {
        var unregisters = this.model.federationSource().stream().map(this::toTransportRegistration)
                .filter(Objects::nonNull)
                .map(federatedGraphQlTransportRegistrar::visit)
                .toList();

        removeCallback.callback = (id, serviceId) -> unregisters.forEach(c -> c.accept(id, serviceId));

        if (unregisters.isEmpty()) {
            return Result.fromError(new GraphQlServiceVisitorError("Could not register GraphQl data federation."));
        }

        return Result.fromResult(new GraphQlServiceVisitorResponse("Successfully registered graphQl data federation.."));

    }

    public GraphQlRegistration toTransportRegistration(
            DataFederationSources graphQlFetcherSource
    ) {
        return null;
    }
}
