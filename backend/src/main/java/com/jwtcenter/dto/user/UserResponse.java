package com.jwtcenter.dto.user;

import com.jwtcenter.enums.UserStatus;

import java.time.Instant;
import java.util.List;

public record UserResponse(
    Long id,
    String username,
    String displayName,
    String phone,
    String email,
    String department,
    UserStatus status,
    List<String> roles,
    List<String> permissions,
    Instant createdAt
) {
}
