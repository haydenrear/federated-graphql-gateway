package com.hayden.gateway.config;

import com.hayden.gateway.federated.FederatedGraphQlMessageHandler;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.MessageChannel;

import java.util.List;

@Configuration
public class IntegrationConfig {

    @Bean
    public MessageChannel messageChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow integrationFlow(FederatedGraphQlMessageHandler graphQlMessageHandler) {
        return IntegrationFlow.from(
                        Http.inboundGateway("/hello")
                                .requestMapping(r -> r.methods(HttpMethod.POST))
                )
                .handle(graphQlMessageHandler)
                .get();
    }

    @Bean
    @Profile("deploy-test")
    public DiscoveryClient discoveryClient() {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return null;
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                return null;
            }

            @Override
            public List<String> getServices() {
                return null;
            }
        };
    }

}
