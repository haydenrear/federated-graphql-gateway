package com.hayden.gateway.discovery;

import com.hayden.gateway.graphql.*;
import com.hayden.graphql.models.visitor.datafetcher.DeleteSchemaApiVisitorModel;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.VisitorModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.hayden.graphql.models.visitor.datafed.GraphQlDataFederationModel;
import com.hayden.graphql.models.visitor.simpletransport.GraphQlTransportModel;

import java.util.Optional;

@Component
@Slf4j
public class ApiVisitorFactory {

    public Optional<GraphQlServiceApiVisitor> from(VisitorModel value) {
        return switch(value)  {
            case GraphQlDataFetcherDiscoveryModel model -> Optional.of(new GraphQlDataFetcher(model));
            case DeleteSchemaApiVisitorModel model -> Optional.of(new DeleteSchemaApiVisitor(model));
            case GraphQlTransportModel model -> Optional.of(new GraphQlTransports(model));
            case GraphQlDataFederationModel model -> Optional.of(new GraphQlDataFederation(model));
            default -> {
                log.error("Found visitor for which logic does not exist: {}.", value.getClass().getSimpleName());
                yield Optional.empty();
            }
        };
    }

}
