package com.ben.services.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Only the gateway knows this key: it guards the endpoint that maps a Keycloak identity to a local user
class InternalApiKeyVerifierTest {

    private final InternalApiKeyVerifier verifier = new InternalApiKeyVerifier("internal-key");

    @Test
    void acceptsTheConfiguredKey() {
        assertThatCode(() -> verifier.requireValid("internal-key")).doesNotThrowAnyException();
    }

    @Test
    void rejectsAWrongKey() {
        assertRejected("internal-key-2");
    }

    @Test
    void rejectsAMissingKey() {
        assertRejected(null);
    }

    private void assertRejected(String providedKey) {
        assertThatThrownBy(() -> verifier.requireValid(providedKey))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
