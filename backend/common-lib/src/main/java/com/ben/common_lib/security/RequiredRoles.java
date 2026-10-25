package com.ben.common_lib.security;

import com.ben.common_lib.enums.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Roles allowed to call an endpoint. Put on a method, or on a controller class for all its methods.
 * ROLE_SYSTEM_ADMIN is always allowed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiredRoles {
    UserRole[] value();
}
