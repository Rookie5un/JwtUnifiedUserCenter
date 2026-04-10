package com.jwtcenter.dto.department;

import java.time.Instant;

public record DepartmentResponse(
    Long id,
    String name,
    String description,
    Instant createdAt
) {
}
