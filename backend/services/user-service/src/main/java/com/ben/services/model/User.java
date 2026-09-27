package com.ben.services.model;


import com.ben.common_lib.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
@Entity
@Table(name = "users")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "keycloak_id", unique = true, length = 255)
    private String keycloakId;

    @NotBlank(message = "fullName is mandatory")
    private String fullName;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    private String phone;


    @Column(nullable = false)
    @NotNull(message = "Role is mandatory")
    // Explicitly preserve the legacy ordinal storage until a versioned DB migration is introduced.
    @Enumerated(EnumType.ORDINAL)
    private UserRole role;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean verified = false;

    private LocalDateTime lastLogin;
}


