package com.hayden.gateway.network_data;


import com.hayden.gateway.discovery.visitor.ServiceVisitorDelegate;
import com.hayden.gateway.discovery.comm.FederatedGraphQlStateTransitions;
import com.hayden.graphql.federated.visitor_model.ChangeVisitorModelService;
import com.hayden.graphql.federated.visitor_model.VisitorModelService;
import com.hayden.graphql.models.visitor.model.VisitorModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/graphql")
public class NetworkDataController {

    private final ChangeVisitorModelService visitorModelService;

    private final VisitorModelService models;

    private final FederatedGraphQlStateTransitions visitorDelegateState;

    @PostConstruct
    public void initializeVisitorModelService() {
    }

    /**
     * @return
     */
    @GetMapping
    public List<VisitorModel> getSources() {
        return models.models(new ServiceVisitorDelegate.ServiceVisitorDelegateContext(visitorDelegateState.getServices()))
                .sendVisitorModels();
    }

}
