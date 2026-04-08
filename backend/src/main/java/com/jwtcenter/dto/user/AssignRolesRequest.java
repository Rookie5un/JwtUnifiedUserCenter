package com.jwtcenter.dto.user;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AssignRolesRequest(@NotNull Set<Long> roleIds) {
}
