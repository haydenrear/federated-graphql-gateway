package com.hayden.gateway.discovery;

import com.hayden.gateway.compile.DgsCompiler;
import com.hayden.gateway.discovery.comm.FederatedGraphQlState;
import com.hayden.gateway.discovery.comm.FederatedGraphQlStateTransitions;
import com.hayden.gateway.discovery.comm.GraphQlVisitorCommunicationComposite;
import com.hayden.gateway.federated.FederatedGraphQlTransportRegistrar;
import com.hayden.gateway.graphql.Context;
import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.gateway.graphql.RegistriesComposite;
import com.hayden.utilitymodule.result.Result;
import com.hayden.utilitymodule.result.error.ErrorCollect;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final FederatedGraphQlStateTransitions federatedGraphQlStateTransitions;



    public Discovery(FederatedGraphQlTransportRegistrar transportRegistrar,
                     MimeTypeRegistry mimetypeRegistry,
                     DgsCompiler dgsCompiler,
                     GraphQlVisitorCommunicationComposite communication,
                     FederatedGraphQlStateTransitions federatedGraphQlStateTransitions) {
        this.federatedGraphQlStateTransitions = federatedGraphQlStateTransitions;
        this.typeDefinitionRegistry = new TypeDefinitionRegistry();
        this.codegenContext = new Context.CodegenContext(dgsCompiler);
        this.mimetypeRegistry = mimetypeRegistry;
        this.transportRegistrar = transportRegistrar;
        this.communication = communication;
    }

    @Override
    @Autowired
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
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
                ));

        logErrors(result);

        federatedGraphQlStateTransitions.registerStartupTask(FederatedGraphQlState.StartupTask.CODE_REGISTRY);
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

        logErrors(result);

        federatedGraphQlStateTransitions.registerStartupTask(FederatedGraphQlState.StartupTask.TYPE_DEFINITION_REGISTRY);
        return this.typeDefinitionRegistry;
    }

    private static void logErrors(Result<GraphQlServiceApiVisitor.GraphQlServiceVisitorResponse, GraphQlServiceApiVisitor.GraphQlServiceVisitorError> result) {
        Optional.ofNullable(result.error())
                .filter(Result.ResultTy::isPresent)
                .stream()
                .flatMap(e -> !e.get().errors().isEmpty() ? Stream.of(e.get().errors()) : Stream.empty())
                .findAny()
                .ifPresent(e -> log.error("Found error when running code registry builder: {}.", printError(e)));
    }

    private static @NotNull String printError(Set<ErrorCollect> e) {
        return e.stream().map(g -> g.getMessage()).collect(Collectors.joining(", "));
    }
}
