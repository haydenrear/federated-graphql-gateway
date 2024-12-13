package com.hayden.gateway.federated;

import com.hayden.gateway.federated.pool.ConnectionTimeoutException;
import com.hayden.graphql.federated.client.FederatedGraphQlClientBuilderHolder;
import com.hayden.graphql.federated.client.IFederatedGraphQlClientBuilder;
import com.hayden.utilitymodule.result.error.Err;
import com.hayden.utilitymodule.result.error.ErrorCollect;
import com.hayden.utilitymodule.result.Result;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@Slf4j
@Service
public class FederatedGraphQlClientPool {


    public static final String FEDERATED_CLIENT_STR = "FederatedGraphQlClientBuilderAutoClosableProxy";
    public static final String TO_STRING = "toString";

    @Value("${federated-graphql.pool.size:5}")
    int poolSize;
    @Value("${federated-graphql.pool.timeout:30}")
    int connectTimeout;

    BlockingQueue<IFederatedGraphQlClientBuilder> builders;

    @PostConstruct
    public void createClients() {
        this.builders = new ArrayBlockingQueue<>(this.poolSize);
        IntStream.range(0, poolSize)
                .forEach(i -> {
                    FederatedGraphQlClientBuilderHolder e = new FederatedGraphQlClientBuilderHolder();
                    IFederatedGraphQlClientBuilder iFederatedGraphQlClientBuilder = federatedProxyRef(e);
                    e.setSelfRef(iFederatedGraphQlClientBuilder);
                    this.builders.add(iFederatedGraphQlClientBuilder);
                });
    }

    public Result<IFederatedGraphQlClientBuilder, ErrorCollect> client() {
        try {
            return Result.ok(this.builders.poll(connectTimeout, TimeUnit.SECONDS))
                    .orError(() -> Err.err(ErrorCollect.fromE(new ConnectionTimeoutException())))
                    .castError();
        } catch (InterruptedException e) {
            return Result.err(ErrorCollect.fromMessage("Could not wait for builder in builders queue with message %s.".formatted(e.getMessage())));
        }
    }

    private @NotNull IFederatedGraphQlClientBuilder federatedProxyRef(IFederatedGraphQlClientBuilder removed) {
        return (IFederatedGraphQlClientBuilder) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{AutoCloseable.class, IFederatedGraphQlClientBuilder.class},
                (proxied, method, args) -> {
                    log.debug("Called {} on {} with args {}.", method.getName(), proxied.getClass().getSimpleName(), Arrays.toString(args));
                    if (method.getName().equals("close")) {
                        log.debug("Adding graphQL client back to the pool.");
                        Assert.isTrue(builders.offer(removed), "Failed to add graphQl client back to connection pool.");
                        return null;
                    } else if (method.getName().equals(TO_STRING)) {
                        return FEDERATED_CLIENT_STR;
                    } else {
                        return method.invoke(removed, args);
                    }
                });
    }

}
