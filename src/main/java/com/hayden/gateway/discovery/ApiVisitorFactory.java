package com.hayden.gateway.discovery;

import com.hayden.gateway.graphql.DeleteSchemaApiVisitor;
import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.gateway.graphql.GraphQlDataFetcher;
import com.hayden.graphql.models.visitor.datafetcher.DeleteSchemaApiVisitorModel;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.VisitorModel;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ApiVisitorFactory {

    public Optional<GraphQlServiceApiVisitor> from(VisitorModel value) {
        return switch(value)  {
            case GraphQlDataFetcherDiscoveryModel model -> Optional.of(new GraphQlDataFetcher(model));
            case DeleteSchemaApiVisitorModel model -> Optional.of(new DeleteSchemaApiVisitor(model));
            default -> Optional.empty();
        };
    }

}
