package com.hayden.gateway.federated;

import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.graphql.federated.transport.FederatedGraphQlTransport;
import com.hayden.graphql.federated.transport.FederatedGraphQlTransportResult;
import com.hayden.graphql.federated.transport.FederatedItemGraphQlTransport;
import com.hayden.graphql.federated.wiring.ReloadIndicator;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceItemId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.client.GraphQlTransport;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FederatedGraphQlTransportRegistrar {

    @Autowired(required = false)
    private ReloadIndicator reloadIndicator;

    private final FederatedGraphQlTransport federatedGraphQlTransport;

    public FederatedGraphQlTransportResult transport() {
        if (reloadIndicator.doReload()) {
            // do reload logic
            return new FederatedGraphQlTransportResult(true, federatedGraphQlTransport);
        }
        return new FederatedGraphQlTransportResult(false, federatedGraphQlTransport);
    }


    public void visit(FederatedGraphQlTransport.GraphQlRegistration graphQlTransport) {
        Optional.ofNullable(reloadIndicator).ifPresent(ReloadIndicator::setReload);
        federatedGraphQlTransport.register(graphQlTransport);
    }


}
