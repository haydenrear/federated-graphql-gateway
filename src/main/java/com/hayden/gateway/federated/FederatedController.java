package com.hayden.gateway.federated;

import com.hayden.graphql.models.client.ClientRequest;
import com.hayden.graphql.models.client.ClientResponse;
import com.hayden.utilitymodule.Cdc;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@RestController
@RequestMapping("/api/v1/federated-gateway")
@RequiredArgsConstructor
public class FederatedController {

//    private final FederatedQuery federatedQuery;

    @Cdc
//    @PostMapping
    ResponseEntity<ClientResponse> call(@RequestBody ClientRequest clientRequest) {
//        return ResponseEntity.ok(federatedQuery.execute(clientRequest));
        return null;
    }

}
