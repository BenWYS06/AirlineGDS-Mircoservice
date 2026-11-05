package com.ben.cloud.security;

import com.ben.cloud.service.LocalUserResolver;
import com.ben.common_lib.enums.UserRole;
import com.ben.common_lib.payload.request.KeycloakUserResolveRequest;
import com.ben.common_lib.payload.response.ResolvedUserResponse;
import com.ben.common_lib.security.TrustedHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.ServerRequest;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Services trust the X-User-* headers without checking, so these rules are the whole security boundary
@ExtendWith(MockitoExtension.class)
class UserContextGatewayFilterTest {

    @Mock
    private LocalUserResolver localUserResolver;

    @InjectMocks
    private UserContextGatewayFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void replacesHeadersAClientTriedToForge() {
        authenticate(claims(), "ROLE_CUSTOMER");
        resolvedUser(7L, UserRole.ROLE_CUSTOMER);
        MockHttpServletRequest forged = new MockHttpServletRequest();
        forged.addHeader(TrustedHeaders.USER_ID, "1");
        forged.addHeader(TrustedHeaders.USER_EMAIL, "admin@example.com");
        forged.addHeader(TrustedHeaders.USER_ROLES, "ROLE_SYSTEM_ADMIN");
        forged.addHeader(TrustedHeaders.INTERNAL_API_KEY, "guessed-key");
        forged.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        ServerRequest result = filter.enrich(ServerRequest.create(forged, List.of()));

        assertThat(result.headers().header(TrustedHeaders.USER_ID)).containsExactly("7");
        assertThat(result.headers().header(TrustedHeaders.USER_EMAIL)).containsExactly("user@example.com");
        assertThat(result.headers().header(TrustedHeaders.USER_ROLES)).containsExactly("ROLE_CUSTOMER");
        assertThat(result.headers().header(TrustedHeaders.INTERNAL_API_KEY)).isEmpty();
        assertThat(result.headers().header(HttpHeaders.AUTHORIZATION)).isEmpty();
    }

    @Test
    void forwardsOnlyRolesAndAddsTheLocalRole() {
        authenticate(claims(), "ROLE_CUSTOMER", "SCOPE_profile");
        resolvedUser(7L, UserRole.ROLE_AIRLINE_OWNER);

        ServerRequest result = filter.enrich(emptyRequest());

        assertThat(result.headers().header(TrustedHeaders.USER_ROLES))
                .containsExactly("ROLE_AIRLINE_OWNER,ROLE_CUSTOMER");
    }

    @Test
    void fullNameFallsBackToUsernameThenEmail() {
        Map<String, Object> withUsername = claims();
        withUsername.put("preferred_username", "user1");
        assertThat(resolvedFullName(withUsername)).isEqualTo("user1");

        assertThat(resolvedFullName(claims())).isEqualTo("user@example.com");
    }

    @Test
    void rejectsARequestWithoutAJwt() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of()));

        assertUnauthorized();
    }

    @Test
    void rejectsATokenWithoutASubject() {
        Map<String, Object> claims = claims();
        claims.remove("sub");
        authenticate(claims);

        assertUnauthorized();
    }

    @Test
    void rejectsATokenWithoutAnEmail() {
        Map<String, Object> claims = claims();
        claims.remove("email");
        authenticate(claims);

        assertUnauthorized();
    }

    @Test
    void rejectsAnUnverifiedEmail() {
        Map<String, Object> claims = claims();
        claims.put("email_verified", false);
        authenticate(claims);

        assertUnauthorized();
    }

    private void assertUnauthorized() {
        assertThatThrownBy(() -> filter.enrich(emptyRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(localUserResolver);
    }

    private String resolvedFullName(Map<String, Object> claims) {
        authenticate(claims);
        resolvedUser(7L, UserRole.ROLE_CUSTOMER);
        filter.enrich(emptyRequest());
        ArgumentCaptor<KeycloakUserResolveRequest> captor = ArgumentCaptor.forClass(KeycloakUserResolveRequest.class);
        verify(localUserResolver, atLeastOnce()).resolve(captor.capture());
        return captor.getValue().fullName();
    }

    private void resolvedUser(Long id, UserRole role) {
        when(localUserResolver.resolve(any())).thenReturn(new ResolvedUserResponse(id, role));
    }

    private ServerRequest emptyRequest() {
        return ServerRequest.create(new MockHttpServletRequest(), List.of());
    }

    private Map<String, Object> claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "keycloak-subject");
        claims.put("email", "user@example.com");
        claims.put("email_verified", true);
        return claims;
    }

    private void authenticate(Map<String, Object> claims, String... authorities) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300), Map.of("alg", "RS256"), claims);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
