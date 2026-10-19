package com.ben.common_lib.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Copies X-User-Roles from the incoming request onto every outgoing Feign call,
 * so the called service can run its own @RequiredRoles checks.
 */
public class FeignRoleForwardInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String roles = request.getHeader(TrustedHeaders.USER_ROLES);
        if (roles != null) {
            template.header(TrustedHeaders.USER_ROLES, roles);
        }
    }
}
