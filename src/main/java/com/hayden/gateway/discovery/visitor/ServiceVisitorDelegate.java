package com.hayden.gateway.discovery.visitor;

import com.hayden.gateway.discovery.comm.DelayedService;
import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.graphql.federated.visitor_model.ChangeVisitorModel;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import com.hayden.graphql.models.visitor.model.Digest;
import com.hayden.graphql.models.visitor.model.Reloadable;
import lombok.experimental.Delegate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public record ServiceVisitorDelegate(
        FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId host,
        Map<Class<? extends GraphQlServiceApiVisitor>, ServiceVisitor> visitors,
        DelayedService delayedService,
        MessageDigestBytes digest
) implements Digest {

    public record ServiceVisitorDelegateContext(List<ServiceVisitorDelegate> delegates)
            implements ChangeVisitorModel.VisitorDelegatesContext {}

    public record ServiceVisitor(@Delegate GraphQlServiceApiVisitor visitor, AtomicBoolean registered, MessageDigestBytes digest)
            implements Digest, Reloadable {

        public ServiceVisitor(GraphQlServiceApiVisitor visitor) {
            this(visitor, new AtomicBoolean(false), visitor.digest());
        }

        public MessageDigestBytes digest() {
            return digest;
        }
    }

    public void onRemoved() {
        this.visitors.values().forEach(ServiceVisitor::onRemoved);
    }

}
