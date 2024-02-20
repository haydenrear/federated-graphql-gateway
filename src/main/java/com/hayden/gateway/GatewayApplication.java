package com.hayden.gateway;

import com.hayden.gateway.config.DgsConfig;
import com.hayden.graphql.federated.transport.FederatedDynamicGraphQlSource;
import com.hayden.graphql.federated.transport.FederatedGraphQlTransport;
import com.hayden.graphql.federated.transport.FederatedItemGraphQlTransport;
import com.hayden.graphql.federated.transport.FetcherGraphQlTransport;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ImportAutoConfiguration({GraphQlAutoConfiguration.class, DgsAutoConfiguration.class})
@EnableConfigurationProperties(GraphQlProperties.class)
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
