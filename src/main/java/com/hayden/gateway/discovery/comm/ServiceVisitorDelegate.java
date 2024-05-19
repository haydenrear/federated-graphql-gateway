package com.hayden.gateway.discovery.comm;

import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import com.hayden.graphql.models.visitor.model.Digest;
import lombok.experimental.Delegate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public record ServiceVisitorDelegate(
        FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId host,
        Map<Class<? extends GraphQlServiceApiVisitor>, ServiceVisitor> visitors,
        DelayedService delayedService,
        MessageDigestBytes digest
) implements Digest{

    public record ServiceVisitor(@Delegate GraphQlServiceApiVisitor visitor, AtomicBoolean registered, MessageDigestBytes digest) implements Digest {
        public ServiceVisitor(GraphQlServiceApiVisitor visitor) {
            this(visitor, new AtomicBoolean(false), visitor.digest());
        }
    }

    public void remove() {
        this.visitors.values().forEach(ServiceVisitor::remove);
    }

}
