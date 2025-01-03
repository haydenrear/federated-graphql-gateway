package com.hayden.gateway.graphql;

import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.graphql.federated.transport.register.GraphQlRegistration;
import com.hayden.graphql.models.visitor.simpletransport.GraphQlTransportModel;
import com.hayden.utilitymodule.result.Result;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public record GraphQlTransports(@Delegate GraphQlTransportModel model, MessageDigestBytes digest, ContextCallback removeCallback)
        implements GraphQlServiceApiVisitor {

    public GraphQlTransports(GraphQlTransportModel model, MessageDigestBytes digest) {
        this(model, digest, new ContextCallback());
    }

    @Override
    public boolean onRemoved() {
        removeCallback.getCallback().accept(id().host().host(), model.serviceId());
        return true;
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(FederatedGraphQlTransportRegistrar federatedGraphQlTransportRegistrar, Context.RegistriesContext registriesContext) {
        Optional.of(this.toTransportRegistration(this.model))
                .ifPresent(m -> removeCallback.setCallback(federatedGraphQlTransportRegistrar.visit(m)));

        return Result.ok(new GraphQlServiceVisitorResponse("Unimplemented error scenario."));
    }

    public GraphQlRegistration toTransportRegistration(GraphQlTransportModel graphQlFetcherSource) {
        return new GraphQlRegistration.GraphQlTransportFederatedGraphQlRegistration(
           graphQlFetcherSource.transportBuilder().toTransport(),
           graphQlFetcherSource.serviceId());
    }

    @Override
    public boolean isReloadable() {
        return false;
    }
}
