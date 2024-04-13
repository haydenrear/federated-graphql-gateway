package com.hayden.gateway.federated;

import com.hayden.graphql.federated.client.FederatedGraphQlClientBuilder;
import com.hayden.graphql.federated.response.ResponseExecutionAdapter;
import com.hayden.graphql.federated.transport.FederatedDynamicGraphQlSource;
import com.hayden.graphql.models.client.ClientRequest;
import com.hayden.graphql.models.federated.request.FederatedRequestData;
import graphql.ExecutionInput;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.ExecutionGraphQlRequest;
import org.springframework.graphql.ExecutionGraphQlResponse;
import org.springframework.graphql.execution.DefaultExecutionGraphQlService;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class WebGraphQlFederatedExecutionService implements FederatedExecutionGraphQlService {


    private final RequestDataParser requestDataParser;
    private final PooledFederatedGraphQlClient clientPool;
    private final FederatedGraphQlTransportRegistrar registrar;
    private final FederatedDynamicGraphQlSource federatedDynamicGraphQlSource;

    private DefaultExecutionGraphQlService defaultExecutionService;


    public FederatedGraphQlClientBuilder.FederatedGraphQlClient federatedClient() {
        return clientPool.builder()
                .map(f -> {
                    var transport = registrar.transport();
                    return f.buildFederatedClient(transport);
                })
                .orElse(null);
    }

    @PostConstruct
    public void createExecutionGraphQlService() {
        this.defaultExecutionService = new DefaultExecutionGraphQlService(federatedDynamicGraphQlSource);
    }

    @Override
    public Mono<ExecutionGraphQlResponse> execute(ClientRequest clientRequest) {
        var requestData = this.requestDataParser.buildRequestData(clientRequest);
        return doFederatedGraphQl(requestData, new ExecutionInput.Builder().build());
    }

    @Override
    public @NotNull Mono<ExecutionGraphQlResponse> execute(@NotNull ExecutionGraphQlRequest request) {
        return doFederatedGraphQl(request);
    }

    @Override
    public @NotNull Mono<ExecutionGraphQlResponse> execute(@NotNull ExecutionGraphQlRequest request, boolean parsed) {
        if (parsed) {
            var requestData = this.requestDataParser.buildRequestData(request);
            return doFederatedGraphQl(requestData, new ExecutionInput.Builder().build());
        } else {
            return this.execute(request);
        }
    }

    @NotNull
    private Mono<ExecutionGraphQlResponse> doFederatedGraphQl(ExecutionGraphQlRequest request) {
        return this.defaultExecutionService.execute(request);
    }

    @NotNull
    private Mono<ExecutionGraphQlResponse> doFederatedGraphQl(FederatedRequestData requestData,
                                                              ExecutionInput request) {
        return Mono.using(
                this::federatedClient,
                federatedClient -> Mono.from(federatedClient.request(requestData))
                        .map(g -> new DefaultExecutionGraphQlResponse(
                                request,
                                ResponseExecutionAdapter.executionResult(g)
                        )),
                FederatedGraphQlClientBuilder.FederatedGraphQlClient::close
        );
    }

}
