package com.ben.services.service.impl;

import com.ben.common_lib.enums.UserRole;
import com.ben.common_lib.payload.request.KeycloakUserResolveRequest;
import com.ben.services.exception.UserIdentityConflictException;
import com.ben.services.model.User;
import com.ben.services.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createsLocalProfileFromVerifiedKeycloakClaims() {
        KeycloakUserResolveRequest request = new KeycloakUserResolveRequest(
                "keycloak-subject",
                "CUSTOMER@EXAMPLE.COM",
                "Customer One",
                Set.of(UserRole.ROLE_CUSTOMER.name()));
        when(userRepository.findByKeycloakId("keycloak-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.resolveKeycloakUser(request);

        assertThat(result.getKeycloakId()).isEqualTo("keycloak-subject");
        assertThat(result.getEmail()).isEqualTo("customer@example.com");
        assertThat(result.getRole()).isEqualTo(UserRole.ROLE_CUSTOMER);
        assertThat(result.getVerified()).isTrue();
        assertThat(result.getLastLogin()).isNotNull();
        verify(userRepository).save(result);
    }

    @Test
    void linksLegacyProfileByVerifiedEmailAndUsesHighestApplicationRole() {
        User legacyUser = new User();
        legacyUser.setId(42L);
        legacyUser.setEmail("owner@example.com");
        when(userRepository.findByKeycloakId("new-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("owner@example.com"))
                .thenReturn(Optional.of(legacyUser));
        when(userRepository.save(legacyUser)).thenReturn(legacyUser);

        User result = userService.resolveKeycloakUser(new KeycloakUserResolveRequest(
                "new-subject",
                "owner@example.com",
                "Owner",
                Set.of(UserRole.ROLE_CUSTOMER.name(), UserRole.ROLE_AIRLINE_OWNER.name())));

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getKeycloakId()).isEqualTo("new-subject");
        assertThat(result.getRole()).isEqualTo(UserRole.ROLE_AIRLINE_OWNER);
    }

    @Test
    void rejectsEmailAlreadyLinkedToDifferentSubject() {
        User existing = new User();
        existing.setKeycloakId("existing-subject");
        when(userRepository.findByKeycloakId("different-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.resolveKeycloakUser(new KeycloakUserResolveRequest(
                "different-subject",
                "user@example.com",
                "User",
                Set.of())))
                .isInstanceOf(UserIdentityConflictException.class);
    }
}
