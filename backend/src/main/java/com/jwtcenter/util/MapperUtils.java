package com.jwtcenter.util;

import com.jwtcenter.dto.permission.PermissionResponse;
import com.jwtcenter.dto.performance.PerformanceRecordResponse;
import com.jwtcenter.dto.role.RoleResponse;
import com.jwtcenter.dto.user.UserResponse;
import com.jwtcenter.entity.Permission;
import com.jwtcenter.entity.PerformanceRecord;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;

import java.util.Comparator;
import java.util.List;

public final class MapperUtils {

    private MapperUtils() {
    }

    public static UserResponse toUserResponse(UserAccount user) {
        List<String> roles = user.getRoles().stream()
            .map(Role::getCode)
            .sorted()
            .toList();
        List<String> permissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode)
            .distinct()
            .sorted()
            .toList();
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getPhone(),
            user.getEmail(),
            user.getDepartment(),
            user.getStatus(),
            roles,
            permissions,
            user.getCreatedAt()
        );
    }

    public static RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(
            role.getId(),
            role.getCode(),
            role.getName(),
            role.getDescription(),
            role.getPermissions().stream().map(Permission::getCode).sorted().toList()
        );
    }

    public static PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
            permission.getId(),
            permission.getCode(),
            permission.getName(),
            permission.getResource(),
            permission.getAction(),
            permission.getType(),
            permission.getDescription()
        );
    }

    public static PerformanceRecordResponse toPerformanceRecordResponse(PerformanceRecord record) {
        String approvedBy = record.getApprovedBy() == null ? null : record.getApprovedBy().getDisplayName();
        return new PerformanceRecordResponse(
            record.getId(),
            record.getOwner().getId(),
            record.getOwner().getDisplayName(),
            record.getDepartment(),
            record.getAmount(),
            record.getOccurredOn(),
            record.getType(),
            record.getNote(),
            record.getStatus(),
            record.getRejectedReason(),
            record.getCreatedAt(),
            record.getApprovedAt(),
            approvedBy
        );
    }
}
