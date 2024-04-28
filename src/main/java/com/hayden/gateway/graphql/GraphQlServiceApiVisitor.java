package com.hayden.gateway.graphql;

import com.google.common.collect.Lists;
import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceItemId;
import com.hayden.utilitymodule.result.Result;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * The API actions are pull, as the gateway only registers schema from servers it already knows from service
 * discovery, and so there are different types of actions for the Type and Fetcher.
 */
public interface GraphQlServiceApiVisitor {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class ContextCallback {
        BiConsumer<String, FederatedGraphQlServiceItemId> callback;
    }

    record GraphQlServiceVisitorError(List<Result.Error> errors)
            implements Result.AggregateError {

        public GraphQlServiceVisitorError(String error) {
            this(Lists.newArrayList(Result.Error.fromMessage(error)));
        }

        public GraphQlServiceVisitorError() {
            this(Lists.newArrayList());
        }
    }

    record GraphQlServiceVisitorResponse(List<String> responses) implements Result.AggregateResponse {

        public GraphQlServiceVisitorResponse(String response) {
            this(Lists.newArrayList(response));
        }

        public GraphQlServiceVisitorResponse() {
            this(Lists.newArrayList());
        }

        @Override
        public void add(Result.AggregateResponse aggregateResponse) {
            if (aggregateResponse instanceof GraphQlServiceVisitorResponse r) {
                this.responses.addAll(r.responses());
            } else {
                Assert.fail("Could not merge together different types of aggregate responses.");
            }
        }
    }

    void remove();

    String id();

    default Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(RegistriesComposite registries, Context.RegistriesContext context) {
        List<Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError>> all = new ArrayList<>();

        Optional.ofNullable(registries.typeDefinitionRegistry())
                .map(t -> this.visit(t, context))
                .ifPresent(all::add);

        Optional.ofNullable(registries.codeRegistry())
                .flatMap(b -> Optional.ofNullable(registries.typeDefinitionRegistry())
                        .map(t -> Pair.of(b, t))
                )
                .map(t -> this.visit(registries.codeRegistry(), registries.typeDefinitionRegistry(), context))
                .ifPresent(all::add);

        Optional.ofNullable(registries.mimeTypeRegistry()).map(t -> this.visit(t, context))
                .ifPresent(all::add);

        Optional.ofNullable(registries.mimeTypeRegistry()).map(t -> this.visit(t, context))
                .ifPresent(all::add);

        return Result.all(all);
    }

    default Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(TypeDefinitionRegistry typeDefinitionRegistry, Context.RegistriesContext ctx) {
        return Result.ok(new GraphQlServiceVisitorResponse("No op."));
    }

    default Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(GraphQLCodeRegistry.Builder codeRegistryBuilder, TypeDefinitionRegistry registry, Context.RegistriesContext ctx) {
        return Result.ok(new GraphQlServiceVisitorResponse("No op."));
    }

    default Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(MimeTypeRegistry mimeTypeRegistry, Context.RegistriesContext ctx) {
        return Result.ok(new GraphQlServiceVisitorResponse("No op."));
    }

    default Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(FederatedGraphQlTransportRegistrar federatedGraphQlTransportRegistrar,
                                                             Context.RegistriesContext registriesContext) {
        return Result.ok(new GraphQlServiceVisitorResponse("No op."));
    }

}
