package com.ben.common_lib.dto;

import com.ben.common_lib.enums.UserRole;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String phone;
    private String fullName;
    private UserRole role;
    private LocalDateTime lastLogin;

    public UserDTO(Long id, String email, String fullName,
                   UserRole role, LocalDateTime lastLogin) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.lastLogin = lastLogin;
    }
}
