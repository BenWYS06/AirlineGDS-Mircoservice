package com.ben.common_lib.security;

import com.ben.common_lib.enums.UserRole;
import com.ben.common_lib.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoleCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequiredRoles required = method.getMethodAnnotation(RequiredRoles.class);
        if (required == null) {
            required = method.getBeanType().getAnnotation(RequiredRoles.class);
        }
        if (required == null) {
            return true;
        }

        // Gateway sets X-User-Roles from the verified JWT and strips any client-sent value.
        String header = request.getHeader(TrustedHeaders.USER_ROLES);
        Set<String> roles = header == null ? Set.of() : Arrays.stream(header.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        if (roles.contains(UserRole.ROLE_SYSTEM_ADMIN.name())) {
            return true;
        }
        for (UserRole role : required.value()) {
            if (roles.contains(role.name())) {
                return true;
            }
        }
        throw new ForbiddenException("Your role is not allowed to perform this action");
    }
}
