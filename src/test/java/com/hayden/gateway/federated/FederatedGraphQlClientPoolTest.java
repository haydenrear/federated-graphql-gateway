package com.hayden.gateway.federated;

import com.hayden.graphql.federated.client.IFederatedGraphQlClientBuilder;
import com.hayden.graphql.federated.transport.federated_transport.FederatedGraphQlTransportResult;
import com.hayden.utilitymodule.result.error.SingleError;
import com.hayden.utilitymodule.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


class FederatedGraphQlClientPoolTest {

    @Test
    void client() {
        FederatedGraphQlClientPool federatedGraphQlClient = new FederatedGraphQlClientPool();
        federatedGraphQlClient.poolSize = 5;
        federatedGraphQlClient.connectTimeout = 1;

        federatedGraphQlClient.createClients();
        assertThat(federatedGraphQlClient.builders.size()).isEqualTo(5);

        Result<IFederatedGraphQlClientBuilder, SingleError> builderCreated = federatedGraphQlClient.client();
        assertThat(federatedGraphQlClient.builders.size()).isEqualTo(4);

        assertThat(builderCreated.r().isPresent()).isTrue();

        try(var b = builderCreated.r().get().buildFederatedClient(new FederatedGraphQlTransportResult(false, null))) {
            System.out.println(b);
        }

        assertThat(federatedGraphQlClient.builders.size()).isEqualTo(5);

    }
}