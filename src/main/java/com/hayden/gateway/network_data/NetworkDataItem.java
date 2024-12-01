package com.hayden.gateway.network_data;

import com.hayden.graphql.models.dataservice.RemoteDataFetcher;
import com.hayden.graphql.models.visitor.model.VisitorModel;

import java.util.ArrayList;
import java.util.List;

public record NetworkDataItem(VisitorModel model, List<RemoteDataFetcher.RequestTemplate> document) {

    public NetworkDataItem(VisitorModel model) {
        this(model, new ArrayList<>());
    }

}
