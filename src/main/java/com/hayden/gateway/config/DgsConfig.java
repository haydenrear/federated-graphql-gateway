package com.hayden.gateway.config;

import com.hayden.gateway.discovery.Discovery;
import com.hayden.graphql.federated.client.FederatedGraphQlClientBuilderHolder;
import com.hayden.graphql.federated.transport.FederatedGraphQlTransport;
import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;

@Configuration
@ComponentScan(basePackageClasses = {FederatedGraphQlTransport.class})
public class DgsConfig {

    @Bean
    @Primary
    public DefaultDgsQueryExecutor.ReloadSchemaIndicator reloadSchemaIndicator(Discovery discovery) {
        return discovery::doReload;
    }

    @Bean
    @Primary
    @SuppressWarnings("rawtypes")
    public Encoder encoder() {
         return FederatedGraphQlClientBuilderHolder.DefaultJackson2Codecs.encoder();
    }

    @Bean
    @Primary
    @SuppressWarnings("rawtypes")
    public Decoder decoder() {
        return FederatedGraphQlClientBuilderHolder.DefaultJackson2Codecs.decoder();
    }


}
