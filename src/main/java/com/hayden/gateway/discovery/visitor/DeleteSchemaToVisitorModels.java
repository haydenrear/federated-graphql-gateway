package com.hayden.gateway.discovery.visitor;

import com.hayden.gateway.graphql.GraphQlDataFetcher;
import com.hayden.graphql.federated.visitor_model.ChangeVisitorModel;
import com.hayden.graphql.federated.visitor_model.ChangeVisitorModelService;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.model.VisitorModel;
import com.hayden.graphql.models.visitor.schema.DeleteSchemaApiVisitorModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public record DeleteSchemaToVisitorModels(DeleteSchemaApiVisitorModel toAdd, AtomicBoolean doRemoveAtomic) implements GatewayChangeVisitor {

    public DeleteSchemaToVisitorModels(DeleteSchemaApiVisitorModel toAdd) {
        this(toAdd, new AtomicBoolean(false));
    }

    @Override
    public boolean doRemove() {
        return this.doRemoveAtomic.get();
    }

    /**
     * Synchronized here probably not necessary because caller is synchronized also...
     *
     * @param model
     * @param delegate
     * @return
     */
    @Override
    public synchronized ChangeVisitorModelService.VisitorModelsContext commandIf(ChangeVisitorModelService.VisitorModelsContext model,
                                                                                 ServiceVisitorDelegate.ServiceVisitorDelegateContext delegate) {
        if (this.doRemoveAtomic.get()) {
            return model;
        }

        List<VisitorModel> mutableVisitorModels = new ArrayList<>(model.visitorModels());

        // first add the thing if it isn't there already.
        if (!mutableVisitorModels.contains(toAdd)) {
            mutableVisitorModels.add(toAdd);
            retrieveSchemaToRemove(model).forEach(mutableVisitorModels::remove);
        }

        if (isTypeExistingOnDelegateContext(delegate)) {
            this.doRemoveAtomic.set(true);
        }

        // if it gets added back, then assume that it was removed and added back, and so remove the command to delete.
        if (isTypeExistingOnToPushModels(model)) {
            this.doRemoveAtomic.set(true);
        }

        return new ChangeVisitorModelService.VisitorModelsContext(mutableVisitorModels);
    }

    private boolean isTypeExistingOnToPushModels(ChangeVisitorModelService.VisitorModelsContext model) {
        return model.visitorModels().stream()
                .anyMatch(this::findMatchingSchema);
    }

    private @NotNull Stream<VisitorModel> retrieveSchemaToRemove(ChangeVisitorModelService.VisitorModelsContext model) {
        return model.visitorModels().stream()
                .filter(this::findMatchingSchema);
    }

    private boolean isTypeExistingOnDelegateContext(ServiceVisitorDelegate.ServiceVisitorDelegateContext delegate) {
        // Note that every time discovery runs it replaces the delegates here with the new ones from the services.
        return delegate.delegates().stream()
                .filter(svd -> Objects.equals(svd.host(), this.toAdd.id()))
                .flatMap(s -> s.visitors().entrySet().stream())
                .map(Map.Entry::getValue)
                .noneMatch(svd -> svd.visitor() instanceof GraphQlDataFetcher fetcherDiscoveryModel
                                  && fetcherDiscoveryModel.fetcherSource().stream()
                                          .noneMatch(schema -> Objects.equals(schema.typeName(), toAdd.toDelete())));
    }

    @Override
    public int compareTo(@NotNull ChangeVisitorModel<ServiceVisitorDelegate.ServiceVisitorDelegateContext> o) {
        // needs to be ran before AddToSchemaToVisitorModels
        if (o instanceof AddToSchemaToVisitorModels) {
            assert o.compareTo(this) == 0;
            return -1;
        } else
            return GatewayChangeVisitor.super.compareTo(o);
    }

    private boolean findMatchingSchema(VisitorModel vm) {
        return vm instanceof GraphQlDataFetcherDiscoveryModel fetcherDiscoveryModel
               && fetcherDiscoveryModel.fetcherSource().stream()
                       .anyMatch(schema -> Objects.equals(schema.typeName(), toAdd.toDelete()));
    }
}
