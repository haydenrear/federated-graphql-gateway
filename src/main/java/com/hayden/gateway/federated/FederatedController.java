package com.hayden.gateway.federated;

import com.hayden.gateway.client.ClientRequest;
import com.hayden.gateway.client.ClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/federated-gateway")
@RequiredArgsConstructor
public class FederatedController {

    private final FederatedQuery federatedQuery;

    @Cdc
    @PostMapping
    ResponseEntity<ClientResponse> call(@RequestBody ClientRequest clientRequest) {
        return ResponseEntity.ok(federatedQuery.execute(clientRequest));
    }

}
