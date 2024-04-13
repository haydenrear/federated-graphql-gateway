package com.hayden.gateway.federated;

import com.hayden.graphql.federated.transport.FederatedGraphQlTransport;
import com.hayden.graphql.federated.transport.FederatedGraphQlTransportResult;
import com.hayden.graphql.federated.transport.GraphQlRegistration;
import com.hayden.graphql.federated.wiring.ReloadIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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


    /**
     *
     * @param graphQlTransport
     */
    public void visit(GraphQlRegistration graphQlTransport) {
        federatedGraphQlTransport.register(graphQlTransport);
        Optional.ofNullable(reloadIndicator).ifPresent(ReloadIndicator::setReload);
    }


}
