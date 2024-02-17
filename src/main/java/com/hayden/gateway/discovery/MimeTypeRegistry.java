package com.hayden.gateway.discovery;

import com.hayden.gateway.graphql.GraphQlServiceRegistration;
import com.hayden.utilitymodule.result.Result;
import graphql.schema.DataFetcher;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class MimeTypeRegistry {


    public record DataFetcherRegistration(
            GraphQlServiceRegistration.DataFetcherSourceId id,
            GraphQlServiceRegistration.DataFetcherData dataFetcherData
    ) {}

    private final Map<GraphQlServiceRegistration.DataFetcherSourceId, DataFetcherRegistration> mimeTypes = new ConcurrentHashMap<>();

    public void register(@NotNull DataFetcherRegistration toRegister) {
        mimeTypes.put(toRegister.id, toRegister);
    }

    public Stream<DataFetcherRegistration> fromMimeType(MimeType mimeType) {
        return mimeTypes.entrySet().stream()
                .filter(m -> m.getKey().mimeType().equals(mimeType))
                .map(Map.Entry::getValue);
    }

}
