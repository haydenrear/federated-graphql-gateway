package com.hayden.gateway.discovery;

import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.gateway.graphql.GraphQlServiceRegistration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@Slf4j
public class GraphQlServiceProvider {

    public record ServiceVisitorDelegate(String host, List<? extends GraphQlServiceApiVisitor> visitors) {}


    public Optional<ServiceVisitorDelegate> getServiceVisitorDelegates(String host,
                                                                       List<Class<? extends GraphQlServiceApiVisitor>> visitors) {
        var r = RestClient.create().get()
                .uri("%s/%s".formatted(host, "graphql"))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<GraphQlServiceRegistration>>() {});

        if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null) {
            return Optional.of(
                    new ServiceVisitorDelegate(
                            host,
                            Optional.ofNullable(r.getBody()).stream()
                                    .flatMap(Collection::stream)
                                    .filter(Objects::nonNull)
                                    .toList()
                    )
            );
        } else {
            log.error("Error when attempting to get graphql schema for: {}.", r.getBody());
        }
        return Optional.empty();
    }

}
