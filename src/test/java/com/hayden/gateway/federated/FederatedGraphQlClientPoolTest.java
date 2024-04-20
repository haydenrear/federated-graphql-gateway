package com.hayden.gateway.federated;

import com.hayden.graphql.federated.client.IFederatedGraphQlClientBuilder;
import com.hayden.graphql.federated.transport.*;
import com.hayden.utilitymodule.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


class FederatedGraphQlClientPoolTest {

    @Test
    void client() throws Exception {
        FederatedGraphQlClientPool federatedGraphQlClient = new FederatedGraphQlClientPool();
        federatedGraphQlClient.poolSize = 5;
        federatedGraphQlClient.connectTimeout = 1;

        federatedGraphQlClient.createClients();
        assertThat(federatedGraphQlClient.builders.size()).isEqualTo(5);

        Result<IFederatedGraphQlClientBuilder, Result.Error> builderCreated = federatedGraphQlClient.client();
        assertThat(federatedGraphQlClient.builders.size()).isEqualTo(4);

        assertThat(builderCreated.isPresent()).isTrue();

        try(var b = builderCreated.get().buildFederatedClient(new FederatedGraphQlTransportResult(false, null))) {
            System.out.println(b);
        }

        assertThat(federatedGraphQlClient.builders.size()).isEqualTo(5);

    }
}