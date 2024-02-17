package com.hayden.gateway.config;

import com.hayden.gateway.discovery.Discovery;
import com.netflix.graphql.dgs.DgsRuntimeWiring;
import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DgsConfig {

    @Bean
    @Primary
    public DefaultDgsQueryExecutor.ReloadSchemaIndicator reloadSchemaIndicator(Discovery discovery) {
        return discovery::doReload;
    }


}
