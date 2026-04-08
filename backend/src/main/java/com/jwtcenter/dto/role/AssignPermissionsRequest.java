package com.jwtcenter.dto.role;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AssignPermissionsRequest(@NotNull Set<Long> permissionIds) {
}
