package com.jwtcenter.dto.permission;

import com.jwtcenter.enums.PermissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PermissionRequest(
    @NotBlank @Size(max = 80) String code,
    @NotBlank @Size(max = 80) String name,
    @NotBlank @Size(max = 80) String resource,
    @NotBlank @Size(max = 40) String action,
    @NotNull PermissionType type,
    @Size(max = 255) String description
) {
}
