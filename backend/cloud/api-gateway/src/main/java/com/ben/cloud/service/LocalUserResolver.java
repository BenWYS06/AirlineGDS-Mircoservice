package com.ben.cloud.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ben.common_lib.payload.request.KeycloakUserResolveRequest;
import com.ben.common_lib.payload.response.ResolvedUserResponse;
import com.ben.common_lib.security.TrustedHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class LocalUserResolver {

    private final RestClient userServiceClient;
    private final String internalApiKey;
    private final Cache<KeycloakUserResolveRequest, ResolvedUserResponse> users;

    public LocalUserResolver(
            @LoadBalanced RestClient.Builder restClientBuilder,
            @Value("${services.user-service.base-url}") String userServiceBaseUrl,
            @Value("${security.internal-api-key}") String internalApiKey,
            @Value("${security.user-cache-ttl:PT5M}") Duration cacheTtl
    ) {
        this.userServiceClient = restClientBuilder.baseUrl(userServiceBaseUrl).build();
        this.internalApiKey = internalApiKey;
        this.users = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(cacheTtl)
                .build();
    }

    public ResolvedUserResponse resolve(KeycloakUserResolveRequest request) {
        return users.get(request, this::resolveFromUserService);
    }

    private ResolvedUserResponse resolveFromUserService(KeycloakUserResolveRequest request) {
        try {
            ResolvedUserResponse response = userServiceClient.post()
                    .uri("/internal/users/resolve")
                    .header(TrustedHeaders.INTERNAL_API_KEY, internalApiKey)
                    .body(request)
                    .retrieve()
                    .body(ResolvedUserResponse.class);

            if (response == null || response.id() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "User service returned an invalid identity mapping");
            }
            return response;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to resolve the authenticated user profile",
                    exception);
        }
    }
}
