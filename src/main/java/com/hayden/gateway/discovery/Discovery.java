package com.hayden.gateway.discovery;

import com.hayden.gateway.compile.DgsCompiler;
import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.gateway.graphql.Context;
import com.hayden.gateway.graphql.RegistriesComposite;
import com.hayden.graphql.federated.transport.source.FederatedDynamicGraphQlSource;
import com.netflix.graphql.dgs.DgsCodeRegistry;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsTypeDefinitionRegistry;
import com.netflix.graphql.dgs.internal.method.MethodDataFetcherFactory;
import graphql.schema.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@DgsComponent
@Component
public class Discovery implements ApplicationContextAware {

    @Autowired
    private DgsCompiler compileDgs;
    @Autowired @Delegate
    private GraphQlVisitorCommunication communication;

    /**
     * Can create DataFetcher from @DgsComponent
     */
    @Autowired
    private MethodDataFetcherFactory methodDataFetcherFactory;

    private FederatedGraphQlTransportRegistrar transportRegistrar;
    private ApplicationContext ctx;

    private final MimeTypeRegistry mimetypeRegistry;
    private final TypeDefinitionRegistry typeDefinitionRegistry;
    private final Context.CodegenContext codegenContext;
    private final FederatedDynamicGraphQlSource federatedDynamicGraphQlSource;


    public Discovery(FederatedGraphQlTransportRegistrar transportRegistrar,
                     MimeTypeRegistry mimetypeRegistry,
                     FederatedDynamicGraphQlSource federatedDynamicGraphQlSource) {
        this.typeDefinitionRegistry = new TypeDefinitionRegistry();
        this.codegenContext = new Context.CodegenContext(this.compileDgs);
        this.mimetypeRegistry = mimetypeRegistry;
        this.transportRegistrar = transportRegistrar;
        this.federatedDynamicGraphQlSource = federatedDynamicGraphQlSource;
    }

    @DgsCodeRegistry
    public GraphQLCodeRegistry.Builder codeRegistryBuilder(GraphQLCodeRegistry.Builder codeRegistryBuilder,
                                                           TypeDefinitionRegistry registry) {
        // if you provide the params fetcher as a proto call and then convert it into graphql at this layer, then
        // you can use it. Because the DataFetcher is an abstraction provided by the service, so it can say anything
        // it wants, if they can provide the code to serialize it. So the goal would be to have the Gateway be a
        // GraphQl interface and then the services are agnostic to serialization by being able to provide their own
        // serialization framework at runtime.
        this.federatedDynamicGraphQlSource.setReload();
        this.communication.visit(
                new RegistriesComposite(registry, codeRegistryBuilder, this.mimetypeRegistry, this.transportRegistrar),
                new Context.RegistriesContext(
                        new Context.TypeDefinitionContext(),
                        codegenContext,
                        new Context.GraphQlTransportContext(new ArrayList<>()),
                        this.ctx
                )
        );
        return codeRegistryBuilder;
    }

    @DgsTypeDefinitionRegistry
    public TypeDefinitionRegistry registry() {
        waitForInitialRegistration();
        this.federatedDynamicGraphQlSource.setReload();
        this.communication.visit(
                new RegistriesComposite(this.typeDefinitionRegistry, this.mimetypeRegistry, this.transportRegistrar),
                new Context.RegistriesContext(
                        new Context.TypeDefinitionContext(),
                        codegenContext,
                        new Context.GraphQlTransportContext(new ArrayList<>()),
                        this.ctx
                )
        );
        return this.typeDefinitionRegistry;
    }

    @Override
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }
}
