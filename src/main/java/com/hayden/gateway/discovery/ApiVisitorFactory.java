package com.hayden.gateway.discovery;

import com.hayden.gateway.discovery.comm.ServiceVisitorDelegate;
import com.hayden.gateway.graphql.*;
import com.hayden.graphql.models.visitor.schema.DeleteSchemaApiVisitorModel;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.model.VisitorModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.hayden.graphql.models.visitor.simpletransport.GraphQlTransportModel;

import java.util.Optional;

@Component
@Slf4j
public class ApiVisitorFactory {

    public Optional<ServiceVisitorDelegate.ServiceVisitor> from(VisitorModel.VisitorModelResponse value) {
        return switch(value.model())  {
            case GraphQlDataFetcherDiscoveryModel model -> Optional.of(new GraphQlDataFetcher(model, value.digest())).map(ServiceVisitorDelegate.ServiceVisitor::new);
            case DeleteSchemaApiVisitorModel model -> Optional.of(new DeleteSchemaApiVisitor(model, value.digest())).map(ServiceVisitorDelegate.ServiceVisitor::new);
            case GraphQlTransportModel model -> Optional.of(new GraphQlTransports(model, value.digest())).map(ServiceVisitorDelegate.ServiceVisitor::new);
            default -> {
                log.error("Found visitor for which logic does not exist: {}.", value.getClass().getSimpleName());
                yield Optional.empty();
            }
        };
    }

}
