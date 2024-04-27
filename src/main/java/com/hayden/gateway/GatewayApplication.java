package com.hayden.gateway;

import com.hayden.graphql.federated.FederatedExecutionGraphQlService;
import com.hayden.graphql.models.GraphQlTarget;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ImportAutoConfiguration({GraphQlAutoConfiguration.class, DgsAutoConfiguration.class})
@EnableConfigurationProperties(GraphQlProperties.class)
@ComponentScan(basePackageClasses = {GraphQlTarget.class, FederatedExecutionGraphQlService.class, GatewayApplication.class})
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
