package com.hayden.gateway.federated;

import com.hayden.graphql.models.client.ClientRequest;
import com.hayden.graphql.models.federated.request.FederatedRequestData;
import com.hayden.graphql.models.federated.request.FederatedRequestDataItem;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestDataParser {

    public FederatedRequestData buildRequestData(ExecutionGraphQlRequest request) {
        return new FederatedRequestData(
                FederatedRequestDataItem.builder()
                        .federatedService(new FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceFetcherId(null, null, null))
                        .requestBody(request.getDocument())
                        .operationName(request.getOperationName())
                        .request(request)
                        .variables(request.getVariables())
                        .extensions(request.getExtensions())
                        .build()
        );
    }

    public FederatedRequestData buildRequestData(ClientRequest request) {
        throw new RuntimeException("Client request not implemented!");
    }

}
