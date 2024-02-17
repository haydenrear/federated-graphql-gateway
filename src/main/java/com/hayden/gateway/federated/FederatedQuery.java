package com.hayden.gateway.federated;

import com.hayden.gateway.client.ClientRequest;
import com.hayden.gateway.client.ClientResponse;
import com.hayden.utilitymodule.result.Result;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FederatedQuery {


    private final DgsQueryExecutor dgsQueryExecutor;
    private final FederatedStitcher federatedStitcher;

    public ClientResponse execute(ClientRequest clientRequest) {
        return new ClientResponse(
                federatedStitcher.toQueries(clientRequest).stream()
                        .map(dgsQueryExecutor::execute)
                        .map(e -> new ClientResponse.ClientResponseItem(Result.from(e.getData(), e.getErrors().stream().map(GraphQLError::getMessage).collect(Collectors.joining(",")))))
                        .toArray(ClientResponse.ClientResponseItem[]::new)
        );
    }

    @Cdc
    public ExecutionResult call(@Language("GraphQL") String toCall) {
        return dgsQueryExecutor.execute(toCall);
    }


}
