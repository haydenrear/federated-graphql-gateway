package com.hayden.gateway.client;

import org.springframework.util.MimeType;

import java.util.Map;

/**
 * @param requests the array of params included in the client's request.
 */
public record ClientRequest(ClientInteractable[] requests) {

    public record SecurityValidation(boolean validationRequired) {}
    public record SecurityArgs(Map<String, SecurityValidation> validations) {}

    /**
     *
     * @param params
     * @param type
     * @param method
     * @param securityArgs params contain links to the data, and the validations performs some arbitrary validation on the data, such as hash if required.
     */
    public record ClientInteractable(Map<String, String> params,
                                     MimeType type,
                                     InteractableMethod method,
                                     SecurityArgs securityArgs) {

        public enum InteractableMethod {
            POST, GET
        }

    }
}

