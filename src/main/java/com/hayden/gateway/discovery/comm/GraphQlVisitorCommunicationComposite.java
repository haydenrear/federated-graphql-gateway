package com.hayden.gateway.discovery.comm;

import com.hayden.graphql.federated.transport.health.HealthEvent;
import com.hayden.gateway.graphql.Context;
import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.gateway.graphql.RegistriesComposite;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import com.hayden.utilitymodule.result.Result;
import com.hayden.utilitymodule.result.map.ResultCollectors;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class GraphQlVisitorCommunicationComposite implements GraphQlServiceApiVisitor{


    public static final String GRAPHQL_SERVICE = "GRAPHQL_SERVICE";

    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private GraphQlServiceProvider graphQlServiceProvider;
    @Autowired
    private FederatedGraphQlStateHolder stateHolder;

    @EventListener
    public void onApplicationEvent(@NotNull HealthEvent.FailedHealthEvent event) {
        stateHolder.remove(event.getSource().serviceInstanceId());
    }

    @PostConstruct
    public void runDiscovery() {
        Timer timer = new Timer();
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        runDiscoveryInner();
                    }
                },
                0,
                20000
        );
    }

    void runDiscoveryInner() {
        removeExpiredServices();
        Optional.ofNullable(discoveryClient.getInstances(GRAPHQL_SERVICE))
                .stream()
                .flatMap(Collection::stream)
                .filter(s -> Objects.nonNull(s.getHost()))
                // TODO: if currently exists, send a hash of the previous and have the service validate it with ping pong
                .map(s -> Optional.ofNullable(this.stateHolder.getService(s))
                        .flatMap(delegate -> graphQlServiceProvider.getServiceVisitorDelegates(delegate, s.getHost()))
                        .or(() -> graphQlServiceProvider.getServiceVisitorDelegates(s.getHost()))
                )
                .flatMap(Optional::stream)
                .forEach(this::putServices);
    }

    void putServices(ServiceVisitorDelegate service) {
        stateHolder.registerServiceDelegate(service);
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(RegistriesComposite registries, Context.RegistriesContext context) {
        return stateHolder.current()
                .map(s -> s.visit(registries, context))
                .collect(ResultCollectors.from(
                        new GraphQlServiceVisitorResponse(),
                        new GraphQlServiceVisitorError()
                ));

    }

     public boolean doReload() {
        return this.stateHolder.doReload();
    }

    public boolean waitForWasInitialRegistration() {
        return this.stateHolder.awaitPreStartup();
    }

    public boolean waitForAllRegistrations() {
        return this.stateHolder.waitForAllRegistrations();
    }

    private void removeExpiredServices() {
        stateHolder.removeExpiredServices();
    }

    @Override
    public boolean invalidateCurrent() {
        return false;
    }

    @Override
    public boolean remove() {
        return false;
    }

    @Override
    public FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId id() {
        throw new NotImplementedException("Composite is leaky...");
    }

    @Override
    public MessageDigestBytes digest() {
        throw new NotImplementedException("Composite is leaky...");
    }

    @Override
    public boolean isReloadable() {
        throw new NotImplementedException("Composite is leaky...");
    }
}
