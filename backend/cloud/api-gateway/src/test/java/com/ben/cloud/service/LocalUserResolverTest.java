package com.ben.cloud.service;

import com.ben.common_lib.enums.UserRole;
import com.ben.common_lib.payload.request.KeycloakUserResolveRequest;
import com.ben.common_lib.payload.response.ResolvedUserResponse;
import com.ben.common_lib.security.TrustedHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// A login must never continue with a wrong identity: errors stay errors, and each user is looked up once
class LocalUserResolverTest {

    private static final String RESOLVE_URL = "http://user-service/internal/users/resolve";

    private final KeycloakUserResolveRequest request = new KeycloakUserResolveRequest(
            "subject", "user@example.com", "User", Set.of("ROLE_CUSTOMER"));

    private MockRestServiceServer userService;
    private LocalUserResolver resolver;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        userService = MockRestServiceServer.bindTo(builder).build();
        resolver = new LocalUserResolver(builder, "http://user-service", "internal-key", Duration.ofMinutes(5));
    }

    @Test
    void sendsTheInternalKeyAndReturnsTheLocalUser() {
        userService.expect(requestTo(RESOLVE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(TrustedHeaders.INTERNAL_API_KEY, "internal-key"))
                .andRespond(withSuccess("{\"id\":7,\"role\":\"ROLE_CUSTOMER\"}", MediaType.APPLICATION_JSON));

        ResolvedUserResponse result = resolver.resolve(request);

        assertThat(result).isEqualTo(new ResolvedUserResponse(7L, UserRole.ROLE_CUSTOMER));
        userService.verify();
    }

    @Test
    void asksUserServiceOnlyOnceForTheSameIdentity() {
        userService.expect(once(), requestTo(RESOLVE_URL))
                .andRespond(withSuccess("{\"id\":7,\"role\":\"ROLE_CUSTOMER\"}", MediaType.APPLICATION_JSON));

        resolver.resolve(request);
        resolver.resolve(request);

        userService.verify();
    }

    @Test
    void anAnswerWithoutAnIdIsABadGateway() {
        userService.expect(requestTo(RESOLVE_URL))
                .andRespond(withSuccess("{\"role\":\"ROLE_CUSTOMER\"}", MediaType.APPLICATION_JSON));

        assertFailsWith(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void userServiceDownIsServiceUnavailable() {
        userService.expect(requestTo(RESOLVE_URL)).andRespond(withServerError());

        assertFailsWith(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void aFailedLookupIsNotCached() {
        userService.expect(requestTo(RESOLVE_URL)).andRespond(withServerError());
        userService.expect(requestTo(RESOLVE_URL))
                .andRespond(withSuccess("{\"id\":7,\"role\":\"ROLE_CUSTOMER\"}", MediaType.APPLICATION_JSON));

        assertFailsWith(HttpStatus.SERVICE_UNAVAILABLE);

        assertThat(resolver.resolve(request).id()).isEqualTo(7L);
        userService.verify();
    }

    private void assertFailsWith(HttpStatus status) {
        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                        assertThat(e.getStatusCode()).isEqualTo(status));
    }
}
