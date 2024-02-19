package com.hayden.gateway.federated;

import com.hayden.graphql.federated.client.FederatedGraphQlClientBuilder;
import com.hayden.graphql.federated.client.IFederatedGraphQlClientBuilder;
import com.hayden.graphql.federated.wiring.ReloadIndicator;
import com.hayden.utilitymodule.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
@Service
public class PooledFederatedGraphQlClient {

    private final SynchronousQueue<FederatedGraphQlClientBuilder> builders;

    @Value("${federated-graphql.pool.size:5}")
    int poolSize;

    @Autowired(required = false)
    private ReloadIndicator reloadIndicator;

    public PooledFederatedGraphQlClient() {
        this.builders = new SynchronousQueue<>();
        IntStream.range(0, poolSize)
                .forEach(i -> this.builders.offer(new FederatedGraphQlClientBuilder()));
    }


    public Result<IFederatedGraphQlClientBuilder, Result.Error> builder() {
        try {
            var removed = this.builders.poll(30, TimeUnit.SECONDS);
            return Result.fromResult((IFederatedGraphQlClientBuilder) Proxy.newProxyInstance(
                    this.getClass().getClassLoader(),
                    new Class[]{AutoCloseable.class, IFederatedGraphQlClientBuilder.class},
                    (proxied, method, args) -> {
                        log.debug("Called {} on {} with args {}.", method.getName(), proxied.getClass().getSimpleName(), Arrays.toString(args));
                        if (method.getName().equals("close")) {
                            log.debug("Calling close on {} with args {}.", proxied.getClass().getSigners(), Arrays.toString(args));
                            builders.offer(removed);
                        }
                        return method.invoke(args);
                    })
            );
        } catch (InterruptedException e) {
            return Result.fromError("Could not wait for builder in builders queue with message %s.".formatted(e.getMessage()));
        }
    }

}
