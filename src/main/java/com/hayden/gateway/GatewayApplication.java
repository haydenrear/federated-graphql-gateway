package com.hayden.gateway;

import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ImportAutoConfiguration({GraphQlAutoConfiguration.class, DgsAutoConfiguration.class})
@EnableConfigurationProperties(GraphQlProperties.class)
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
