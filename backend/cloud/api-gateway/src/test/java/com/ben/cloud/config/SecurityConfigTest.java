package com.ben.cloud.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void mapsRealmAndConfiguredClientRolesOnly() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "subject",
                        "preferred_username", "user@example.com",
                        "realm_access", Map.of("roles", List.of("ROLE_CUSTOMER")),
                        "resource_access", Map.of(
                                "airline-api", Map.of("roles", List.of("airline-owner")),
                                "untrusted-client", Map.of("roles", List.of("SYSTEM_ADMIN"))
                        )));

        JwtAuthenticationToken authentication = (JwtAuthenticationToken)
                securityConfig.jwtAuthenticationConverter("airline-api").convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_CUSTOMER", "ROLE_AIRLINE_OWNER")
                .doesNotContain("ROLE_SYSTEM_ADMIN");
    }
}
