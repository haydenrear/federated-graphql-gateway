package com.hayden.gateway.fetcher.support;

import com.hayden.gateway.discovery.GraphQlVisitorCommunication;
import com.hayden.gateway.discovery.ServiceVisitorDelegate;
import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.gateway.federated.WebGraphQlFederatedExecutionService;
import com.hayden.graphql.federated.client.FederatedGraphQlClientBuilder;
import com.hayden.graphql.federated.execution.DataServiceRequestExecutor;
import com.hayden.graphql.models.dataservice.BeanContext;
import com.hayden.graphql.models.dataservice.RemoteDataFetcher;
import com.hayden.graphql.models.federated.request.ClientFederatedRequestItem;
import com.hayden.graphql.models.federated.request.FederatedRequestData;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceItemId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.TypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class RemoteDataFetcherImpl<T> implements RemoteDataFetcher<T>, ApplicationEventPublisher {

    private ApplicationContext applicationContext;
    private WebGraphQlFederatedExecutionService executionService;

    /**
     * Ability to retrieve any bean from the context.
     * @param env
     * @return
     */
    @Override
    public BeanContext ctx(DataFetchingEnvironment env) {
        return new BeanContext() {
            @Override
            public <U> U get(Class<U> clzz, Object... args) {
                return applicationContext.getBean(clzz, args);
            }
        };
    }

    @Override
    public T execute(DataFetchingEnvironment env) {
        return this.toResultData(
                Flux.using(
                                () -> executionService.federatedClient(),
                                federatedClient -> federatedClient.request(toRequestData(env)),
                                FederatedGraphQlClientBuilder.FederatedGraphQlClient::close
                        )
                        .log()
                        .collectList()
        );
    }

    @NotNull
    private T toResultData(Mono<List<DataServiceRequestExecutor.FederatedGraphQlResponse>> t) {
        try {
            return t.map(this::convert)
                    .log()
                    .toFuture()
                    .get();
        } catch (InterruptedException |
                 ExecutionException e) {
            log.error("Error when attempting to get response: {}.", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private T convert(List<DataServiceRequestExecutor.FederatedGraphQlResponse> l) {
        List<?> value = l.stream().flatMap(result -> {
            try {
                return Stream.of(result.toResult().getData());
            } catch (
                    ClassCastException c) {
                log.error("Error when converting {} with error {}.", result.toResult(), c.getMessage());
                return Stream.empty();
            }
        }).collect(Collectors.toList());
        return this.from(value);
    }
}
