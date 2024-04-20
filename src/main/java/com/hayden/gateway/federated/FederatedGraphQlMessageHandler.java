package com.hayden.gateway.federated;

import com.hayden.graphql.federated.FederatedExecutionGraphQlService;
import com.hayden.graphql.models.client.ClientRequest;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.integration.graphql.outbound.GraphQlMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class FederatedGraphQlMessageHandler extends GraphQlMessageHandler {
    private final FederatedExecutionGraphQlService graphQlService;

    public FederatedGraphQlMessageHandler(FederatedExecutionGraphQlService graphQlService) {
        super(graphQlService);
        this.graphQlService = graphQlService;
    }

    @Override
    protected Publisher<?> handleRequestMessage(Message<?> requestMessage) {
        return switch (requestMessage.getPayload()) {
            case ClientRequest c -> this.graphQlService.execute(c).log();
            case ExecutionGraphQlRequest c -> this.graphQlService.execute(c).log();
            default -> super.handleRequestMessage(requestMessage) instanceof Publisher<?> requestPublisher
                    ? requestPublisher
                    : Mono.empty();
        };
    }
}
