package com.hayden.gateway.federated;

import com.hayden.graphql.models.client.ClientRequest;
import com.hayden.graphql.models.client.ClientResponse;
import com.hayden.tracing_apt.Cdc;
import com.hayden.utilitymodule.result.Result;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.ExecutionStrategy;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The ClientRequest are **not** graphql. So then you go to your registry that the services
 * have registered, find the appropriate protocol mimetypes for those, and generate the graphql
 * and stitch it together. User can also send graphql directly... but it's not very safe. Trust
 * the data services not the users.
 */
@Component
@RequiredArgsConstructor
public class FederatedQuery {


    private final DgsQueryExecutor dgsQueryExecutor;
    private final FederatedStitcher federatedStitcher;

    public ClientResponse execute(ClientRequest clientRequest) {
        return new ClientResponse(
                federatedStitcher.toQueries(clientRequest).values().stream()
                        .map(this::call)
                        .flatMap(Optional::stream)
                        .map(e -> new ClientResponse.ClientResponseItem(
                                Result.from(
                                        e.getData(),
                                        e.getErrors().stream().map(GraphQLError::getMessage)
                                                .collect(Collectors.joining(","))
                                )
                        ))
                        .toArray(ClientResponse.ClientResponseItem[]::new)
        );
    }

//    @Cdc
    public Optional<ExecutionResult> call(List<FederatedStitcher.FederatedGraphQlRequest> toCall) {
        AtomicReference<ExecutionResult> last = new AtomicReference<>();
        return toCall.stream()
                .map(f -> dgsQueryExecutor.execute(f.query(), f.variables()))
                // could remove if failed, but will automatically do this in Discovery
                .peek(last::set)
                .filter(ExecutionResult::isDataPresent)
                .findFirst()
                .or(() -> Optional.ofNullable(last.get()));
    }
}
