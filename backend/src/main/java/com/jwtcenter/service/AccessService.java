package com.jwtcenter.service;

import com.jwtcenter.entity.Permission;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.UserStatus;
import com.jwtcenter.exception.ApiException;
import com.jwtcenter.repository.UserRepository;
import com.jwtcenter.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AccessService {

    private final UserRepository userRepository;

    public AccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserAccount currentUser() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.");
        }
        UserAccount user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Current user is no longer available."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Current user is disabled.");
        }
        return user;
    }

    public boolean hasRole(UserAccount user, String roleCode) {
        return user.getRoles().stream().map(Role::getCode).anyMatch(roleCode::equals);
    }

    public boolean hasPermission(UserAccount user, String permissionCode) {
        return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode)
            .anyMatch(permissionCode::equals);
    }

    public boolean hasAnyPermission(UserAccount user, String... permissionCodes) {
        for (String permissionCode : permissionCodes) {
            if (hasPermission(user, permissionCode)) {
                return true;
            }
        }
        return false;
    }

    public void requirePermission(UserAccount user, String permissionCode) {
        if (!hasPermission(user, permissionCode)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing required permission: " + permissionCode);
        }
    }

    public void requireAnyPermission(UserAccount user, String... permissionCodes) {
        if (!hasAnyPermission(user, permissionCodes)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action.");
        }
    }

    public void requireAdmin(UserAccount user) {
        if (!hasRole(user, "ADMIN")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Administrator permission is required.");
        }
    }

    public void requireManagerOrAdmin(UserAccount user) {
        if (!(hasRole(user, "MANAGER") || hasRole(user, "ADMIN"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Manager or administrator permission is required.");
        }
    }
}
