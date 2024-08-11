package com.hayden.gateway.graphql;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import com.hayden.graphql.models.visitor.model.*;
import com.hayden.utilitymodule.result.Agg;
import com.hayden.utilitymodule.result.error.AggregateError;
import com.hayden.utilitymodule.result.error.ErrorCollect;
import com.hayden.utilitymodule.result.res.Responses;
import com.hayden.utilitymodule.result.Result;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.BiConsumer;


/**
 * The API actions are pull, as the gateway only registers schema from servers it already knows from service
 * discovery, and so there are different types of actions for the Type and Fetcher.
 */
public interface GraphQlServiceApiVisitor extends Invalidatable, Removable, Id, Digest, Reloadable {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class ContextCallback {
        BiConsumer<String, FederatedGraphQlServiceFetcherItemId> callback;
    }

    record GraphQlServiceVisitorError(Set<ErrorCollect> errors)
            implements AggregateError {

        public GraphQlServiceVisitorError(String error) {
            this(Sets.newHashSet(ErrorCollect.fromMessage(error)));
        }

        public GraphQlServiceVisitorError() {
            this(Sets.newHashSet());
        }
    }

    record GraphQlServiceVisitorResponse(List<String> responses) implements Responses.AggregateResponse {

        public GraphQlServiceVisitorResponse(String response) {
            this(Lists.newArrayList(response));
        }

        public GraphQlServiceVisitorResponse() {
            this(Lists.newArrayList());
        }

        @Override
        public void add(Agg aggregateResponse) {
            if (aggregateResponse instanceof GraphQlServiceVisitorResponse r) {
                this.responses.addAll(r.responses());
            } else {
                Assert.isTrue(false, "Could not merge together different types of aggregate responses.");
            }
        }
    }


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

        Optional.ofNullable(registries.mimeTypeRegistry())
                .map(t -> this.visit(t, context))
                .ifPresent(all::add);

        Optional.ofNullable(registries.federatedGraphQlTransportRegistrar())
                .map(t -> this.visit(t, context))
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
