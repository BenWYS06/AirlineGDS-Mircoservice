package com.ben.services.service.impl;

import com.ben.common_lib.enums.UserRole;
import com.ben.common_lib.exception.UserException;
import com.ben.common_lib.payload.request.KeycloakUserResolveRequest;
import com.ben.services.exception.UserIdentityConflictException;
import com.ben.services.model.User;
import com.ben.services.repository.UserRepository;
import com.ben.services.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getUserById(Long id) throws UserException {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found with id: " + id));
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User resolveKeycloakUser(KeycloakUserResolveRequest request) {
        String keycloakId = request.keycloakId().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> bySubject = userRepository.findByKeycloakId(keycloakId);
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);

        User user;
        if (bySubject.isPresent()) {
            user = bySubject.get();
            if (byEmail.isPresent() && !byEmail.get().getId().equals(user.getId())) {
                throw new UserIdentityConflictException("Email belongs to a different Keycloak identity");
            }
        } else if (byEmail.isPresent()) {
            user = byEmail.get();
            if (StringUtils.hasText(user.getKeycloakId()) && !keycloakId.equals(user.getKeycloakId())) {
                throw new UserIdentityConflictException("Email is already linked to another Keycloak identity");
            }
            user.setKeycloakId(keycloakId);
        } else {
            user = new User();
            user.setKeycloakId(keycloakId);
            user.setEmail(email);
        }

        user.setEmail(email);
        user.setFullName(StringUtils.hasText(request.fullName())
                ? request.fullName().trim()
                : email);
        user.setRole(resolveApplicationRole(request));
        user.setVerified(true);
        user.setLastLogin(LocalDateTime.now());
        return userRepository.save(user);
    }

    private UserRole resolveApplicationRole(KeycloakUserResolveRequest request) {
        if (request.roles().contains(UserRole.ROLE_SYSTEM_ADMIN.name())) {
            return UserRole.ROLE_SYSTEM_ADMIN;
        }
        if (request.roles().contains(UserRole.ROLE_AIRLINE_OWNER.name())) {
            return UserRole.ROLE_AIRLINE_OWNER;
        }
        return UserRole.ROLE_CUSTOMER;
    }
}
