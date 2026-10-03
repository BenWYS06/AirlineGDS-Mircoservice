package com.ben.common_lib.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

/** Identity claims that the gateway has already verified with Keycloak. */
public record KeycloakUserResolveRequest(
        @NotBlank String keycloakId,
        @NotBlank @Email String email,
        String fullName,
        Set<String> roles
) {
    public KeycloakUserResolveRequest {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
