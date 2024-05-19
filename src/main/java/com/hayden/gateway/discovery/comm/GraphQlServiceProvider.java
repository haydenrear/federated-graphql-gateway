package com.hayden.gateway.discovery.comm;

import com.hayden.gateway.discovery.ApiVisitorFactory;
import com.hayden.gateway.discovery.DiscoveryProperties;
import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.graphql.models.visitor.model.Digest;
import com.hayden.graphql.models.visitor.model.Id;
import com.hayden.graphql.models.visitor.model.VisitorModel;
import com.hayden.utilitymodule.MapFunctions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GraphQlServiceProvider {

    private final ApiVisitorFactory factory;
    private final DiscoveryProperties discoveryProperties;

    @Value("${federated.version:0.0.1}")
    private String version;

    public record ServiceVisitorDelegateRequest(List<ServiceVisitorValidation> visitors) {}
    public record ServiceVisitorValidation(MessageDigestBytes digest) implements Digest {}
    public record ServiceVisitorRequest(Map<Class<? extends GraphQlServiceApiVisitor>, ServiceVisitorValidation> visitorDelegateType,
                                        ServiceVisitorValidation parentValidation) implements Digest {
        @Override
        public MessageDigestBytes digest() {
            return parentValidation.digest();
        }
    }

    public Optional<ServiceVisitorDelegate> getServiceVisitorDelegates(String host) {
        var r = RestClient.create().get()
                .uri("%s/%s".formatted(host, "/api/v1/graphql"))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<VisitorModel.VisitorResponse>() {});

        if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null) {
            VisitorModel.VisitorResponse body = r.getBody();
            return body.visitorModels().stream().map(Id::id).findAny()
                    .map(id -> new ServiceVisitorDelegate(
                            id,
                            MapFunctions.CollectMap(
                                    body.visitorModels().stream()
                                            .filter(Objects::nonNull)
                                            .filter(v -> v.version().equals(version))
                                            .map(factory::from)
                                            .flatMap(Optional::stream)
                                            .map(e -> Map.entry(e.visitor().getClass(), e))
                            ),
                            new DelayedService(id, discoveryProperties.getDiscoveryPingSeconds()),
                            body.digest()
                    ));
        } else {
            log.error("Error when attempting to get graphql schema for: {}.", r.getBody());
        }
        return Optional.empty();
    }

}
