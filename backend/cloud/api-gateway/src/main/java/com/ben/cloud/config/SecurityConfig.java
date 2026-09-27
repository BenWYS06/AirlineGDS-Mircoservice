package com.ben.cloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/fallback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cities/**", "/api/airports/**")
                        .hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/airlines")
                        .hasRole("SYSTEM_ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(
            @Value("${security.keycloak.client-id}") String clientId
    ) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("preferred_username");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> extractRoles(jwt, clientId));
        return converter;
    }

    private Collection<GrantedAuthority> extractRoles(Jwt jwt, String clientId) {
        Set<String> roles = new LinkedHashSet<>();
        addRoles(roles, jwt.getClaimAsMap("realm_access"));

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null && resourceAccess.get(clientId) instanceof Map<?, ?> clientAccess) {
            addRoles(roles, clientAccess);
        }

        return roles.stream()
                .map(this::normalizeRole)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private void addRoles(Set<String> target, Map<?, ?> accessClaim) {
        if (accessClaim != null && accessClaim.get("roles") instanceof Collection<?> roles) {
            roles.stream().map(String::valueOf).forEach(target::add);
        }
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase().replace('-', '_');
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
