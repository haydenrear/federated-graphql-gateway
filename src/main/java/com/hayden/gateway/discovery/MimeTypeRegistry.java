package com.hayden.gateway.discovery;

import com.hayden.gateway.graphql.GraphQlDataFetcher;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class MimeTypeRegistry {


//    private final Map<DataFetcherSourceId, GraphQlDataFetcher.DataFetcherRegistration> mimeTypes
//            = new ConcurrentHashMap<>();
//
//    public void register(@NotNull GraphQlDataFetcher.DataFetcherRegistration toRegister) {
//        mimeTypes.put(toRegister.id(), toRegister);
//    }
//
//    public Stream<GraphQlDataFetcher.DataFetcherRegistration> fromMimeType(MimeType mimeType) {
//        return mimeTypes.entrySet().stream()
//                .filter(m -> m.getKey().mimeType().equals(mimeType))
//                .map(Map.Entry::getValue);
//    }
//
}
