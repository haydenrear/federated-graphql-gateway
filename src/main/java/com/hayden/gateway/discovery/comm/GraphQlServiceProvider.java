package com.hayden.gateway.discovery.comm;

import com.hayden.gateway.discovery.visitor.ApiVisitorFactory;
import com.hayden.gateway.discovery.DiscoveryProperties;
import com.hayden.graphql.federated.visitor_model.*;
import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.gateway.discovery.visitor.ServiceVisitorDelegate;
import com.hayden.graphql.models.visitor.model.Digest;
import com.hayden.graphql.models.visitor.model.Id;
import com.hayden.graphql.models.visitor.model.VisitorModel;
import com.hayden.utilitymodule.MapFunctions;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GraphQlServiceProvider {

    private final ApiVisitorFactory factory;
    private final DiscoveryProperties discoveryProperties;
    private final ChangeVisitorModelService changeVisitorModelService;

    @Value("${federated.version:0.0.1}")
    private String version;

    public record ServiceVisitorValidation(MessageDigestBytes digest) implements Digest {}
    public record ServiceVisitorRequest(Map<Class<? extends GraphQlServiceApiVisitor>, ServiceVisitorValidation> visitorDelegateType,
                                        ServiceVisitorValidation parentValidation) implements Digest {

        public ServiceVisitorRequest() {
            this(new HashMap<>(), new ServiceVisitorValidation(new MessageDigestBytes(new byte[] {})));
        }

        @Override
        public MessageDigestBytes digest() {
            return parentValidation.digest();
        }
    }

    public Optional<ServiceVisitorDelegate> getServiceVisitorDelegates(String host) {
        return this.getServiceVisitorDelegates(null, host);
    }

    public Optional<ServiceVisitorDelegate> getServiceVisitorDelegates(@Nullable ServiceVisitorDelegate delegate, String host) {
        Optional<ServiceVisitorDelegate> definitelyAnOptional = Optional.of(callFederatedService(delegate, host))
                .filter(r -> r.getStatusCode().is2xxSuccessful())
                .flatMap(r -> Optional.ofNullable(r.getBody()))
                .flatMap(body -> body.visitorModels().stream().map(Id::id)
                        .findAny()
                        .map(id -> new ServiceVisitorDelegate(
                                id,
                                MapFunctions.Collect(
                                        body.visitorModels().stream()
                                                .filter(Objects::nonNull)
                                                .filter(v -> v.version().equals(version))
                                                .map(factory::from)
                                                .flatMap(Optional::stream)
                                                .map(e -> Map.entry(e.visitor().getClass(), e)),
                                        new HashMap<>()
                                ),
                                new DelayedService(id, discoveryProperties.getDiscoveryPingSeconds()),
                                body.digest()
                        )));

        definitelyAnOptional.ifPresent(this::registerChangesToNetworkingInfo);

        return definitelyAnOptional;
    }

    private void registerChangesToNetworkingInfo(ServiceVisitorDelegate svd) {
//        svd.visitors().entrySet().stream()
//                .flatMap(e -> new GatewayChangeVisitor() {
//
////                    private final AtomicBoolean doAction = new AtomicBoolean(true);
//
////                    public boolean doAction(VisitorModel model) {
////                        return doAction.compareAndSet(true, false);
////                    }
//
//                    @Override
//                    public boolean doRemove() {
//                        return true;
//                    }
//
//                    @Override
//                    public ChangeVisitorModelServiceImpl.VisitorModelsContext commandIf(ChangeVisitorModelServiceImpl.VisitorModelsContext model,
//                                                                                        ServiceVisitorDelegate.ServiceVisitorDelegateContext delegate) {
//                        // TODO:
//                        return model;
//                    }
//                })
//                .forEach(this.changeVisitorModelService::register);

    }

    private static @NotNull ResponseEntity<VisitorModel.VisitorResponse> callFederatedService(@Nullable ServiceVisitorDelegate delegate, String host) {
        return RestClient.create().post()
                .uri("%s/%s".formatted(host, "/api/v1/graphql"))
                .body(Optional.ofNullable(delegate)
                        .map(s -> new ServiceVisitorRequest(
                                MapFunctions.CollectMap(
                                        s.visitors().entrySet().stream()
                                                .map(e -> Map.entry(
                                                        e.getKey(),
                                                        new ServiceVisitorValidation(e.getValue().digest())))),
                                new ServiceVisitorValidation(s.digest())
                        ))
                        .orElse(new ServiceVisitorRequest())
                )
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});
    }

}
