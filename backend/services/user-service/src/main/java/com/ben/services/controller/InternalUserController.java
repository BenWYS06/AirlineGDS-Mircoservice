package com.ben.services.controller;

import com.ben.common_lib.payload.request.KeycloakUserResolveRequest;
import com.ben.common_lib.payload.response.ResolvedUserResponse;
import com.ben.common_lib.security.TrustedHeaders;
import com.ben.services.security.InternalApiKeyVerifier;
import com.ben.services.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;
    private final InternalApiKeyVerifier apiKeyVerifier;

    @PostMapping("/resolve")
    public ResponseEntity<ResolvedUserResponse> resolve(
            @RequestHeader(value = TrustedHeaders.INTERNAL_API_KEY, required = false) String apiKey,
            @RequestBody @Valid KeycloakUserResolveRequest request
    ) {
        // Check this request whether come from internal-Api (Api-Gateway)
        apiKeyVerifier.requireValid(apiKey);
        var user = userService.resolveKeycloakUser(request);
        return ResponseEntity.ok(new ResolvedUserResponse(user.getId(), user.getRole()));
    }
}
