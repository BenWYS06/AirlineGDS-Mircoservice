package com.ben.services.service;

import com.ben.common_lib.exception.UserException;
import com.ben.common_lib.payload.request.KeycloakUserResolveRequest;
import com.ben.services.model.User;

import java.util.List;

public interface UserService {
    User getUserById(Long id) throws UserException;
    List<User> getUsers();
    User resolveKeycloakUser(KeycloakUserResolveRequest request);
}
