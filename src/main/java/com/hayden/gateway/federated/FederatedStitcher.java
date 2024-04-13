package com.hayden.gateway.federated;

import com.hayden.graphql.models.client.ClientRequest;
import com.hayden.gateway.discovery.MimeTypeRegistry;
import com.hayden.utilitymodule.MapFunctions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FederatedStitcher {

    private final MimeTypeRegistry mimeTypeRegistry;

    public record FederatedGraphQlRequest(String query, Map<String, Object> variables) {}

    public Map<MimeType, List<FederatedGraphQlRequest>> toQueries(ClientRequest request) {
        return MapFunctions.CollectMap(
                        Arrays.stream(request.requests())
                                .flatMap(c -> mimeTypeRegistry.fromMimeType(c.type())
                                        .map(d -> Map.entry(
                                                c.type(),
                                                new FederatedGraphQlRequest(d.dataFetcherData().template().toQuery(c.params()), d.dataFetcherData().template().vars()))
                                        )
                                )
                                .collect(Collectors.collectingAndThen(
                                        Collectors.groupingBy(Map.Entry::getKey),
                                        e -> e.entrySet().stream().map(v -> Map.entry(v.getKey(), v.getValue().stream().map(Map.Entry::getValue).toList())))
                                )
                );
    }
}
