package com.hayden.gateway.discovery;

import com.hayden.graphql.models.visitor.VisitorModel;
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

    @Value("${federated.version:0.0.1}")
    private String version;


    public Optional<ServiceVisitorDelegate> getServiceVisitorDelegates(String host) {
        var r = RestClient.create().get()
                .uri("%s/%s".formatted(host, "/api/v1/graphql"))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<VisitorModel>>() {});

        if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null) {
            return Optional.of(
                    new ServiceVisitorDelegate(
                            host,
                            Optional.ofNullable(r.getBody()).stream()
                                    .flatMap(Collection::stream)
                                    .filter(Objects::nonNull)
                                    .filter(v -> v.version().equals(version))
                                    .map(factory::from)
                                    .flatMap(Optional::stream)
                                    .toList()
                    )
            );
        } else {
            log.error("Error when attempting to get graphql schema for: {}.", r.getBody());
        }
        return Optional.empty();
    }

}
