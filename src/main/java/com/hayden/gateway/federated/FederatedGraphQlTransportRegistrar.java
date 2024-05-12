package com.hayden.gateway.federated;

import com.hayden.graphql.federated.transport.federated_transport.FederatedGraphQlTransport;
import com.hayden.graphql.federated.transport.federated_transport.FederatedGraphQlTransportResult;
import com.hayden.graphql.federated.transport.register.GraphQlRegistration;
import com.hayden.graphql.federated.wiring.ReloadIndicator;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class FederatedGraphQlTransportRegistrar {

    private final ReloadIndicator reloadIndicator;
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
    public BiConsumer<String, FederatedGraphQlServiceFetcherItemId> visit(GraphQlRegistration graphQlTransport) {
        var unregister = federatedGraphQlTransport.register(graphQlTransport);
        reloadIndicator.setReload();
        return unregister;
    }


}
