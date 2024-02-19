package com.hayden.gateway.discovery;

import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;

import java.util.List;

public record ServiceVisitorDelegate(String host,
                                     List<? extends GraphQlServiceApiVisitor> visitors) {
}
