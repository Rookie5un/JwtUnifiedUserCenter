package com.jwtcenter.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 80) String name,
    @Size(max = 255) String description
) {
}
