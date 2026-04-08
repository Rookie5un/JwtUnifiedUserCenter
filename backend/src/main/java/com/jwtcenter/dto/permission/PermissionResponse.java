package com.jwtcenter.dto.permission;

import com.jwtcenter.enums.PermissionType;

public record PermissionResponse(
    Long id,
    String code,
    String name,
    String resource,
    String action,
    PermissionType type,
    String description
) {
}
