package com.hayden.gateway.discovery.comm;

import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public record DelayedService(
        FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId host,
        int discoveryPingSeconds,
        AtomicBoolean initialized,
        int initialDelay) implements Delayed {

    public DelayedService(FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId host, int discoveryPingSeconds) {
        this(host, discoveryPingSeconds, new AtomicBoolean(false), 15);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.SECONDS) - o.getDelay(TimeUnit.SECONDS));
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        if (!this.initialized.getAndSet(true))
            return unit.convert(Duration.ofSeconds(initialDelay));

        return unit.convert(Duration.ofSeconds(discoveryPingSeconds));
    }

}
