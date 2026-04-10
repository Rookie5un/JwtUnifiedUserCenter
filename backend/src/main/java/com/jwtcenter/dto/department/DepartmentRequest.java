package com.jwtcenter.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
    @NotBlank @Size(max = 80) String name,
    @Size(max = 255) String description
) {
}
