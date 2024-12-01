package com.hayden.gateway.network_data;


import com.hayden.gateway.discovery.visitor.ServiceVisitorDelegate;
import com.hayden.gateway.discovery.comm.FederatedGraphQlStateTransitions;
import com.hayden.graphql.federated.visitor_model.ChangeVisitorModelService;
import com.hayden.graphql.federated.visitor_model.VisitorModelService;
import com.hayden.graphql.models.dataservice.RemoteDataFetcher;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherGraphQlSource;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.model.VisitorModel;
import graphql.schema.DataFetcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/graphql")
public class NetworkDataController {

    private final VisitorModelService models;

    private final FederatedGraphQlStateTransitions visitorDelegateState;

    @PostConstruct
    public void initializeVisitorModelService() {
    }

    /**
     * @return
     */
    @GetMapping
    public List<NetworkDataItem> getSources() {
        return models.models(new ServiceVisitorDelegate.ServiceVisitorDelegateContext(visitorDelegateState.getServices()))
                .sendVisitorModels()
                .stream()
                .map(vm -> {
                    if (vm instanceof GraphQlDataFetcherDiscoveryModel discoveryModel) {
                        // these requestTemplates eventually turn into the template for the function calling service to call
                        var requestTemplates = discoveryModel.fetcherSource().stream()
                                .flatMap(NetworkDataController::loadDataFetcherClassFromTypeName)
                                .flatMap(c -> {
                                    try {
                                        return Stream.of((DataFetcher) c.getConstructor().newInstance());
                                    } catch (NoSuchMethodException n) {
                                        log.error("GraphQL data fetcher {} did not have no argument constructor. However it needs this constructor.", c.getName(), n);
                                        return Stream.empty();
                                    }catch (
                                            InstantiationException |
                                            IllegalAccessException |
                                            InvocationTargetException e) {
                                        log.error("Error when searching for data fetcher graphql type: {}", c.getName(), e);
                                        return Stream.empty();
                                    }
                                })
                                .flatMap(dataFetcher -> dataFetcher instanceof RemoteDataFetcher<?> rdf
                                                ? Stream.of(rdf)
                                                : Stream.empty())
                                .flatMap(rdf -> rdf.requestTemplates().stream())
                                .toList();

                        return new NetworkDataItem(vm, requestTemplates);
                    }

                    return new NetworkDataItem(vm);
                })
                .toList();
    }

    private static @NotNull Stream<? extends Class<?>> loadDataFetcherClassFromTypeName(DataFetcherGraphQlSource dataFetcherGraphQlSource) {
            return dataFetcherGraphQlSource.sources()
                    .values().stream()
                    .map(dataSource -> "%s.%s".formatted(dataSource.sourceMetadata().packageName(), dataFetcherGraphQlSource.typeName()))
                    .flatMap(tyName -> {
                        try {
                            return Stream.of(Class.forName(tyName));
                        } catch (
                                ClassNotFoundException e) {
                            log.error("Error when searching for data fetcher graphql type: {}", dataFetcherGraphQlSource.typeName(), e);
                            return Stream.empty();
                        }
                    });
    }

}
