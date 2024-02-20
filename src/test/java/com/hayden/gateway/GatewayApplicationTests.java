package com.hayden.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;

@SpringBootTest
class GatewayApplicationTests {

	@MockBean
	DiscoveryClient discoveryClient;

	@Test
	void contextLoads() {
	}

}
