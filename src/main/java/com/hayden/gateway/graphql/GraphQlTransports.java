package com.hayden.gateway.graphql;

import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.graphql.federated.transport.register.GraphQlRegistration;
import com.hayden.graphql.models.visitor.simpletransport.GraphQlTransportModel;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public record GraphQlTransports(@Delegate GraphQlTransportModel model, String id, ContextCallback removeCallback)
        implements GraphQlServiceApiVisitor {

    public GraphQlTransports(GraphQlTransportModel model) {
        this(model, model.serviceId().host(), new ContextCallback());
    }

    @Override
    public void remove() {
        removeCallback.callback.accept(id, model.serviceId());
    }

    @Override
    public void visit(FederatedGraphQlTransportRegistrar federatedGraphQlTransportRegistrar, Context.RegistriesContext registriesContext) {
        Optional.ofNullable(this.toTransportRegistration(this.model))
                .ifPresent(m -> removeCallback.callback = federatedGraphQlTransportRegistrar.visit(m));
    }

    public GraphQlRegistration toTransportRegistration(GraphQlTransportModel graphQlFetcherSource) {
        return null;
    }

}
