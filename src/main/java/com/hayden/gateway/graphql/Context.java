package com.hayden.gateway.graphql;

import com.hayden.gateway.compile.DgsCompiler;
import com.hayden.graphql.federated.transport.register.GraphQlRegistration;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

public interface Context {
    record CodegenContext(DgsCompiler javaCompiler) implements Context {}

    record TypeDefinitionContext() implements Context {}

    record MimeTypeDefinitionContext(List<GraphQlDataFetcher.DataFetcherRegistration> dataFetchers) implements Context {}

    record GraphQlTransportContext(List<GraphQlRegistration.GraphQlTransportFederatedGraphQlRegistration> transportRegistrations) implements Context {
    }

    record RegistriesContext(TypeDefinitionContext typeDefinitionContext,
                             CodegenContext codegenContext,
                             MimeTypeDefinitionContext mimeTypeDefinitionContext,
                             GraphQlTransportContext transportContext,
                             ApplicationContext ctx) {
        public RegistriesContext(TypeDefinitionContext typeDefinitionContext, CodegenContext codegenContext, GraphQlTransportContext graphQlTransportContext,
                                 ApplicationContext ctx) {
            this(typeDefinitionContext, codegenContext,
                    new MimeTypeDefinitionContext(new ArrayList<>()),
                    graphQlTransportContext, ctx);
        }
    }
}
