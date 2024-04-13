package com.hayden.gateway.federated;

import com.hayden.graphql.models.client.ClientRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.graphql.ExecutionGraphQlResponse;
import org.springframework.graphql.ExecutionGraphQlService;
import reactor.core.publisher.Mono;

public interface FederatedExecutionGraphQlService extends ExecutionGraphQlService {

    Mono<ExecutionGraphQlResponse> execute(ClientRequest clientRequest);

    Mono<ExecutionGraphQlResponse> execute(@NotNull ExecutionGraphQlRequest request, boolean parsed);

}
