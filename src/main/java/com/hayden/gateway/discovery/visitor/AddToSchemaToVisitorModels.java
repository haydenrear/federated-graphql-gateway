package com.hayden.gateway.discovery.visitor;

import com.hayden.graphql.federated.visitor_model.ChangeVisitorModelService;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;

import java.util.concurrent.atomic.AtomicBoolean;

public record AddToSchemaToVisitorModels(GraphQlDataFetcherDiscoveryModel toAdd, AtomicBoolean doRemoveAtomic) implements GatewayChangeVisitor {
    public AddToSchemaToVisitorModels(GraphQlDataFetcherDiscoveryModel toAdd) {
        this(toAdd, new AtomicBoolean(false));
    }

    @Override
    public boolean doRemove() {
        return this.doRemoveAtomic.get();
    }

    @Override
    public ChangeVisitorModelService.VisitorModelsContext commandIf(ChangeVisitorModelService.VisitorModelsContext model, ServiceVisitorDelegate.ServiceVisitorDelegateContext visitor) {
        // implement if needed... currently not existing but could ostensibly exist as delete exists...
        return model;
    }

}
