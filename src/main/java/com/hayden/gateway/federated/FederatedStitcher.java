package com.hayden.gateway.federated;

import com.hayden.gateway.client.ClientRequest;
import com.hayden.gateway.discovery.MimeTypeRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FederatedStitcher {

    private final MimeTypeRegistry mimeTypeRegistry;

    public List<String> toQueries(ClientRequest request) {
        return Arrays.stream(request.requests())
                .map(c -> mimeTypeRegistry.fromMimeType(c.type())
                        .map(d -> d.dataFetcherData().template().toQuery(c.params()))
                        .findAny()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
