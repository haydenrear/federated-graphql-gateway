package com.hayden.gateway.federated;

import com.hayden.graphql.models.client.ClientRequest;
import com.hayden.graphql.models.federated.request.FederatedRequestData;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestDataParser {

    public FederatedRequestData buildRequestData(ExecutionGraphQlRequest request) {
        return null;
    }

    public FederatedRequestData buildRequestData(ClientRequest request) {
        return null;
    }

}
