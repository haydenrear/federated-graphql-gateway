package com.hayden.gateway;

import com.hayden.graphql.federated.transport.FederatedGraphQlTransport;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = GraphQlWebMvcAutoConfiguration.class)
@ImportAutoConfiguration(DgsAutoConfiguration.class)
@Import(FederatedGraphQlTransport.class)
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
