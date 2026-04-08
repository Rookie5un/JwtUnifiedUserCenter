package com.jwtcenter.dto.auth;

import com.jwtcenter.dto.user.UserResponse;

import java.time.Instant;

public record AuthTokenResponse(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    UserResponse user
) {
}
