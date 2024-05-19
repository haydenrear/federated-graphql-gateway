package com.hayden.gateway.discovery;

import com.hayden.gateway.compile.DgsCompiler;
import com.hayden.gateway.discovery.comm.FederatedGraphQlState;
import com.hayden.gateway.discovery.comm.FederatedGraphQlStateHolder;
import com.hayden.gateway.discovery.comm.GraphQlVisitorCommunicationComposite;
import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.gateway.graphql.Context;
import com.hayden.gateway.graphql.RegistriesComposite;
import com.netflix.graphql.dgs.DgsCodeRegistry;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsTypeDefinitionRegistry;
import graphql.schema.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@DgsComponent
@Component
public class Discovery implements ApplicationContextAware {


    private ApplicationContext ctx;

    @Delegate
    private final GraphQlVisitorCommunicationComposite communication;

    private final FederatedGraphQlTransportRegistrar transportRegistrar;
    private final MimeTypeRegistry mimetypeRegistry;
    private final TypeDefinitionRegistry typeDefinitionRegistry;
    private final Context.CodegenContext codegenContext;
    private final FederatedGraphQlStateHolder federatedGraphQlStateHolder;



    public Discovery(FederatedGraphQlTransportRegistrar transportRegistrar,
                     MimeTypeRegistry mimetypeRegistry,
                     DgsCompiler dgsCompiler,
                     GraphQlVisitorCommunicationComposite communication,
                     FederatedGraphQlStateHolder federatedGraphQlStateHolder) {
        this.federatedGraphQlStateHolder = federatedGraphQlStateHolder;
        this.typeDefinitionRegistry = new TypeDefinitionRegistry();
        this.codegenContext = new Context.CodegenContext(dgsCompiler);
        this.mimetypeRegistry = mimetypeRegistry;
        this.transportRegistrar = transportRegistrar;
        this.communication = communication;
    }

    @DgsCodeRegistry
    public GraphQLCodeRegistry.Builder codeRegistryBuilder(GraphQLCodeRegistry.Builder codeRegistryBuilder,
                                                           TypeDefinitionRegistry registry) {
        var result = this.communication.visit(
                new RegistriesComposite(registry, codeRegistryBuilder, this.mimetypeRegistry, this.transportRegistrar),
                new Context.RegistriesContext(
                        new Context.TypeDefinitionContext(),
                        codegenContext,
                        new Context.GraphQlTransportContext(new ArrayList<>()),
                        this.ctx
                )
        );

        Optional.ofNullable(result.error())
                .ifPresent(e -> log.error("Found error when running code registry builder: {}.", e.getMessages()));

        federatedGraphQlStateHolder.registerStartupTask(FederatedGraphQlState.StartupTask.CODE_REGISTRY);
        return codeRegistryBuilder;
    }

    @DgsTypeDefinitionRegistry
    public TypeDefinitionRegistry registry() {
        var result = this.communication.visit(
                new RegistriesComposite(this.typeDefinitionRegistry, this.mimetypeRegistry, this.transportRegistrar),
                new Context.RegistriesContext(
                        new Context.TypeDefinitionContext(),
                        codegenContext,
                        new Context.GraphQlTransportContext(new ArrayList<>()),
                        this.ctx
                )
        );

        Optional.ofNullable(result.error())
                .ifPresent(e -> log.error("Found error when running type registry: {}.", e.getMessages()));

        federatedGraphQlStateHolder.registerStartupTask(FederatedGraphQlState.StartupTask.TYPE_DEFINITION_REGISTRY);
        return this.typeDefinitionRegistry;
    }

    @Override
    @Autowired
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }
}
