package com.hayden.gateway.graphql;

import com.hayden.gateway.compile.DgsCompiler;
import com.hayden.graphql.federated.transport.FederatedGraphQlTransport;

import java.util.ArrayList;
import java.util.List;

public interface Context {
    record CodegenContext(DgsCompiler javaCompiler) implements Context {}

    record TypeDefinitionContext() implements Context {}

    record MimeTypeDefinitionContext(
            List dataFetchers) implements Context {}

    record GraphQlTransportContext(List<FederatedGraphQlTransport.GraphQlTransportRegistration> transportRegistrations) {


    }

    record RegistriesContext(TypeDefinitionContext typeDefinitionContext,
                             CodegenContext codegenContext,
                             MimeTypeDefinitionContext mimeTypeDefinitionContext,
                             GraphQlTransportContext transportContext) {
        public RegistriesContext(TypeDefinitionContext typeDefinitionContext, CodegenContext codegenContext, GraphQlTransportContext graphQlTransportContext) {
            this(typeDefinitionContext, codegenContext,
                    new MimeTypeDefinitionContext(new ArrayList<>()),
                    graphQlTransportContext);
        }
    }
}
