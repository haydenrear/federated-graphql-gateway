package com.hayden.gateway.discovery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("discovery")
@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscoveryProperties {

    int discoveryPingSeconds;

}
