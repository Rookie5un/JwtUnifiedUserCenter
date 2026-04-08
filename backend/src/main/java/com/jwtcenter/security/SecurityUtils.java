package com.jwtcenter.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal principal)) {
            return null;
        }
        return principal;
    }

    public static Long currentUserId() {
        AppPrincipal principal = currentPrincipal();
        return principal == null ? null : principal.getId();
    }
}
