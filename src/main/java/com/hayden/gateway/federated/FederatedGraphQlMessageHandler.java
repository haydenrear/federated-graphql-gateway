package com.hayden.gateway.federated;

import com.hayden.graphql.models.client.ClientRequest;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.integration.graphql.outbound.GraphQlMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class FederatedGraphQlMessageHandler extends GraphQlMessageHandler {
    private final FederatedExecutionGraphQlService graphQlService;

    public FederatedGraphQlMessageHandler(FederatedExecutionGraphQlService graphQlService) {
        super(graphQlService);
        this.graphQlService = graphQlService;
    }

    @Override
    protected Object handleRequestMessage(Message<?> requestMessage) {
        if (requestMessage.getPayload() instanceof ClientRequest clientRequest) {
            return this.graphQlService.execute(clientRequest);
        }
        else if (requestMessage.getPayload() instanceof ExecutionGraphQlRequest request){
            return this.graphQlService.execute(request);
        } else {
            return super.handleRequestMessage(requestMessage);
        }
    }
}
