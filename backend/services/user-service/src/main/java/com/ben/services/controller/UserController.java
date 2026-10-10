package com.ben.services.controller;

import com.ben.common_lib.enums.UserRole;

import com.ben.common_lib.security.RequiredRoles;

import com.ben.common_lib.dto.UserDTO;
import com.ben.common_lib.exception.UserException;
import com.ben.common_lib.security.TrustedHeaders;
import com.ben.services.mapper.UserMapper;
import com.ben.services.model.User;
import com.ben.services.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/users/profile")
    public ResponseEntity<UserDTO> getUserProfile(
            @RequestHeader(TrustedHeaders.USER_ID) Long userId) throws UserException {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @GetMapping("/api/users/{userId}")
    public ResponseEntity<UserDTO> getUserById(
            @PathVariable Long userId) throws UserException {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @RequiredRoles({})
    @GetMapping("/api/users")
    public ResponseEntity<List<UserDTO>> getUsers() {
        List<User> users = userService.getUsers();
        return ResponseEntity.ok(UserMapper.toDTOList(users));
    }
}
