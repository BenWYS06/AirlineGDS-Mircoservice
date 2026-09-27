package com.ben.cloud.security;

import com.ben.cloud.service.LocalUserResolver;
import com.ben.common_lib.payload.request.KeycloakUserResolveRequest;
import com.ben.common_lib.payload.response.ResolvedUserResponse;
import com.ben.common_lib.security.TrustedHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class UserContextGatewayFilter {

    private final LocalUserResolver localUserResolver;

    public UserContextGatewayFilter(LocalUserResolver localUserResolver) {
        this.localUserResolver = localUserResolver;
    }

    public ServerRequest enrich(ServerRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated JWT is required");
        }
        if (!StringUtils.hasText(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The access token has no subject claim");
        }

        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The access token has no email claim");
        }
        if (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A verified Keycloak email is required");
        }

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(role -> role.startsWith("ROLE_"))
                .collect(Collectors.toCollection(TreeSet::new));
        String fullName = firstNonBlank(
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("preferred_username"),
                email);

        KeycloakUserResolveRequest resolveRequest = new KeycloakUserResolveRequest(
                jwt.getSubject(), email, fullName, roles);
        ResolvedUserResponse localUser = localUserResolver.resolve(resolveRequest);
        if (localUser.role() != null) {
            roles.add(localUser.role().name());
        }

        return ServerRequest.from(request)
                .headers(headers -> {
                    headers.remove(TrustedHeaders.USER_ID);
                    headers.remove(TrustedHeaders.USER_EMAIL);
                    headers.remove(TrustedHeaders.USER_ROLES);
                    headers.remove(TrustedHeaders.INTERNAL_API_KEY);
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.set(TrustedHeaders.USER_ID, localUser.id().toString());
                    headers.set(TrustedHeaders.USER_EMAIL, email);
                    headers.set(TrustedHeaders.USER_ROLES, String.join(",", roles));
                })
                .build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "Keycloak user";
    }
}
