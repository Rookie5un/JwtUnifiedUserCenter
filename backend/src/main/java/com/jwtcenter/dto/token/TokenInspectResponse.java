package com.jwtcenter.dto.token;

import com.jwtcenter.dto.user.UserResponse;

import java.time.Instant;
import java.util.List;

public record TokenInspectResponse(
    boolean valid,
    boolean expired,
    Instant expiresAt,
    UserResponse user,
    List<String> roles,
    List<String> permissions
) {
}
